package com.collectivworks.otprelay.ui

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * Route names for the app's destinations.
 *
 * These are plain strings because no destination takes arguments yet. Once one
 * does, switch to type-safe routes: add the `kotlin.plugin.serialization` plugin
 * and replace these constants with `@Serializable` data classes.
 */
private object Routes {
    const val LANDING = "landing"
    const val SETTINGS = "settings"
    const val SIGN_IN = "sign_in"
}

private const val NAV_ANIM_MILLIS = 300

/**
 * Owns the back stack for the whole app. [MainActivity][com.collectivworks.otprelay.MainActivity]
 * does nothing but wrap this in the theme.
 */
@Composable
fun OtpRelayApp(
    settings: SettingsUiState,
    onDynamicColorChange: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    onSendTestOtp: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.LANDING,
        // Push navigation: a new screen slides in from the right edge and shoves the
        // current one left; going back reverses it. Set on the NavHost so every
        // destination shares the convention — a destination can still override these
        // (a modal sheet that slides up from the bottom, say).
        //
        // SlideDirection is the direction content *travels*, so a screen entering
        // from the right edge slides towards Left.
        enterTransition = {
            slideIntoContainer(SlideDirection.Left, tween(NAV_ANIM_MILLIS))
        },
        exitTransition = {
            slideOutOfContainer(SlideDirection.Left, tween(NAV_ANIM_MILLIS))
        },
        popEnterTransition = {
            slideIntoContainer(SlideDirection.Right, tween(NAV_ANIM_MILLIS))
        },
        popExitTransition = {
            slideOutOfContainer(SlideDirection.Right, tween(NAV_ANIM_MILLIS))
        }
    ) {
        composable(Routes.LANDING) {
            val relay = rememberRelaySwitchState()
            LandingScreen(
                isSyncing = relay.isOn,
                onPowerClick = relay.toggle,
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                isSignedIn = settings.account != null,
                // Signed out, the row leads to sign-in. Signed in, it leads to
                // settings, which is where logging out lives.
                onAccountClick = {
                    navController.navigate(
                        if (settings.account == null) Routes.SIGN_IN else Routes.SETTINGS
                    )
                }
            )
        }
        composable(Routes.SIGN_IN) {
            val viewModel: SignInViewModel = viewModel(factory = SignInViewModel.Factory)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            SignInScreen(
                state = state,
                onSubmit = viewModel::signIn,
                onInputChange = viewModel::clearError,
                onSignedIn = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                state = settings,
                onAccountClick = {
                    if (settings.account == null) navController.navigate(Routes.SIGN_IN)
                },
                onDynamicColorChange = onDynamicColorChange,
                onSendTestOtp = onSendTestOtp,
                onLogoutClick = onSignOut
            )
        }
    }
}
