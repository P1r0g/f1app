package com.example.f1app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.io.EOFException
import java.net.SocketTimeoutException

enum class DataOrigin { CACHE, NETWORK, BACKUP }
enum class ContentErrorKind { NO_INTERNET, API_CONNECTION, TIMEOUT, HTTP, INVALID_DATA, UNKNOWN }
data class ContentError(
    val kind: ContentErrorKind,
    val httpCode: Int? = null,
    val append: Boolean = false
)

data class ContentUiState(
    val drivers: List<Driver> = emptyList(),
    val season: Int? = null,
    val isLoading: Boolean = false,
    val isAppending: Boolean = false,
    val hasMore: Boolean = false,
    val error: ContentError? = null,
    val origin: DataOrigin = DataOrigin.CACHE,
    val backupExists: Boolean = false
)

class ContentViewModel(
    private val repository: DriverPageSource,
    private val storage: ChampionshipStorage,
    private val isInternetAvailable: () -> Boolean,
    private val logger: (String) -> Unit = {}
) : ViewModel() {
    companion object { const val PAGE_SIZE = 10 }

    private val cached = storage.read()
    private val mutableState = MutableStateFlow(
        ContentUiState(
            drivers = cached.drivers,
            season = cached.season,
            backupExists = storage.backupExists()
        )
    )
    val state = mutableState.asStateFlow()

    private var opened = false
    private var nextOffset = 0
    private var requestGeneration = 0
    private var requestJob: Job? = null

    fun onScreenOpened() {
        if (!opened) refresh()
    }

    fun refresh() {
        opened = true
        load(append = false)
    }

    fun loadMore() {
        if (!state.value.isLoading && state.value.hasMore) load(append = true)
    }

    fun clearContent() {
        cancelRequest()
        opened = true
        nextOffset = 0
        val previous = state.value
        storage.write(StoredChampionship(season = previous.season))
        mutableState.value = ContentUiState(
            season = previous.season,
            backupExists = storage.backupExists()
        )
    }

    fun createBackup(): Boolean {
        val result = storage.createBackup()
        mutableState.update { it.copy(backupExists = storage.backupExists()) }
        return result
    }

    fun restoreBackup(): Boolean {
        val restored = storage.restoreBackup() ?: return false
        cancelRequest()
        opened = true
        nextOffset = 0
        mutableState.value = ContentUiState(
            drivers = restored.drivers,
            season = restored.season,
            origin = DataOrigin.BACKUP,
            backupExists = storage.backupExists()
        )
        return true
    }

    fun deleteBackup(): Boolean {
        val result = storage.deleteBackup()
        mutableState.update { it.copy(backupExists = storage.backupExists()) }
        return result
    }

    private fun cancelRequest() {
        requestGeneration++
        requestJob?.cancel()
    }

    private fun load(append: Boolean) {
        if (!append) requestJob?.cancel()
        val generation = ++requestGeneration
        val offset = if (append) nextOffset else 0
        mutableState.update {
            it.copy(isLoading = true, isAppending = append, error = null)
        }

        requestJob = viewModelScope.launch {
            try {
                if (!isInternetAvailable()) {
                    logger("Request skipped: NO_INTERNET")
                    mutableState.update {
                        it.copy(error = ContentError(ContentErrorKind.NO_INTERNET, append = append))
                    }
                    return@launch
                }
                logger("GET current/drivers-championship?limit=$PAGE_SIZE&offset=$offset")
                val page = repository.loadPage(PAGE_SIZE, offset)
                if (generation != requestGeneration) return@launch
                val previous = state.value

                if (append && previous.season != null && page.season != null &&
                    previous.season != page.season) {
                    throw InvalidApiResponseException("Championship season changed between pages")
                }

                val drivers = if (append) {
                    val merged = previous.drivers.associateBy { it.driverId }.toMutableMap()
                    page.drivers.forEach { merged[it.driverId] = it }
                    if (page.hasMore && merged.size == previous.drivers.size) {
                        throw InvalidApiResponseException("Next page made no progress")
                    }
                    merged.values.toList()
                } else page.drivers

                val sorted = drivers.sortedBy { it.position ?: Int.MAX_VALUE }
                val season = page.season ?: previous.season
                storage.write(StoredChampionship(sorted, season))
                nextOffset = page.nextOffset
                mutableState.value = previous.copy(
                    drivers = sorted,
                    season = season,
                    isLoading = false,
                    isAppending = false,
                    hasMore = page.hasMore,
                    error = null,
                    origin = DataOrigin.NETWORK
                )
                logger("Loaded offset=$offset, rows=${page.drivers.size}, hasMore=${page.hasMore}")
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                if (generation == requestGeneration) {
                    logger("Request failed: ${exception.javaClass.simpleName}: ${exception.message}")
                    val error = when {
                        // A received HTTP response is evidence about the API, even if
                        // the device disconnects immediately after receiving it.
                        exception is HttpException ->
                            ContentError(ContentErrorKind.HTTP, exception.code())
                        // Check again: connectivity may disappear during the request.
                        exception is IOException && !isInternetAvailable() ->
                            ContentError(ContentErrorKind.NO_INTERNET)
                        exception is SocketTimeoutException ->
                            ContentError(ContentErrorKind.TIMEOUT)
                        exception is JsonParseException || exception is InvalidApiResponseException ||
                            exception is MalformedJsonException || exception is EOFException ->
                            ContentError(ContentErrorKind.INVALID_DATA)
                        exception is IOException -> ContentError(ContentErrorKind.API_CONNECTION)
                        else -> ContentError(ContentErrorKind.UNKNOWN)
                    }
                    logger("Error category: ${error.kind}, HTTP=${error.httpCode}")
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            isAppending = false,
                            error = error.copy(append = append)
                        )
                    }
                }
            } finally {
                if (generation == requestGeneration) {
                    mutableState.update { it.copy(isLoading = false, isAppending = false) }
                }
            }
        }
    }
}
