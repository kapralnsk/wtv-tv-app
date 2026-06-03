package tv.wtv.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import tv.wtv.app.ui.player.PlayerScreen

private object Routes {
    const val PLAYER = "player/{channelName}"
    fun player(channelName: String) = "player/$channelName"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.player("dunduk")) {
        composable(
            route = Routes.PLAYER,
            arguments = listOf(navArgument("channelName") { type = NavType.StringType })
        ) { backStackEntry ->
            val channelName = backStackEntry.arguments?.getString("channelName") ?: return@composable
            PlayerScreen(
                channelName = channelName,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
