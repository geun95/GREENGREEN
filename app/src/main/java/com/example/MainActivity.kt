package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.PlantViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-Edge 지원 활성화 (디자인 가이드라인 최우선 요건)
        enableEdgeToEdge()
        
        val viewModel = ViewModelProvider(this)[PlantViewModel::class.java]

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // 전체 바의 표출 유무 결정 (하위 세부 등록/분석 중 화면에서는 가려줌)
                val isBottomBarVisible = remember(currentRoute) {
                    currentRoute == "home" || currentRoute == "my_plants" || currentRoute == "schedule"
                }

                Scaffold(
                    bottomBar = {
                        if (isBottomBarVisible) {
                            // M3 네비게이션 컨테이너
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 8.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("bottom_nav_bar")
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                            ) {
                                // 1. 홈 탭
                                NavigationBarItem(
                                    selected = currentRoute == "home",
                                    onClick = {
                                        navController.navigate("home") {
                                            popUpTo("home") { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentRoute == "home") Icons.Filled.Home else Icons.Outlined.Home,
                                            contentDescription = "홈"
                                        )
                                    },
                                    label = { Text("홈", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = GreenPrimary,
                                        selectedTextColor = GreenPrimary,
                                        indicatorColor = GreenSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("nav_home")
                                )

                                // 2. 내 식물 탭
                                NavigationBarItem(
                                    selected = currentRoute == "my_plants",
                                    onClick = {
                                        navController.navigate("my_plants") {
                                            popUpTo("home") { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentRoute == "my_plants") Icons.Filled.Eco else Icons.Outlined.Eco,
                                            contentDescription = "내 식물"
                                        )
                                    },
                                    label = { Text("내 식물", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = GreenPrimary,
                                        selectedTextColor = GreenPrimary,
                                        indicatorColor = GreenSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("nav_my_plants")
                                )

                                // 3. 일정 탭
                                NavigationBarItem(
                                    selected = currentRoute == "schedule",
                                    onClick = {
                                        navController.navigate("schedule") {
                                            popUpTo("home") { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentRoute == "schedule") Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                                            contentDescription = "일정"
                                        )
                                    },
                                    label = { Text("일정", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = GreenPrimary,
                                        selectedTextColor = GreenPrimary,
                                        indicatorColor = GreenSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("nav_schedule")
                                )
                            }
                        }
                    },
                    floatingActionButton = {
                        if (isBottomBarVisible) {
                            FloatingActionButton(
                                onClick = { navController.navigate("add_plant") },
                                shape = CircleShape,
                                containerColor = GreenPrimary,
                                contentColor = Color.White,
                                elevation = FloatingActionButtonDefaults.elevation(6.dp),
                                modifier = Modifier
                                    .size(56.dp)
                                    .testTag("center_add_plant_fab")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "식물 등록",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToMyPlants = { navController.navigate("my_plants") },
                                modifier = Modifier.padding(bottom = if (isBottomBarVisible) 48.dp else 0.dp)
                            )
                        }
                        composable("my_plants") {
                            MyPlantsScreen(
                                viewModel = viewModel,
                                onNavigateToAddPlant = { navController.navigate("add_plant") },
                                onNavigateToDetail = { id -> navController.navigate("plant_detail/$id") },
                                modifier = Modifier.padding(bottom = if (isBottomBarVisible) 48.dp else 0.dp)
                            )
                        }
                        composable("add_plant") {
                            AddPlantScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onPlantCreated = {
                                    navController.navigate("my_plants") {
                                        popUpTo("home") { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                        composable("plant_detail/{plantId}") { backStackEntry ->
                            val id = backStackEntry.arguments?.getString("plantId")?.toIntOrNull() ?: 0
                            DetailScreen(
                                plantId = id,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToChat = { plantId -> navController.navigate("chat/$plantId") }
                            )
                        }
                        composable("chat/{plantId}") { backStackEntry ->
                            val id = backStackEntry.arguments?.getString("plantId")?.toIntOrNull() ?: 0
                            val plants by viewModel.allPlants.collectAsStateWithLifecycle()
                            val plant = plants.find { it.id == id }
                            if (plant != null) {
                                ChatScreen(
                                    plant = plant,
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }
                        composable("schedule") {
                            ScheduleScreen(
                                viewModel = viewModel,
                                onNavigateToDetail = { id -> navController.navigate("plant_detail/$id") },
                                modifier = Modifier.padding(bottom = if (isBottomBarVisible) 48.dp else 0.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
