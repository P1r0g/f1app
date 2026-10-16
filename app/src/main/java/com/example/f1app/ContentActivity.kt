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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.f1app.ui.theme.F1appTheme

// The field names correspond to the driver response from f1api.dev.
// Local snapshot of https://f1api.dev/api/current/drivers retrieved on 2026-10-02.
// Names, numbers, countries and team IDs are preserved as returned by the API.
data class Driver(
    val driverId: String,
    val name: String,
    val surname: String,
    val number: Int,
    val nationality: String,
    val shortName: String,
    val teamId: String
)

val demoDrivers = listOf(
    Driver("antonelli", "Andrea", "Kimi Antonelli", 12, "Italy", "ANT", "mercedes"),
    Driver("russell", "George", "Russell", 63, "Great Britain", "RUS", "mercedes"),
    Driver("hamilton", "Lewis", "Hamilton", 44, "Great Britain", "HAM", "ferrari"),
    Driver("norris", "Lando", "Norris", 4, "Great Britain", "NOR", "mclaren"),
    Driver("leclerc", "Charles", "Leclerc", 16, "Monaco", "LEC", "ferrari"),
    Driver("max_verstappen", "Max", "Verstappen", 33, "Netherlands", "VER", "red_bull"),
    Driver("piastri", "Oscar", "Piastri", 81, "Australia", "PIA", "mclaren"),
    Driver("hadjar", "Isack", "Hadjar", 6, "France", "HAD", "red_bull"),
    Driver("lawson", "Liam", "Lawson", 30, "New Zealand", "LAW", "rb"),
    Driver("gasly", "Pierre", "Gasly", 10, "France", "GAS", "alpine"),
    Driver("lindblad", "Arvid", "Lindblad", 36, "Great Britain", "LIN", "rb"),
    Driver("colapinto", "Franco", "Colapinto", 43, "Argentina", "COL", "alpine"),
    Driver("bearman", "Oliver", "Bearman", 87, "Great Britain", "BEA", "haas"),
    Driver("bortoleto", "Gabriel", "Bortoleto", 5, "Brazil", "BOR", "audi"),
    Driver("hulkenberg", "Nico", "Hulkenberg", 27, "Germany", "HUL", "audi"),
    Driver("ocon", "Esteban", "Ocon", 31, "France", "OCO", "haas"),
    Driver("sainz", "Carlos", "Sainz", 55, "Spain", "SAI", "williams"),
    Driver("albon", "Alex", "Albon", 23, "Thailand", "ALB", "williams"),
    Driver("alonso", "Fernando", "Alonso", 14, "Spain", "ALO", "aston_martin"),
    Driver("stroll", "Lance", "Stroll", 18, "Canada", "STR", "aston_martin"),
    Driver("bottas", "Valtteri", "Bottas", 77, "Finland", "BOT", "cadillac"),
    Driver("perez", "Sergio", "Pérez", 11, "Mexico", "PER", "cadillac")
)

class ContentActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(
            LocaleHelper.applyLanguage(newBase, AppSettings.getLanguage(newBase))
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
        val expectedLanguage = if (AppSettings.getLanguage(this) == "English") "en" else "ru"
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
            var drivers by remember(currentDrivers) { mutableStateOf(currentDrivers) }
            F1appTheme(
                darkTheme = AppSettings.isDarkTheme(this),
                fontScale = AppSettings.getFontScale(this)
            ) {
                ContentScreen(
                    drivers = drivers,
                    onSettingsClick = {
                        startActivity(Intent(this, SettingsActivity::class.java))
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
    var visibleCount by remember(drivers) { mutableStateOf(20) }
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
                text = stringResource(R.string.characters_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Button(onClick = onSettingsClick) {
                Text(stringResource(R.string.settings))
            }
        }
        Text(
            text = stringResource(R.string.content_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = if (drivers.isEmpty()) onLoadDemoClick else onClearClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(if (drivers.isEmpty()) R.string.load_demo else R.string.clear_content))
        }
        Spacer(Modifier.height(12.dp))
        if (drivers.isEmpty()) {
            Text(
                text = stringResource(R.string.empty_content),
                color = MaterialTheme.colorScheme.onBackground
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(drivers.take(visibleCount), key = { it.driverId }) { driver ->
                    DriverCard(driver)
                }
                if (visibleCount < drivers.size) {
                    item(key = "load_more") {
                        Button(
                            onClick = { visibleCount = (visibleCount + 20).coerceAtMost(drivers.size) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.load_more, drivers.size - visibleCount))
                        }
                    }
                }
            }
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

@Composable
fun DriverCard(driver: Driver) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp)) {
            Box(
                Modifier.width(5.dp).height(104.dp)
                    .background(teamColor(driver.teamId))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${driver.name} ${driver.surname}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.character_status, driver.number.toString()))
                Text(stringResource(R.string.character_species, teamName(driver.teamId)))
                Text(stringResource(R.string.character_gender, driver.nationality))
                Text(stringResource(R.string.character_origin, driver.shortName))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ContentScreenPreview() {
    F1appTheme(darkTheme = true) {
        ContentScreen(drivers = demoDrivers.take(2), onSettingsClick = {})
    }
}
