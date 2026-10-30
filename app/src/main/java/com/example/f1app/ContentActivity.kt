package com.example.f1app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.f1app.ui.theme.F1appTheme

data class Driver(
    val driverId: String,
    val name: String,
    val surname: String,
    val number: Int,
    val nationality: String,
    val shortName: String,
    val teamId: String,
    val position: Int = 0,
    val points: Int = 0
)

// Локальный снимок ответа API.
// https://f1api.dev/api/current/drivers-championship
// Поле position сохраняется именно таким, каким его возвращает API.
val demoDrivers = listOf(
    Driver(
        "antonelli", "Andrea", "Kimi Antonelli",
        12, "Italy", "ANT", "mercedes",
        position = 1, points = 365
    ),
    Driver(
        "russell", "George", "Russell",
        63, "Great Britain", "RUS", "mercedes",
        position = 2, points = 242
    ),
    Driver(
        "hamilton", "Lewis", "Hamilton",
        44, "Great Britain", "HAM", "ferrari",
        position = 3, points = 241
    ),
    Driver(
        "max_verstappen", "Max", "Verstappen",
        33, "Netherlands", "VER", "red_bull",
        position = 4, points = 236
    ),
    Driver(
        "leclerc", "Charles", "Leclerc",
        16, "Monaco", "LEC", "ferrari",
        position = 5, points = 233
    ),
    Driver(
        "norris", "Lando", "Norris",
        4, "Great Britain", "NOR", "mclaren",
        position = 6, points = 211
    ),
    Driver(
        "piastri", "Oscar", "Piastri",
        81, "Australia", "PIA", "mclaren",
        position = 7, points = 144
    ),
    Driver(
        "hadjar", "Isack", "Hadjar",
        6, "France", "HAD", "red_bull",
        position = 8, points = 105
    ),
    Driver(
        "lawson", "Liam", "Lawson",
        30, "New Zealand", "LAW", "rb",
        position = 9, points = 77
    ),
    Driver(
        "gasly", "Pierre", "Gasly",
        10, "France", "GAS", "alpine",
        position = 10, points = 42
    ),
    Driver(
        "lindblad", "Arvid", "Lindblad",
        36, "Great Britain", "LIN", "rb",
        position = 11, points = 38
    ),
    Driver(
        "colapinto", "Franco", "Colapinto",
        43, "Argentina", "COL", "alpine",
        position = 12, points = 27
    ),
    Driver(
        "bearman", "Oliver", "Bearman",
        87, "Great Britain", "BEA", "haas",
        position = 13, points = 23
    ),
    Driver(
        "hulkenberg", "Nico", "Hulkenberg",
        27, "Germany", "HUL", "audi",
        position = 14, points = 13
    ),
    Driver(
        "bortoleto", "Gabriel", "Bortoleto",
        5, "Brazil", "BOR", "audi",
        position = 15, points = 10
    ),
    Driver(
        "ocon", "Esteban", "Ocon",
        31, "France", "OCO", "haas",
        position = 16, points = 7
    ),
    Driver(
        "alonso", "Fernando", "Alonso",
        14, "Spain", "ALO", "aston_martin",
        position = 17, points = 7
    ),
    Driver(
        "sainz", "Carlos", "Sainz",
        55, "Spain", "SAI", "williams",
        position = 18, points = 7
    ),
    Driver(
        "albon", "Alex", "Albon",
        23, "Thailand", "ALB", "williams",
        position = 19, points = 5
    ),
    Driver(
        "stroll", "Lance", "Stroll",
        18, "Canada", "STR", "aston_martin",
        position = 21, points = 0
    ),
    Driver(
        "bottas", "Valtteri", "Bottas",
        77, "Finland", "BOT", "cadillac",
        position = 22, points = 0
    ),
    Driver(
        "perez", "Sergio", "Pérez",
        11, "Mexico", "PER", "cadillac",
        position = 23, points = 0
    )
)

class ContentActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(
            LocaleHelper.applyLanguage(
                newBase,
                AppSettings.getLanguage(newBase)
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("F1Lifecycle", "ContentActivity: onCreate")
        showContent()
    }

    override fun onStart() {
        super.onStart()
        Log.d("F1Lifecycle", "ContentActivity: onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d("F1Lifecycle", "ContentActivity: onResume")

        val expectedLanguage =
            if (AppSettings.getLanguage(this) == "English") "en" else "ru"

        if (resources.configuration.locales[0].language != expectedLanguage) {
            recreate()
            return
        }

        showContent()
    }

    override fun onPause() {
        Log.d("F1Lifecycle", "ContentActivity: onPause")
        super.onPause()
    }

    override fun onStop() {
        Log.d("F1Lifecycle", "ContentActivity: onStop")
        super.onStop()
    }

    override fun onRestart() {
        super.onRestart()
        Log.d("F1Lifecycle", "ContentActivity: onRestart")
    }

    override fun onDestroy() {
        Log.d("F1Lifecycle", "ContentActivity: onDestroy")
        super.onDestroy()
    }

    private fun showContent() {
        val currentDrivers = BackupManager.getCurrentDrivers(this)

        setContent {
            var drivers by remember(currentDrivers) {
                mutableStateOf(currentDrivers)
            }

            F1appTheme(
                darkTheme = AppSettings.isDarkTheme(this),
                fontScale = AppSettings.getFontScale(this)
            ) {
                ContentScreen(
                    drivers = drivers,
                    onSettingsClick = {
                        startActivity(
                            Intent(this, SettingsActivity::class.java)
                        )
                    },
                    onClearClick = {
                        BackupManager.setCurrentDrivers(this, emptyList())
                        drivers = emptyList()
                    },
                    onLoadDemoClick = {
                        BackupManager.setCurrentDrivers(this, demoDrivers)
                        drivers = demoDrivers
                    }
                )
            }
        }
    }
}

@Composable
fun ContentScreen(
    drivers: List<Driver>,
    onSettingsClick: () -> Unit,
    onClearClick: () -> Unit = {},
    onLoadDemoClick: () -> Unit = {}
) {
    val english =
        LocalConfiguration.current.locales[0].language == "en"

    var visibleCount by remember(drivers) {
        mutableStateOf(20)
    }

    val standings = remember(drivers) {
        drivers.sortedBy {
            if (it.position > 0) it.position else Int.MAX_VALUE
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (english) {
                    "Driver Standings"
                } else {
                    "Кубок пилотов"
                },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Button(onClick = onSettingsClick) {
                Text(stringResource(R.string.settings))
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = if (english) {
                "2026 season · f1api.dev snapshot"
            } else {
                "Сезон 2026 · снимок данных f1api.dev"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = if (drivers.isEmpty()) {
                onLoadDemoClick
            } else {
                onClearClick
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = when {
                    drivers.isEmpty() && english -> "Load standings"
                    drivers.isEmpty() -> "Загрузить таблицу"
                    english -> "Clear standings"
                    else -> "Очистить таблицу"
                }
            )
        }

        Spacer(Modifier.height(12.dp))

        if (drivers.isEmpty()) {
            Text(
                text = if (english) {
                    "No standings. Load data or restore a backup in settings."
                } else {
                    "Таблица пуста. Загрузите данные или восстановите копию в настройках."
                },
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            StandingsHeader(english = english)

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(
                    items = standings.take(visibleCount),
                    key = { it.driverId }
                ) { driver ->
                    DriverCard(driver)
                }

                if (visibleCount < standings.size) {
                    item(key = "load_more") {
                        Button(
                            onClick = {
                                visibleCount =
                                    (visibleCount + 20)
                                        .coerceAtMost(standings.size)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Text(
                                stringResource(
                                    R.string.load_more,
                                    standings.size - visibleCount
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StandingsHeader(english: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "№",
                modifier = Modifier.width(40.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (english) "Driver / team" else "Пилот / команда",
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (english) "Points" else "Очки",
                modifier = Modifier.width(64.dp),
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
        modifier = Modifier.fillMaxWidth(),
        color = if (driver.position in 1..3) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (driver.position > 0) {
                        driver.position.toString()
                    } else {
                        "—"
                    },
                    modifier = Modifier.width(40.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(36.dp)
                        .background(teamColor(driver.teamId))
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                ) {
                    Text(
                        text = "${driver.name} ${driver.surname}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = teamName(driver.teamId),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = driver.points.toString(),
                    modifier = Modifier.width(64.dp),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider()
        }
    }
}

private fun teamName(teamId: String): String = when (teamId) {
    "mercedes" -> "Mercedes Formula 1 Team"
    "ferrari" -> "Scuderia Ferrari"
    "mclaren" -> "McLaren Formula 1 Team"
    "red_bull" -> "Red Bull Racing"
    "rb" -> "RB F1 Team"
    "alpine" -> "Alpine F1 Team"
    "haas" -> "Haas F1 Team"
    "audi" -> "Audi Revolut F1 Team"
    "williams" -> "Williams Racing"
    "aston_martin" -> "Aston Martin F1 Team"
    "cadillac" -> "Cadillac Formula 1 Team"
    else -> teamId
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
            drivers = demoDrivers,
            onSettingsClick = {}
        )
    }
}