package com.example.bakalariapp.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bakalariapp.data.preferences.MarksCacheDataStore
import com.example.bakalariapp.data.preferences.TokenDataStore
import com.example.bakalariapp.ui.screens.LoginScreen
import com.example.bakalariapp.ui.screens.MarksDetailScreen
import com.example.bakalariapp.ui.screens.SubjectsScreen
import com.example.bakalariapp.ui.viewmodel.LoginViewModel
import com.example.bakalariapp.ui.viewmodel.MarksViewModel
import com.example.bakalariapp.ui.viewmodel.MarksViewModelFactory
import kotlinx.coroutines.flow.first

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Subjects : Screen("subjects")
    data object MarksDetail : Screen("marks_detail")
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val tokenDataStore = remember { TokenDataStore(context) }
    val marksCacheDataStore = remember { MarksCacheDataStore(context) }
    
    // Shared ViewModels at NavGraph level
    val marksViewModel: MarksViewModel = viewModel(
        factory = MarksViewModelFactory(tokenDataStore, marksCacheDataStore)
    )
    val loginViewModel: LoginViewModel = viewModel(
        factory = LoginViewModelFactory(tokenDataStore)
    )
    
    // Determine start destination based on token
    var startDestination by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        val token = tokenDataStore.accessToken.first()
        startDestination = if (!token.isNullOrBlank()) Screen.Subjects.route else Screen.Login.route
    }
    
    // Show simple loading while checking auth
    if (startDestination == null) {
        return
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination!!
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Subjects.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Subjects.route) {
            SubjectsScreen(
                viewModel = marksViewModel,
                onSubjectClick = { subject ->
                    marksViewModel.selectSubject(subject)
                    navController.navigate(Screen.MarksDetail.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Subjects.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(
            route = Screen.MarksDetail.route,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {
            val selectedSubject = marksViewModel.uiState.selectedSubject
            val absencesPerSubject = marksViewModel.uiState.absencesPerSubject
            if (selectedSubject != null) {
                MarksDetailScreen(
                    subject = selectedSubject,
                    absencesPerSubject = absencesPerSubject,
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }
    }
}

class LoginViewModelFactory(private val tokenDataStore: TokenDataStore) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(tokenDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
