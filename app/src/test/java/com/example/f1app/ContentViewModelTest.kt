package com.example.f1app

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException
import retrofit2.Response
import okhttp3.ResponseBody.Companion.toResponseBody

@OptIn(ExperimentalCoroutinesApi::class)
class ContentViewModelTest {
    @Before fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }


    private fun createViewModel(
        source: DriverPageSource,
        storage: ChampionshipStorage,
        isInternetAvailable: () -> Boolean = { true }
    ) = ContentViewModel(source, storage, isInternetAvailable)

    private fun driver(id: Int, points: Double = id.toDouble()) =
        Driver("id$id", "Name", "Driver$id", id + 1, "", "TST", "team", id + 1, points)

    private class FakeSource(
        val handler: suspend (Int, Int) -> DriverPage
    ) : DriverPageSource {
        val calls = mutableListOf<Pair<Int, Int>>()
        override suspend fun loadPage(limit: Int, offset: Int): DriverPage {
            calls += limit to offset
            return handler(limit, offset)
        }
    }

    private class FakeStorage(
        var current: StoredChampionship = StoredChampionship(),
        var backup: StoredChampionship? = null
    ) : ChampionshipStorage {
        override fun read() = current
        override fun write(snapshot: StoredChampionship) { current = snapshot }
        override fun createBackup(): Boolean {
            if (current.drivers.isEmpty()) return false
            backup = current
            return true
        }
        override fun restoreBackup(): StoredChampionship? = backup?.also { current = it }
        override fun deleteBackup(): Boolean { backup = null; return true }
        override fun backupExists() = backup != null
    }

    @Test fun `first screen opening starts one request and replaces legacy cache`() = runTest {
        val legacy = driver(0).copy(position = null, points = null)
        val store = FakeStorage(StoredChampionship(listOf(legacy), 2026))
        val source = FakeSource { _, _ -> DriverPage(listOf(driver(0, 365.0)), 2026, 1, false) }
        val vm = createViewModel(source, store)
        vm.onScreenOpened()
        vm.onScreenOpened()
        advanceUntilIdle()
        assertEquals(1, source.calls.size)
        assertEquals(365.0, vm.state.value.drivers.single().points!!, 0.0)
        assertEquals(1, vm.state.value.drivers.single().position)
        assertEquals(vm.state.value.drivers, store.current.drivers)
        assertEquals(DataOrigin.NETWORK, vm.state.value.origin)
    }

    @Test fun `load more uses server offsets and blocks double taps`() = runTest {
        val source = FakeSource { limit, offset ->
            val count = if (offset == 20) 2 else limit
            DriverPage((offset until offset + count).map { driver(it) }, 2026, offset + count, count == limit)
        }
        val vm = createViewModel(source, FakeStorage())
        vm.refresh(); advanceUntilIdle()
        vm.loadMore(); vm.loadMore(); advanceUntilIdle()
        vm.loadMore(); advanceUntilIdle()
        vm.loadMore(); advanceUntilIdle()
        assertEquals(listOf(0, 10, 20), source.calls.map { it.second })
        assertEquals(22, vm.state.value.drivers.size)
        assertFalse(vm.state.value.hasMore)
    }

    @Test fun `API connection error with internet preserves saved standings`() = runTest {
        val snapshot = StoredChampionship(listOf(driver(0, 0.0)), 2026)
        val store = FakeStorage(snapshot)
        val source = FakeSource { _, _ -> throw UnknownHostException("offline") }
        val vm = createViewModel(source, store)
        vm.refresh(); advanceUntilIdle()
        assertEquals(snapshot.drivers, vm.state.value.drivers)
        assertEquals(snapshot, store.current)
        assertEquals(ContentErrorKind.API_CONNECTION, vm.state.value.error?.kind)
        assertFalse(vm.state.value.isLoading)
    }

    @Test fun `failed next page can be retried at the same offset`() = runTest {
        var failed = false
        val source = FakeSource { _, offset ->
            if (offset == 10 && !failed) {
                failed = true
                throw SocketTimeoutException("timeout")
            }
            if (offset == 0) DriverPage((0..9).map { driver(it) }, 2026, 10, true)
            else DriverPage(listOf(driver(10)), 2026, 11, false)
        }
        val vm = createViewModel(source, FakeStorage())
        vm.refresh(); advanceUntilIdle()
        vm.loadMore(); advanceUntilIdle()
        assertEquals(10, vm.state.value.drivers.size)
        assertEquals(ContentErrorKind.TIMEOUT, vm.state.value.error?.kind)
        assertTrue(vm.state.value.error!!.append)
        vm.loadMore(); advanceUntilIdle()
        assertEquals(listOf(0, 10, 10), source.calls.map { it.second })
        assertEquals(11, vm.state.value.drivers.size)
        assertNull(vm.state.value.error)
    }

    @Test fun `clear prevents a cancelled request from bringing old rows back`() = runTest {
        val source = FakeSource { _, _ ->
            try { awaitCancellation() }
            catch (_: CancellationException) { DriverPage(listOf(driver(9)), 2026, 1, false) }
        }
        val store = FakeStorage(StoredChampionship(listOf(driver(0)), 2026))
        val vm = createViewModel(source, store)
        vm.refresh(); runCurrent()
        vm.clearContent(); advanceUntilIdle()
        assertTrue(vm.state.value.drivers.isEmpty())
        assertTrue(store.current.drivers.isEmpty())
        assertNull(vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
    }

    @Test fun `restoring a backup prevents an in-flight request from overwriting it`() = runTest {
        val source = FakeSource { _, _ ->
            try { awaitCancellation() }
            catch (_: CancellationException) { DriverPage(listOf(driver(9, 999.0)), 2026, 1, false) }
        }
        val backup = StoredChampionship(listOf(driver(0, 8.5)), 2026)
        val store = FakeStorage(backup = backup)
        val vm = createViewModel(source, store)
        vm.refresh(); runCurrent()
        assertTrue(vm.restoreBackup())
        advanceUntilIdle()
        assertEquals(backup.drivers, vm.state.value.drivers)
        assertEquals(DataOrigin.BACKUP, vm.state.value.origin)
        assertFalse(vm.state.value.hasMore)
        assertEquals(backup, store.current)
    }

    @Test fun `overlapping pages are deduplicated without changing the cursor`() = runTest {
        val source = FakeSource { _, offset -> when (offset) {
            0 -> DriverPage((0..9).map { driver(it) }, 2026, 10, true)
            10 -> DriverPage((9..18).map { driver(it) }, 2026, 20, true)
            else -> DriverPage(listOf(driver(19)), 2026, 21, false)
        } }
        val vm = createViewModel(source, FakeStorage())
        vm.refresh(); advanceUntilIdle()
        vm.loadMore(); advanceUntilIdle()
        vm.loadMore(); advanceUntilIdle()
        assertEquals(listOf(0, 10, 20), source.calls.map { it.second })
        assertEquals(20, vm.state.value.drivers.size)
    }

    @Test fun `empty response clears stale rows and ends loading`() = runTest {
        val store = FakeStorage(StoredChampionship(listOf(driver(0)), 2026))
        val vm = createViewModel(FakeSource { _, _ -> DriverPage(emptyList(), 2026, 0, false) }, store)
        vm.refresh(); advanceUntilIdle()
        assertTrue(vm.state.value.drivers.isEmpty())
        assertFalse(vm.state.value.hasMore)
        assertNull(vm.state.value.error)
        assertTrue(store.current.drivers.isEmpty())
    }

    @Test fun `backup creation and deletion update the UI state`() = runTest {
        val store = FakeStorage(StoredChampionship(listOf(driver(0)), 2026))
        val vm = createViewModel(FakeSource { _, _ -> error("No network request expected") }, store)
        assertTrue(vm.createBackup())
        assertTrue(vm.state.value.backupExists)
        assertTrue(vm.deleteBackup())
        assertFalse(vm.state.value.backupExists)
    }

    @Test fun `offline device shows connection advice without calling API`() = runTest {
        val saved = StoredChampionship(listOf(driver(0)), 2026)
        val store = FakeStorage(saved)
        val source = FakeSource { _, _ -> error("Must not request API while offline") }
        val vm = createViewModel(source, store, isInternetAvailable = { false })
        vm.refresh(); advanceUntilIdle()
        assertTrue(source.calls.isEmpty())
        assertEquals(ContentErrorKind.NO_INTERNET, vm.state.value.error?.kind)
        assertEquals(saved, store.current)
        assertEquals(saved.drivers, vm.state.value.drivers)
        assertFalse(vm.state.value.isLoading)
    }

    @Test fun `internet disappearing during request is not reported as server failure`() = runTest {
        var online = true
        val source = FakeSource { _, _ ->
            online = false
            throw UnknownHostException("connection lost")
        }
        val vm = createViewModel(source, FakeStorage(), isInternetAvailable = { online })
        vm.refresh(); advanceUntilIdle()
        assertEquals(1, source.calls.size)
        assertEquals(ContentErrorKind.NO_INTERNET, vm.state.value.error?.kind)
    }

    @Test fun `timeout after losing internet shows offline error`() = runTest {
        var online = true
        val source = FakeSource { _, _ ->
            online = false
            throw SocketTimeoutException("connection lost")
        }
        val vm = createViewModel(source, FakeStorage(), isInternetAvailable = { online })
        vm.refresh(); advanceUntilIdle()
        assertEquals(ContentErrorKind.NO_INTERNET, vm.state.value.error?.kind)
    }

    @Test fun `HTTP 503 remains server error even if connectivity changes afterwards`() = runTest {
        var online = true
        val saved = StoredChampionship(listOf(driver(0)), 2026)
        val source = FakeSource { _, _ ->
            online = false
            throw HttpException(Response.error<Any>(503, "unavailable".toResponseBody()))
        }
        val vm = createViewModel(source, FakeStorage(saved), isInternetAvailable = { online })
        vm.refresh(); advanceUntilIdle()
        assertEquals(ContentErrorKind.HTTP, vm.state.value.error?.kind)
        assertEquals(503, vm.state.value.error?.httpCode)
        assertEquals(saved.drivers, vm.state.value.drivers)
    }

    @Test fun `HTTP 400 retains code for API request error message`() = runTest {
        val source = FakeSource { _, _ ->
            throw HttpException(Response.error<Any>(400, "bad request".toResponseBody()))
        }
        val vm = createViewModel(source, FakeStorage())
        vm.refresh(); advanceUntilIdle()
        assertEquals(ContentErrorKind.HTTP, vm.state.value.error?.kind)
        assertEquals(400, vm.state.value.error?.httpCode)
    }

    @Test fun `retry succeeds after internet is enabled`() = runTest {
        var online = false
        val source = FakeSource { _, _ -> DriverPage(listOf(driver(0)), 2026, 1, false) }
        val vm = createViewModel(source, FakeStorage(), isInternetAvailable = { online })
        vm.refresh(); advanceUntilIdle()
        assertEquals(ContentErrorKind.NO_INTERNET, vm.state.value.error?.kind)
        online = true
        vm.refresh(); advanceUntilIdle()
        assertEquals(1, source.calls.size)
        assertNull(vm.state.value.error)
        assertEquals(DataOrigin.NETWORK, vm.state.value.origin)
    }

}
