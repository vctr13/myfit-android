package com.example.myfit.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myfit.ui.archive.ArchiveScreen
import com.example.myfit.ui.archive.DayDetailScreen
import com.example.myfit.ui.chat.ChatScreen
import com.example.myfit.ui.diet.DietScreen
import com.example.myfit.ui.fitness.FitnessScreen
import com.example.myfit.ui.home.HomeScreen
import com.example.myfit.ui.nav.Screen
import com.example.myfit.ui.pedometer.PedometerScreen
import com.example.myfit.ui.products.MyProductsScreen
import com.example.myfit.ui.settings.SettingsScreen
import com.example.myfit.ui.update.UpdateState
import com.example.myfit.ui.update.UpdateViewModel
import kotlinx.coroutines.launch

private data class DrawerItem(val screen: Screen, val label: String, val icon: ImageVector)

private val mainItems = listOf(
    DrawerItem(Screen.Home, "Главная", Icons.Filled.Home),
    DrawerItem(Screen.Fitness, "Фитнес", Icons.Filled.FitnessCenter),
    DrawerItem(Screen.Diet, "Диета", Icons.Filled.Restaurant),
    DrawerItem(Screen.Chat, "Чат с AI", Icons.Filled.Chat),
    DrawerItem(Screen.Pedometer, "Шагомер", Icons.Filled.DirectionsWalk),
    DrawerItem(Screen.Archive, "Архив питания", Icons.Filled.DateRange),
)

@Composable
fun MainScreen() {
    val navController  = rememberNavController()
    val drawerState    = rememberDrawerState(DrawerValue.Closed)
    val scope          = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest    = backStackEntry?.destination
    val updateVm: UpdateViewModel = viewModel()
    var showUpdateDialog by remember { mutableStateOf(false) }

    fun openDrawer()  = scope.launch { drawerState.open() }
    fun closeDrawer() = scope.launch { drawerState.close() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                Text("MyFIT", style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                Text("AI-нутрициолог", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp))
                HorizontalDivider(Modifier.padding(vertical = 10.dp))

                mainItems.forEach { item ->
                    val selected = currentDest?.hierarchy?.any { it.route == item.screen.route } == true
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                        selected = selected,
                        onClick = {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                            closeDrawer()
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors   = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            selectedIconColor      = MaterialTheme.colorScheme.onTertiaryContainer,
                            selectedTextColor      = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    )
                }

                HorizontalDivider(Modifier.padding(vertical = 10.dp))

                val settingsSelected = currentDest?.hierarchy?.any { it.route == Screen.Settings.route } == true
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("Настройки") },
                    selected = settingsSelected,
                    onClick = {
                        navController.navigate(Screen.Settings.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                        closeDrawer()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    colors   = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        selectedIconColor      = MaterialTheme.colorScheme.onTertiaryContainer,
                        selectedTextColor      = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                )

                NavigationDrawerItem(
                    icon     = { Icon(Icons.Filled.SystemUpdate, contentDescription = null) },
                    label    = { Text("Обновления") },
                    selected = false,
                    onClick  = {
                        showUpdateDialog = true
                        closeDrawer()
                        updateVm.checkForUpdates()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        if (showUpdateDialog) {
            UpdateDialog(
                vm        = updateVm,
                onDismiss = { showUpdateDialog = false; updateVm.reset() }
            )
        }

        NavHost(navController = navController, startDestination = Screen.Home.route) {
            composable(Screen.Home.route) {
                HomeScreen(onOpenDrawer = { openDrawer() })
            }
            composable(Screen.Fitness.route) {
                FitnessScreen(onOpenDrawer = { openDrawer() })
            }
            composable(Screen.Diet.route) {
                DietScreen(onOpenDrawer = { openDrawer() })
            }
            composable(Screen.Chat.route) {
                ChatScreen(onOpenDrawer = { openDrawer() })
            }
            composable(Screen.Pedometer.route) {
                PedometerScreen(onOpenDrawer = { openDrawer() })
            }
            composable(Screen.Archive.route) {
                ArchiveScreen(
                    onBack = { navController.popBackStack() },
                    onDayClick = { date -> navController.navigate(Screen.DayDetail.createRoute(date)) }
                )
            }
            composable(
                route = Screen.DayDetail.route,
                arguments = listOf(navArgument(Screen.DayDetail.ARG) { type = NavType.StringType })
            ) { backStack ->
                val date = backStack.arguments?.getString(Screen.DayDetail.ARG) ?: return@composable
                DayDetailScreen(date = date, onBack = { navController.popBackStack() })
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onOpenDrawer = { openDrawer() },
                    onNavigateToMyProducts = { navController.navigate(Screen.MyProducts.route) }
                )
            }
            composable(Screen.MyProducts.route) {
                MyProductsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

// ── Update dialog ─────────────────────────────────────────────────────────────

@Composable
private fun UpdateDialog(vm: UpdateViewModel, onDismiss: () -> Unit) {
    val state = vm.state

    // Prevent accidental dismiss during download
    val dismissible = state !is UpdateState.Downloading

    AlertDialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        title = { Text("Обновления") },
        text  = {
            Column(
                modifier            = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (state) {
                    is UpdateState.Idle, is UpdateState.Checking -> {
                        Spacer(Modifier.height(8.dp))
                        CircularProgressIndicator(modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Проверяем обновления…", style = MaterialTheme.typography.bodyMedium)
                    }

                    is UpdateState.UpToDate -> {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint     = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Обновлений нет", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Текущая версия: ${state.version}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    is UpdateState.UpdateAvailable -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                "Доступна версия ${state.release.versionName}",
                                style      = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Текущая: ${vm.currentVersion}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (state.release.changelog.isNotBlank()) {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "Что нового:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(state.release.changelog, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    is UpdateState.Downloading -> {
                        Spacer(Modifier.height(8.dp))
                        CircularProgressIndicator(modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Загружаем обновление…", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Не закрывайте это окно",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    is UpdateState.NeedsPermission -> {
                        Text(
                            "Разрешите установку из неизвестных источников для VFit.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "После выдачи разрешения вернитесь и нажмите «Обновить» снова.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    is UpdateState.Error -> {
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (state) {
                is UpdateState.UpdateAvailable ->
                    Button(onClick = { vm.downloadAndInstall(state.release) }) {
                        Text("Обновить")
                    }
                is UpdateState.NeedsPermission ->
                    Button(onClick = { vm.openInstallPermissionSettings() }) {
                        Text("Открыть настройки")
                    }
                is UpdateState.Downloading -> { /* кнопок нет */ }
                else ->
                    TextButton(onClick = onDismiss) { Text("Закрыть") }
            }
        },
        dismissButton = {
            if (state is UpdateState.UpdateAvailable || state is UpdateState.NeedsPermission) {
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        }
    )
}
