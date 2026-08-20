package com.example.nargesapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nargesapp.ui.screens.AddDebtScreen
import com.example.nargesapp.ui.screens.AddTransactionScreen
import com.example.nargesapp.ui.screens.CardsScreen
import com.example.nargesapp.ui.screens.DebtDetailScreen
import com.example.nargesapp.ui.screens.LoanDetailScreen
import com.example.nargesapp.ui.screens.DebtsScreen
import com.example.nargesapp.ui.screens.HomeScreen
import com.example.nargesapp.ui.screens.LockScreen
import com.example.nargesapp.ui.screens.LockSetupScreen
import com.example.nargesapp.ui.screens.MoreScreen
import com.example.nargesapp.ui.screens.NotificationsScreen
import com.example.nargesapp.ui.screens.ReportsScreen
import com.example.nargesapp.ui.screens.SettingsScreen
import com.example.nargesapp.ui.screens.ShoppingListScreen
import com.example.nargesapp.ui.screens.TransactionScreen
import com.example.nargesapp.ui.theme.AppTypography
import com.example.nargesapp.ui.theme.BackgroundLight
import com.example.nargesapp.ui.theme.CardWhite
import com.example.nargesapp.ui.theme.PrimaryGreen
import com.example.nargesapp.ui.utils.LockPreferences
import com.example.nargesapp.ui.viewmodel.TransactionViewModel

class MainActivity : FragmentActivity() {

    private var shouldLock = mutableStateOf(false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* نتیجه نیازی به پردازش خاصی ندارد؛ اگر رد شود، Worker خودش هنگام نمایش اعلان چک می‌کند */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(android.app.AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                try {
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        android.net.Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                } catch (e: Exception) {
                    // برخی دستگاه‌ها این صفحه‌ی تنظیمات را ندارند؛ در این صورت بی‌خطر رد می‌شویم
                }
            }
        }

        // مقداردهی اولیه‌ی تمام ریپازیتوری‌ها اکنون در NargesApplication.onCreate انجام می‌شود
        // تا حتی بدون باز شدن این اکتیویتی نیز (مثلاً هنگام فعال شدن زنگ AlarmManager در پس‌زمینه) در دسترس باشند

        setContent {
            val navController = rememberNavController()
            val viewModel: TransactionViewModel = viewModel()
            val context = LocalContext.current

            var isUnlocked by remember {
                mutableStateOf(
                    !LockPreferences.isLockEnabled(context)
                )
            }

            LaunchedEffect(shouldLock.value) {
                if (
                    shouldLock.value &&
                    LockPreferences.isLockEnabled(context)
                ) {
                    isUnlocked = false
                    shouldLock.value = false
                }
            }

            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    background = BackgroundLight,
                    primary = PrimaryGreen,
                    surface = CardWhite
                ),
                typography = AppTypography
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundLight
                ) {
                    if (!isUnlocked) {
                        LockScreen(
                            onUnlocked = {
                                isUnlocked = true
                            }
                        )
                    } else {
                        NavHost(
                            navController = navController,
                            startDestination = "home",
                            enterTransition = {
                                fadeIn(
                                    animationSpec = tween(280)
                                )
                            },
                            exitTransition = {
                                fadeOut(
                                    animationSpec = tween(280)
                                )
                            },
                            popEnterTransition = {
                                fadeIn(
                                    animationSpec = tween(280)
                                )
                            },
                            popExitTransition = {
                                fadeOut(
                                    animationSpec = tween(280)
                                )
                            }
                        ) {
                            composable("home") {
                                HomeScreen(
                                    navController,
                                    viewModel
                                )
                            }

                            composable("transactions") {
                                TransactionScreen(
                                    navController,
                                    viewModel
                                )
                            }

                            composable("reports") {
                                ReportsScreen(
                                    navController,
                                    viewModel
                                )
                            }

                            composable("more") {
                                MoreScreen(navController)
                            }

                            composable("cards") {
                                CardsScreen(
                                    navController,
                                    viewModel
                                )
                            }

                            composable("shopping") {
                                ShoppingListScreen(navController)
                            }

                            composable("lock_setup") {
                                LockSetupScreen(navController)
                            }

                            composable("settings") {
                                SettingsScreen(navController)
                            }

                            composable("debts") {
                                DebtsScreen(navController)
                            }
                            composable("notifications") {
                                NotificationsScreen(navController)
                            }
composable("add_debt") {
    AddDebtScreen(
        navController = navController
    )
}
                            composable(
    route = "add_debt/{type}/{title}/{amount}/{date}/{note}",
    arguments = listOf(
        navArgument("type") {
            type = NavType.StringType
        },
        navArgument("title") {
            type = NavType.StringType
        },
        navArgument("amount") {
            type = NavType.LongType
        },
        navArgument("date") {
            type = NavType.StringType
        },
        navArgument("note") {
            type = NavType.StringType
        }
    )
) { backStackEntry ->
    AddDebtScreen(
        navController = navController,
        initialType = backStackEntry.arguments
            ?.getString("type")
            ?: "payable",
        initialTitle = backStackEntry.arguments
            ?.getString("title")
            ?: "",
        initialAmount = backStackEntry.arguments
            ?.getLong("amount")
            ?: 0L,
        initialDate = backStackEntry.arguments
            ?.getString("date")
            ?: "",
        initialNote = backStackEntry.arguments
            ?.getString("note")
            ?: ""
    )
}

                            composable(
                                "debt_detail/{debtId}"
                            ) { backStackEntry ->
                                val debtId = backStackEntry.arguments
                                    ?.getString("debtId")
                                    ?.toIntOrNull()
                                    ?: -1

                                DebtDetailScreen(
                                    navController = navController,
                                    debtId = debtId
                                )
                            }

                            composable(
                                "loan_detail/{loanGroupId}"
                            ) { backStackEntry ->
                                val loanGroupId = backStackEntry.arguments
                                    ?.getString("loanGroupId")
                                    ?: ""

                                LoanDetailScreen(
                                    navController = navController,
                                    loanGroupId = loanGroupId
                                )
                            }

                            composable("add_transaction") {
                                AddTransactionScreen(
                                    navController,
                                    viewModel
                                )
                            }

                            composable(
                                "edit_transaction/{transactionId}"
                            ) { backStackEntry ->
                                val id = backStackEntry.arguments
                                    ?.getString("transactionId")
                                    ?.toIntOrNull()
                                    ?: -1

                                AddTransactionScreen(
                                    navController = navController,
                                    viewModel = viewModel,
                                    transactionId = id
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        shouldLock.value = true
    }
}