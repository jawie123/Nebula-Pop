package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.game.BubbleGameViewModel
import com.example.ui.GameScreen
import com.example.ui.HomeScreen
import com.example.ui.SectorMapScreen
import com.example.ui.theme.BubbleSphereOdysseyTheme
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object SectorMapRoute

@Serializable
data class GameRoute(
    val isEndless: Boolean,
    val sectorNumber: Int = 1
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            BubbleSphereOdysseyTheme {
                val navController = rememberNavController()
                val viewModel: BubbleGameViewModel = viewModel()
                val progress by viewModel.progress.collectAsStateWithLifecycle()

                NavHost(
                    navController = navController,
                    startDestination = HomeRoute
                ) {
                    composable<HomeRoute> {
                        HomeScreen(
                            progress = progress,
                            onPlaySector = { sector ->
                                viewModel.startSector(sector)
                                navController.navigate(GameRoute(isEndless = false, sectorNumber = sector))
                            },
                            onOpenSectorMap = {
                                navController.navigate(SectorMapRoute)
                            },
                            onPlayEndless = {
                                viewModel.startEndless()
                                navController.navigate(GameRoute(isEndless = true))
                            },
                            onToggleSound = viewModel::toggleSound,
                            onToggleHaptics = viewModel::toggleHaptics,
                            onToggleColorblind = viewModel::toggleColorblind
                        )
                    }

                    composable<SectorMapRoute> {
                        SectorMapScreen(
                            progress = progress,
                            onSelectSector = { sector ->
                                viewModel.startSector(sector)
                                navController.navigate(GameRoute(isEndless = false, sectorNumber = sector))
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<GameRoute> { backStackEntry ->
                        val route = backStackEntry.toRoute<GameRoute>()
                        GameScreen(
                            viewModel = viewModel,
                            onOpenSectorMap = {
                                navController.popBackStack(HomeRoute, inclusive = false)
                                navController.navigate(SectorMapRoute)
                            },
                            onExitToHub = {
                                navController.popBackStack(HomeRoute, inclusive = false)
                            }
                        )
                    }
                }
            }
        }
    }
}
