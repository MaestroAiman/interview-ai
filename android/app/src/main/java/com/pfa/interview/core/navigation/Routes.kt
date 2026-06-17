package com.pfa.interview.core.navigation

sealed class Routes(val route: String) {
    object Splash : Routes("splash")
    object Onboarding : Routes("onboarding")
    object Login : Routes("login")
    object Register : Routes("register")
    object Home : Routes("home")
    object Setup : Routes("setup")
    object Session : Routes("session/{sessionId}") {
        fun createRoute(id: String) = "session/$id"
    }
    object Results : Routes("results/{sessionId}") {
        fun createRoute(id: String) = "results/$id"
    }
    object History : Routes("history")
    object Progress : Routes("progress")
    object Profile : Routes("profile")
    object AdminUsers : Routes("admin/users")
    object AdminDashboard : Routes("admin/dashboard")
    object HistoryDetails : Routes("history/{sessionId}/details") {
        fun createRoute(sessionId: String) = "history/$sessionId/details"
    }
}
