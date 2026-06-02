package com.myreader.app.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.*
import androidx.navigation.compose.*
import com.myreader.app.presentation.auth.LoginScreen
import com.myreader.app.presentation.auth.RegisterScreen
import com.myreader.app.presentation.auth.ForgotPasswordScreen
import com.myreader.app.presentation.auth.OnboardingScreen
import com.myreader.app.presentation.home.HomeScreen
import com.myreader.app.presentation.bookdetail.BookDetailScreen
import com.myreader.app.presentation.reader.ReaderScreen
import com.myreader.app.presentation.library.LibraryScreen
import com.myreader.app.presentation.profile.ProfileScreen
import com.myreader.app.presentation.admin.AdminDashboardScreen
import com.myreader.app.presentation.home.SearchScreen
import com.myreader.app.presentation.home.CategoryScreen

// ─── Route Definitions ────────────────────────────────────────────────────
sealed class Screen(val route: String) {
    // Auth
    object Onboarding      : Screen("onboarding")
    object Login           : Screen("login")
    object Register        : Screen("register")
    object ForgotPassword  : Screen("forgot_password")

    // Main
    object Home            : Screen("home")
    object Search          : Screen("search")
    object Category        : Screen("category/{categoryName}") {
        fun createRoute(name: String) = "category/$name"
    }
    object BookDetail      : Screen("book/{bookId}") {
        fun createRoute(id: String) = "book/$id"
    }
    object Reader          : Screen("reader/{bookId}?isDownloaded={isDownloaded}") {
        fun createRoute(id: String, downloaded: Boolean = false) = "reader/$id?isDownloaded=$downloaded"
    }

    // Tabs
    object Library         : Screen("library")
    object Profile         : Screen("profile")

    // Admin
    object AdminDashboard  : Screen("admin")
    object AdminAddBook    : Screen("admin/add_book")
    object AdminEditBook   : Screen("admin/edit_book/{bookId}") {
        fun createRoute(id: String) = "admin/edit_book/$id"
    }
    object AdminUsers      : Screen("admin/users")
    object AdminAnalytics  : Screen("admin/analytics")
}

// ─── Main Navigation Graph ────────────────────────────────────────────────
@Composable
fun MyReaderNavGraph(
    navController: NavHostController,
    startDestination: String,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeIn(tween(300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = tween(300)
            ) + fadeOut(tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = tween(300)
            ) + fadeIn(tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeOut(tween(300))
        }
    ) {
        // ── Auth ──────────────────────────────────────────────────────────
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }

        // ── Home ──────────────────────────────────────────────────────────
        composable(Screen.Home.route) {
            HomeScreen(
                onBookClick  = { navController.navigate(Screen.BookDetail.createRoute(it)) },
                onSearchClick = { navController.navigate(Screen.Search.route) },
                onCategoryClick = { navController.navigate(Screen.Category.createRoute(it)) },
                onLibraryClick = { navController.navigate(Screen.Library.route) },
                onProfileClick = { navController.navigate(Screen.Profile.route) },
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(
                onBookClick = { navController.navigate(Screen.BookDetail.createRoute(it)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Screen.Category.route,
            arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
        ) { entry ->
            CategoryScreen(
                categoryName = entry.arguments?.getString("categoryName") ?: "",
                onBookClick  = { navController.navigate(Screen.BookDetail.createRoute(it)) },
                onBack       = { navController.popBackStack() },
            )
        }

        // ── Book Detail ───────────────────────────────────────────────────
        composable(
            route = Screen.BookDetail.route,
            arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "https://myreader.app/book/{bookId}" })
        ) { entry ->
            BookDetailScreen(
                bookId = entry.arguments?.getString("bookId") ?: "",
                onReadClick     = { navController.navigate(Screen.Reader.createRoute(it)) },
                onDownloadRead  = { navController.navigate(Screen.Reader.createRoute(it, true)) },
                onBack          = { navController.popBackStack() },
            )
        }

        // ── Reader ────────────────────────────────────────────────────────
        composable(
            route = Screen.Reader.route,
            arguments = listOf(
                navArgument("bookId")       { type = NavType.StringType },
                navArgument("isDownloaded") { type = NavType.BoolType; defaultValue = false }
            )
        ) { entry ->
            ReaderScreen(
                bookId       = entry.arguments?.getString("bookId") ?: "",
                isDownloaded = entry.arguments?.getBoolean("isDownloaded") ?: false,
                onBack       = { navController.popBackStack() },
            )
        }

        // ── Library & Profile ─────────────────────────────────────────────
        composable(Screen.Library.route) {
            LibraryScreen(
                onBookClick = { navController.navigate(Screen.BookDetail.createRoute(it)) },
                onReadClick = { bookId, downloaded ->
                    navController.navigate(Screen.Reader.createRoute(bookId, downloaded))
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                onBack         = { navController.popBackStack() },
                onAdminClick   = { navController.navigate(Screen.AdminDashboard.route) },
                onLogout       = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        // ── Admin ─────────────────────────────────────────────────────────
        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(
                onBack       = { navController.popBackStack() },
                onAddBook    = { navController.navigate(Screen.AdminAddBook.route) },
                onEditBook   = { navController.navigate(Screen.AdminEditBook.createRoute(it)) },
                onUsersClick = { navController.navigate(Screen.AdminUsers.route) },
            )
        }
    }
}
