package com.sivan.brickradar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sivan.brickradar.ui.AddModelScreen
import com.sivan.brickradar.ui.ModelDetailScreen
import com.sivan.brickradar.ui.ModelListScreen
import com.sivan.brickradar.ui.StatistikScreen
import com.sivan.brickradar.ui.UpdateChecker
import com.sivan.brickradar.ui.theme.BrickRadarTheme

// Nyckel för resultatet "en modell skapades" i AddModelScreens anropande
// backstack-entry (modelList) — se ModelListScreen.modelCreatedFlow.
private const val MODEL_CREATED_KEY = "model_created"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BrickRadarTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Kollar en gång vid appstart om servern har en nyare
                    // version (se UpdateViewModel.init) — renderas ovanpå
                    // NavHost, oberoende av vilken skärm som just nu visas.
                    UpdateChecker()
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "modelList") {
                        composable("modelList") { backStackEntry ->
                            ModelListScreen(
                                onModelClick = { modelId ->
                                    navController.navigate("modelDetail/$modelId")
                                },
                                onAddModelClick = { navController.navigate("addModel") },
                                onStatistikClick = { navController.navigate("statistik") },
                                modelCreatedFlow = backStackEntry.savedStateHandle
                                    .getStateFlow(MODEL_CREATED_KEY, false),
                                onModelCreatedConsumed = {
                                    backStackEntry.savedStateHandle[MODEL_CREATED_KEY] = false
                                },
                            )
                        }
                        composable("statistik") {
                            StatistikScreen(
                                onBack = { navController.popBackStack() },
                                onModelClick = { modelId -> navController.navigate("modelDetail/$modelId") },
                            )
                        }
                        composable(
                            route = "modelDetail/{modelId}",
                            arguments = listOf(navArgument("modelId") { type = NavType.IntType }),
                        ) { backStackEntry ->
                            val modelId = backStackEntry.arguments?.getInt("modelId") ?: return@composable
                            ModelDetailScreen(modelId = modelId, onDeleted = { navController.popBackStack() })
                        }
                        composable("addModel") {
                            AddModelScreen(
                                onSaved = {
                                    navController.previousBackStackEntry?.savedStateHandle
                                        ?.set(MODEL_CREATED_KEY, true)
                                    navController.popBackStack()
                                },
                                onCancel = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }
}
