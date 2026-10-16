package com.example.f1app

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.f1app.ui.theme.F1appTheme

class SettingsActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val language = AppSettings.getLanguage(newBase)

        super.attachBaseContext(
            LocaleHelper.applyLanguage(newBase, language)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("F1Lifecycle", "SettingsActivity: onCreate")

        showSettings()
    }

    override fun onStart() {
        super.onStart()
        Log.d("F1Lifecycle", "SettingsActivity: onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d("F1Lifecycle", "SettingsActivity: onResume")
    }

    override fun onPause() {
        Log.d("F1Lifecycle", "SettingsActivity: onPause")
        super.onPause()
    }

    override fun onStop() {
        Log.d("F1Lifecycle", "SettingsActivity: onStop")
        super.onStop()
    }

    override fun onRestart() {
        super.onRestart()
        Log.d("F1Lifecycle", "SettingsActivity: onRestart")
    }

    override fun onDestroy() {
        Log.d("F1Lifecycle", "SettingsActivity: onDestroy")
        super.onDestroy()
    }

    private fun showSettings() {
        setContent {

            val darkTheme = AppSettings.isDarkTheme(this)
            val fontScale = AppSettings.getFontScale(this)
            val language = AppSettings.getLanguage(this)
            val notifications = AppSettings.isNotificationsEnabled(this)
            val backupExists = BackupManager.backupExists(this)

            F1appTheme(
                darkTheme = darkTheme,
                fontScale = fontScale
            ) {
                SettingsScreen(
                    initialDarkTheme = darkTheme,
                    initialFontScale = fontScale,
                    initialLanguage = language,
                    initialNotifications = notifications,
                    initialBackupExists = backupExists,
                    userName = AppSettings.getUserName(this),
                    userLogin = AppSettings.getUserLogin(this),

                    onDarkThemeChanged = { enabled ->
                        AppSettings.setDarkTheme(this, enabled)
                        showSettings()
                    },

                    onFontScaleChanged = { scale ->
                        AppSettings.setFontScale(this, scale)
                        showSettings()
                    },

                    onLanguageChanged = { selectedLanguage ->
                        if (selectedLanguage != AppSettings.getLanguage(this)) {
                            AppSettings.setLanguage(
                                this,
                                selectedLanguage
                            )

                            recreate()
                        }
                    },

                    onNotificationsChanged = { enabled ->
                        AppSettings.setNotificationsEnabled(
                            this,
                            enabled
                        )
                    },

                    onCreateBackup = {
                        val created = BackupManager.createBackup(this)
                        AlertDialog.Builder(this)
                            .setTitle(R.string.backup)
                            .setMessage(
                                if (created) R.string.backup_created_message
                                else R.string.backup_failed
                            )
                            .setPositiveButton(R.string.ok, null)
                            .show()

                        showSettings()
                    },

                    onRestoreBackup = {
                        showRestoreDialog()
                    },

                    onDeleteBackup = {
                        showDeleteDialog()
                    }
                )
            }
        }
    }

    private fun showRestoreDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.restore_backup)
            .setMessage(R.string.restore_backup_question)
            .setPositiveButton(R.string.restore) { _, _ ->
                val restored = BackupManager.restoreBackup(this)
                Toast.makeText(
                    this,
                    if (restored != null) R.string.backup_restored else R.string.restore_failed,
                    Toast.LENGTH_LONG
                ).show()
                showSettings()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_backup)
            .setMessage(R.string.delete_backup_question)
            .setPositiveButton(R.string.delete) { _, _ ->
                if (!BackupManager.deleteBackup(this)) {
                    Toast.makeText(this, R.string.delete_failed, Toast.LENGTH_LONG).show()
                }
                showSettings()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}

@Composable
fun SettingsScreen(
    initialDarkTheme: Boolean,
    initialFontScale: Float,
    initialLanguage: String,
    initialNotifications: Boolean,
    initialBackupExists: Boolean,
    onDarkThemeChanged: (Boolean) -> Unit,
    onFontScaleChanged: (Float) -> Unit,
    onLanguageChanged: (String) -> Unit,
    onNotificationsChanged: (Boolean) -> Unit,
    onCreateBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onDeleteBackup: () -> Unit,
    userName: String = "",
    userLogin: String = ""
) {
    var darkTheme by remember(initialDarkTheme) {
        mutableStateOf(initialDarkTheme)
    }

    var fontScale by remember(initialFontScale) {
        mutableFloatStateOf(initialFontScale)
    }

    var language by remember(initialLanguage) {
        mutableStateOf(initialLanguage)
    }

    var notifications by remember(initialNotifications) {
        mutableStateOf(initialNotifications)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 24.dp,
                bottom = 32.dp
            )
    ) {

        Text(
            text = androidx.compose.ui.res.stringResource(
                R.string.settings_title
            ),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (userLogin.isNotBlank()) {
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.profile),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.profile_name, userName),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.profile_login, userLogin),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        Text(
            text = androidx.compose.ui.res.stringResource(
                R.string.appearance
            ),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = androidx.compose.ui.res.stringResource(
                    R.string.dark_theme
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Switch(
                checked = darkTheme,
                onCheckedChange = {
                    darkTheme = it
                    onDarkThemeChanged(it)
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = androidx.compose.ui.res.stringResource(
                R.string.font_size
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Slider(
            value = fontScale,
            onValueChange = {
                fontScale = it
                onFontScaleChanged(it)
            },
            valueRange = 0.8f..1.3f
        )

        Text(
            text = androidx.compose.ui.res.stringResource(
                R.string.font_size_value,
                "${(fontScale * 100).toInt()}%"
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = androidx.compose.ui.res.stringResource(
                R.string.language
            ),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = language == "Русский",
                onClick = {
                    language = "Русский"
                    onLanguageChanged("Русский")
                }
            )

            Text(
                text = androidx.compose.ui.res.stringResource(
                    R.string.russian
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = language == "English",
                onClick = {
                    language = "English"
                    onLanguageChanged("English")
                }
            )

            Text(
                text = androidx.compose.ui.res.stringResource(
                    R.string.english
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = androidx.compose.ui.res.stringResource(
                    R.string.notifications
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Switch(
                checked = notifications,
                onCheckedChange = {
                    notifications = it
                    onNotificationsChanged(it)
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = androidx.compose.ui.res.stringResource(
                R.string.backup
            ),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onCreateBackup,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = androidx.compose.ui.res.stringResource(
                    R.string.create_backup
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onRestoreBackup,
            enabled = initialBackupExists,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = androidx.compose.ui.res.stringResource(
                    R.string.restore_backup
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onDeleteBackup,
            enabled = initialBackupExists,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = androidx.compose.ui.res.stringResource(
                    R.string.delete_backup
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (initialBackupExists) {
                androidx.compose.ui.res.stringResource(
                    R.string.backup_exists
                )
            } else {
                androidx.compose.ui.res.stringResource(
                    R.string.backup_not_exists
                )
            },
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    F1appTheme(
        darkTheme = true
    ) {
        SettingsScreen(
            initialDarkTheme = true,
            initialFontScale = 1.0f,
            initialLanguage = "Русский",
            initialNotifications = true,
            initialBackupExists = true,
            onDarkThemeChanged = {},
            onFontScaleChanged = {},
            onLanguageChanged = {},
            onNotificationsChanged = {},
            onCreateBackup = {},
            onRestoreBackup = {},
            onDeleteBackup = {}
        )
    }
}