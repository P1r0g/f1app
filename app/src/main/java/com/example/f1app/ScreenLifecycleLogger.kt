package com.example.f1app

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController

internal const val LIFECYCLE_LOG_TAG = "F1Lifecycle"

/**
 * Observers belong to navigation entries, not the shared Activity lifecycle.
 * Keep observing entries while they remain in the back stack, even when their
 * composables leave composition, to receive actual ON_DESTROY events on removal.
 */
@Composable
internal fun ObserveScreenLifecycles(navController: NavHostController) {
    DisposableEffect(navController) {
        val observers = mutableMapOf<NavBackStackEntry, LifecycleEventObserver>()
        val listener = NavController.OnDestinationChangedListener { controller, destination, _ ->
            val entry = controller.currentBackStackEntry
                ?: return@OnDestinationChangedListener
            val screenName = when (destination.route) {
                AppRoutes.START -> "StartScreen"
                AppRoutes.REGISTRATION -> "RegistrationScreen"
                AppRoutes.CONTENT -> "ContentScreen"
                AppRoutes.SETTINGS -> "SettingsScreen"
                else -> return@OnDestinationChangedListener
            }

            if (!observers.containsKey(entry)) {
                val observer = object : LifecycleEventObserver {
                    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                        Log.d(LIFECYCLE_LOG_TAG, "$screenName: ${event.name}")
                        if (event == Lifecycle.Event.ON_DESTROY) {
                            source.lifecycle.removeObserver(this)
                            observers.remove(entry)
                        }
                    }
                }
                observers[entry] = observer
                // LifecycleRegistry replays previous upward events as needed.
                entry.lifecycle.addObserver(observer)
            }
        }
        // Also immediately observes the current destination, if one already exists.
        navController.addOnDestinationChangedListener(listener)

        onDispose {
            navController.removeOnDestinationChangedListener(listener)
            observers.forEach { (entry, observer) ->
                entry.lifecycle.removeObserver(observer)
            }
            observers.clear()
        }
    }
}
