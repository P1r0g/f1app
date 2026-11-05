package com.example.f1app

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.f1app.ui.theme.F1appTheme

internal object AppRoutes {
    const val START = "start"
    const val REGISTRATION = "registration"
    const val CONTENT = "content"
    const val SETTINGS = "settings"
    const val REGISTERED_LOGIN = "registered_login"
}

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(
            LocaleHelper.applyLanguage(newBase, AppSettings.getLanguage(newBase))
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(LIFECYCLE_LOG_TAG, "MainActivity: onCreate")
        setContent {
            F1App(
                onLanguageChanged = { language ->
                    if (language != AppSettings.getLanguage(this)) {
                        AppSettings.setLanguage(this, language)
                        // rememberNavController restores the back stack after recreation.
                        recreate()
                    }
                },
                onExit = { finish() }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(LIFECYCLE_LOG_TAG, "MainActivity: onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(LIFECYCLE_LOG_TAG, "MainActivity: onResume")
    }

    override fun onPause() {
        Log.d(LIFECYCLE_LOG_TAG, "MainActivity: onPause")
        super.onPause()
    }

    override fun onStop() {
        Log.d(LIFECYCLE_LOG_TAG, "MainActivity: onStop")
        super.onStop()
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(LIFECYCLE_LOG_TAG, "MainActivity: onRestart")
    }

    override fun onDestroy() {
        Log.d(LIFECYCLE_LOG_TAG, "MainActivity: onDestroy")
        super.onDestroy()
    }
}

@Composable
private fun F1App(
    onLanguageChanged: (String) -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    var darkTheme by remember { mutableStateOf(AppSettings.isDarkTheme(context)) }
    var fontScale by remember { mutableFloatStateOf(AppSettings.getFontScale(context)) }
    var notifications by remember {
        mutableStateOf(AppSettings.isNotificationsEnabled(context))
    }
    var userLogin by rememberSaveable { mutableStateOf(AppSettings.getUserLogin(context)) }
    var userName by rememberSaveable { mutableStateOf(AppSettings.getUserName(context)) }
    val appContext = context.applicationContext
    val contentFactory = remember(appContext) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(ContentViewModel::class.java))
                return ContentViewModel(
                    repository = F1Repository(F1ApiClient.create()),
                    storage = PreferencesChampionshipStorage(appContext),
                    isInternetAvailable = InternetConnectionChecker(appContext)::isAvailable,
                    logger = { message -> Log.d("F1Network", message) }
                ) as T
            }
        }
    }
    val contentModel: ContentViewModel = viewModel(factory = contentFactory)
    val contentState by contentModel.state.collectAsStateWithLifecycle()

    ObserveScreenLifecycles(navController)

    // Other destinations use NavHost's back stack. Root Back finishes the Activity.
    BackHandler(
        enabled = currentEntry?.destination?.route == AppRoutes.START ||
            currentEntry?.destination?.route == AppRoutes.CONTENT
    ) {
        onExit()
    }

    F1appTheme(darkTheme = darkTheme, fontScale = fontScale) {
        NavHost(navController = navController, startDestination = AppRoutes.START) {
            composable(AppRoutes.START) { entry ->
                val resultFlow = remember(entry) {
                    entry.savedStateHandle.getStateFlow(AppRoutes.REGISTERED_LOGIN, "")
                }
                val registeredLogin by resultFlow.collectAsStateWithLifecycle()

                StartScreen(
                    initialLogin = userLogin,
                    userName = userName,
                    registeredLogin = registeredLogin,
                    onRegistrationClick = {
                        navController.navigate(AppRoutes.REGISTRATION) {
                            launchSingleTop = true
                        }
                    },
                    onLoginClick = { login, _ ->
                        userLogin = login.trim()
                        AppSettings.saveProfile(context, userLogin, userName)
                        navController.navigate(AppRoutes.CONTENT) {
                            popUpTo(AppRoutes.START) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(AppRoutes.REGISTRATION) {
                RegistrationScreen(
                    onRegister = { login ->
                        userLogin = login
                        userName = AppSettings.getUserName(context)
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(AppRoutes.REGISTERED_LOGIN, login)
                        navController.popBackStack()
                    }
                )
            }

            composable(AppRoutes.CONTENT) {
                LaunchedEffect(contentModel) {
                    contentModel.onScreenOpened()
                }
                ContentScreen(
                    state = contentState,
                    onRefresh = contentModel::refresh,
                    onLoadMore = contentModel::loadMore,
                    onClearClick = contentModel::clearContent,
                    onSettingsClick = {
                        navController.navigate(AppRoutes.SETTINGS) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(AppRoutes.SETTINGS) {
                SettingsScreen(
                    initialDarkTheme = darkTheme,
                    initialFontScale = fontScale,
                    initialLanguage = AppSettings.getLanguage(context),
                    initialNotifications = notifications,
                    initialBackupExists = contentState.backupExists,
                    userName = userName,
                    userLogin = userLogin,
                    onDarkThemeChanged = { enabled ->
                        darkTheme = enabled
                        AppSettings.setDarkTheme(context, enabled)
                    },
                    onFontScaleChanged = { scale ->
                        fontScale = scale
                        AppSettings.setFontScale(context, scale)
                    },
                    onLanguageChanged = onLanguageChanged,
                    onNotificationsChanged = { enabled ->
                        notifications = enabled
                        AppSettings.setNotificationsEnabled(context, enabled)
                    },
                    onCreateBackup = {
                        val created = contentModel.createBackup()
                        AlertDialog.Builder(context)
                            .setTitle(R.string.backup)
                            .setMessage(
                                if (created) R.string.backup_created_message
                                else R.string.backup_failed
                            )
                            .setPositiveButton(R.string.ok, null)
                            .show()
                    },
                    onRestoreBackup = {
                        AlertDialog.Builder(context)
                            .setTitle(R.string.restore_backup)
                            .setMessage(R.string.restore_backup_question)
                            .setPositiveButton(R.string.restore) { _, _ ->
                                val restored = contentModel.restoreBackup()
                                Toast.makeText(
                                    context,
                                    if (restored) R.string.backup_restored
                                    else R.string.restore_failed,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                    },
                    onDeleteBackup = {
                        AlertDialog.Builder(context)
                            .setTitle(R.string.delete_backup)
                            .setMessage(R.string.delete_backup_question)
                            .setPositiveButton(R.string.delete) { _, _ ->
                                val deleted = contentModel.deleteBackup()
                                if (!deleted) {
                                    Toast.makeText(
                                        context, R.string.delete_failed, Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                    }
                )
            }
        }
    }
}
