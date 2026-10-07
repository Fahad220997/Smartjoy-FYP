package com.smartjoy.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smartjoy.app.service.SmartJoyService
import com.smartjoy.app.ui.screens.*
import com.smartjoy.app.ui.theme.SmartJoyTheme
import com.smartjoy.app.viewmodel.AuthViewModel

private object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val VERIFY_EMAIL = "verify_email"
    const val PROFILE = "profile"
    const val NAVIGATION = "navigation"
    const val CHILD_TRACKING = "child_tracking"
    const val DANGER_ZONE = "danger_zone"

    val POST_AUTH_ROUTES = setOf(PROFILE, NAVIGATION, CHILD_TRACKING, DANGER_ZONE)
}

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartJoyTheme {
                NotificationPermissionRequest()

                Surface(modifier = Modifier.fillMaxSize()) {
                    SmartJoyNavHost(authViewModel = authViewModel)
                }
            }
        }
    }
}

@Composable
private fun NotificationPermissionRequest() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { }
        
        LaunchedEffect(Unit) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun SmartJoyNavHost(authViewModel: AuthViewModel) {
    val navController: NavHostController = rememberNavController()
    val uiState by authViewModel.uiState.collectAsState()
    val context = LocalContext.current

    val startDestination = when {
        uiState.isLoggedIn && uiState.isEmailVerified -> Routes.PROFILE
        uiState.isLoggedIn -> Routes.VERIFY_EMAIL
        else -> Routes.LOGIN
    }

    LaunchedEffect(uiState.isLoggedIn, uiState.isEmailVerified) {
        if (uiState.isLoggedIn && uiState.isEmailVerified) {
            val intent = Intent(context, SmartJoyService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }

    NavHost(
        navController = navController, 
        startDestination = startDestination,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() }
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                uiState = uiState,
                onLogin = { email, password -> authViewModel.login(email, password) },
                onNavigateToRegister = {
                    authViewModel.clearMessages()
                    navController.navigate(Routes.REGISTER)
                },
                onNavigateToForgotPassword = {
                    authViewModel.clearMessages()
                    navController.navigate(Routes.FORGOT_PASSWORD)
                }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                uiState = uiState,
                onRegister = { name, email, password, confirmPassword ->
                    authViewModel.register(name, email, password, confirmPassword)
                },
                onNavigateToLogin = {
                    authViewModel.clearMessages()
                    navController.popBackStack()
                }
            )
        }
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                uiState = uiState,
                onSendResetEmail = { email -> authViewModel.sendPasswordReset(email) },
                onNavigateBack = {
                    authViewModel.clearMessages()
                    authViewModel.resetPasswordResetFlag()
                    navController.popBackStack()
                }
            )
        }
        composable(Routes.VERIFY_EMAIL) {
            VerifyEmailScreen(
                uiState = uiState,
                onResendEmail = { authViewModel.resendVerificationEmail() },
                onCheckVerified = { authViewModel.refreshEmailVerified() },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.LOGIN) { popUpTo(0) }
                }
            )
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                uiState = uiState,
                onUpdateName = { newName -> authViewModel.updateDisplayName(newName) },
                onUploadPhoto = { uri -> authViewModel.uploadProfilePhoto(uri) },
                onOpenNavigation = { navController.navigate(Routes.NAVIGATION) },
                onOpenChildTracking = { navController.navigate(Routes.CHILD_TRACKING) },
                onOpenDangerZone = { navController.navigate(Routes.DANGER_ZONE) },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.LOGIN) { popUpTo(0) }
                }
            )
        }
        composable(Routes.NAVIGATION) {
            NavigationScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.CHILD_TRACKING) {
            ChildTrackingScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.DANGER_ZONE) {
            DangerZoneScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }

    LaunchedEffect(uiState.isLoggedIn, uiState.isEmailVerified) {
        val currentRoute = navController.currentDestination?.route
        if (uiState.isLoggedIn && uiState.isEmailVerified && (currentRoute !in Routes.POST_AUTH_ROUTES)) {
            navController.navigate(Routes.PROFILE) { popUpTo(0) }
        } else if (
            uiState.isLoggedIn && !uiState.isEmailVerified &&
            currentRoute != Routes.VERIFY_EMAIL && (currentRoute !in Routes.POST_AUTH_ROUTES)
        ) {
            navController.navigate(Routes.VERIFY_EMAIL) { popUpTo(0) }
        }
    }
}
