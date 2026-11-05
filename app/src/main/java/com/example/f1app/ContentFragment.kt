package com.example.f1app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.f1app.ui.theme.F1appTheme
import java.math.BigDecimal

// This file now contains only Compose UI; MainActivity hosts all four screens.
@Composable
fun ContentScreen(
    state: ContentUiState,
    onSettingsClick: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onClearClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.characters_title),
                Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Button(onClick = onSettingsClick) { Text(stringResource(R.string.settings)) }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = state.season?.let {
                stringResource(R.string.network_subtitle, it)
            } ?: stringResource(R.string.network_subtitle_unknown),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onRefresh,
                enabled = !state.isLoading,
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.refresh_content)) }
            OutlinedButton(
                onClick = onClearClick,
                enabled = state.drivers.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.clear_content)) }
        }

        if (state.isLoading && !state.isAppending && state.drivers.isNotEmpty()) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        state.error?.let { error ->
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(contentErrorMessage(error))
                    TextButton(
                        onClick = if (error.append) onLoadMore else onRefresh,
                        enabled = !state.isLoading
                    ) { Text(stringResource(R.string.retry_loading)) }
                }
            }
        }

        if (state.drivers.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            if (state.origin != DataOrigin.NETWORK || state.error != null) {
                Text(
                    stringResource(
                        if (state.origin == DataOrigin.BACKUP) R.string.backup_data_notice
                        else R.string.cached_data_notice
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (state.drivers.any { it.points == null }) {
                Text(
                    stringResource(R.string.legacy_data_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                stringResource(R.string.loaded_count, state.drivers.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(12.dp))

        if (state.drivers.isEmpty()) {
            if (state.isLoading) {
                Column(
                    Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.loading_data),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            } else {
                Text(
                    stringResource(R.string.empty_content),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        } else {
            StandingsHeader()
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                // The list contains only rows actually loaded from the server/cache.
                items(state.drivers, key = { it.driverId }) { driver -> DriverCard(driver) }
                item(key = "load_more") {
                    if (state.isLoading && state.isAppending) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(Modifier.size(24.dp))
                            Text(
                                stringResource(R.string.loading_more),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    } else if (state.hasMore) {
                        Button(
                            onClick = onLoadMore,
                            enabled = !state.isLoading,
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                        ) { Text(stringResource(R.string.load_next_page)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun contentErrorMessage(error: ContentError): String = when (error.kind) {
    ContentErrorKind.NO_INTERNET -> stringResource(R.string.error_no_internet)
    ContentErrorKind.API_CONNECTION -> stringResource(R.string.error_api_connection)
    ContentErrorKind.TIMEOUT -> stringResource(R.string.error_timeout)
    ContentErrorKind.INVALID_DATA -> stringResource(R.string.error_invalid_data)
    ContentErrorKind.UNKNOWN -> stringResource(R.string.error_unknown)
    ContentErrorKind.HTTP -> when (val code = error.httpCode ?: 0) {
        404 -> stringResource(R.string.error_not_found)
        in 500..599 -> stringResource(R.string.error_http, code)
        else -> stringResource(R.string.error_http_client, code)
    }
}

@Composable
private fun StandingsHeader() {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("№", Modifier.width(40.dp), fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.standings_driver),
                Modifier.weight(1f).padding(start = 14.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.standings_points), Modifier.width(64.dp),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DriverCard(driver: Driver) {
    Surface(
        Modifier.fillMaxWidth(),
        color = if (driver.position?.let { it in 1..3 } == true) MaterialTheme.colorScheme.surfaceVariant
        else MaterialTheme.colorScheme.surface
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    driver.position?.toString() ?: "—",
                    Modifier.width(40.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Box(Modifier.width(4.dp).height(36.dp).background(teamColor(driver.teamId)))
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(
                        "${driver.name} ${driver.surname}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        driver.teamName.ifBlank { driver.teamId },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    driver.points?.let { BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() } ?: "—",
                    Modifier.width(64.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider()
        }
    }
}

private fun teamColor(teamId: String): Color = when (teamId) {
    "red_bull" -> Color(0xFF3671C6)
    "ferrari" -> Color(0xFFED1131)
    "mercedes" -> Color(0xFF27F4D2)
    "mclaren" -> Color(0xFFFF8000)
    "aston_martin" -> Color(0xFF229971)
    "alpine" -> Color(0xFFFF87BC)
    "williams" -> Color(0xFF64C4FF)
    "rb" -> Color(0xFF6692FF)
    "audi" -> Color(0xFFF50537)
    "cadillac" -> Color(0xFFA4A4A4)
    "haas" -> Color(0xFFB6BABD)
    else -> Color.Gray
}

@Preview(showBackground = true)
@Composable
fun ContentScreenPreview() {
    F1appTheme(darkTheme = true) {
        ContentScreen(
            state = ContentUiState(
                drivers = listOf(
                    Driver("preview", "Andrea", "Kimi Antonelli", 12, "Italy", "ANT", "mercedes", 1, 365.0, "Mercedes")
                ),
                season = 2026,
                origin = DataOrigin.NETWORK
            ),
            onSettingsClick = {},
            onRefresh = {},
            onLoadMore = {},
            onClearClick = {}
        )
    }
}
