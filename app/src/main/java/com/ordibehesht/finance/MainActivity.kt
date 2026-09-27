package com.ordibehesht.finance

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
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
import com.ordibehesht.finance.ui.screens.AddDebtScreen
import com.ordibehesht.finance.ui.screens.AddTransactionScreen
import com.ordibehesht.finance.ui.screens.CardsScreen
import com.ordibehesht.finance.ui.screens.DebtDetailScreen
import com.ordibehesht.finance.ui.screens.LoanDetailScreen
import com.ordibehesht.finance.ui.screens.DebtsScreen
import com.ordibehesht.finance.ui.screens.HomeScreen
import com.ordibehesht.finance.ui.screens.LockScreen
import com.ordibehesht.finance.ui.screens.LockSetupScreen
import com.ordibehesht.finance.ui.screens.MoreScreen
import com.ordibehesht.finance.ui.screens.OnboardingScreen
import com.ordibehesht.finance.dong.DongListScreen
import com.ordibehesht.finance.dong.DongGroupScreen
import com.ordibehesht.finance.ui.screens.NotificationsScreen
import com.ordibehesht.finance.ui.screens.ReportsScreen
import com.ordibehesht.finance.ui.screens.SettingsScreen
import com.ordibehesht.finance.ui.screens.ShoppingListScreen
import com.ordibehesht.finance.ui.screens.TransactionScreen
import com.ordibehesht.finance.ui.theme.AppTypography
import com.ordibehesht.finance.ui.theme.BackgroundLight
import com.ordibehesht.finance.ui.theme.CardWhite
import com.ordibehesht.finance.ui.theme.PrimaryGreen
import com.ordibehesht.finance.ui.utils.LockPreferences
import com.ordibehesht.finance.ui.utils.OnboardingPreferences
import com.ordibehesht.finance.ui.viewmodel.TransactionViewModel

class MainActivity : FragmentActivity() {

    private var shouldLock = mutableStateOf(false)

    // مسیری که باید بعد از باز شدن (و در صورت نیاز، رفع قفل) اپ به آن برویم؛
    // از کلیک روی اعلان یادآوری بدهی/طلب پر می‌شود (ر.ک. DebtReminderAlarmReceiver)
    private var pendingNavigation = mutableStateOf<String?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* نتیجه نیازی به پردازش خاصی ندارد؛ اگر رد شود، Worker خودش هنگام نمایش اعلان چک می‌کند */ }

    private fun consumeNavigateToExtra(intent: android.content.Intent?) {
        val route = intent?.getStringExtra("navigate_to") ?: return
        pendingNavigation.value = route
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        // وقتی اکتیویتی از قبل در پشته وجود دارد (مثلاً اپ باز است) و کاربر روی اعلان می‌زند،
        // Intent جدید از همین‌جا می‌رسد، نه از onCreate
        setIntent(intent)
        consumeNavigateToExtra(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        consumeNavigateToExtra(intent)

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

            // هر بار مسیر pending تغییر کند (اعلان جدید زده شده) و صفحه قفل نباشد،
            // به همان صفحه (مثلاً جزئیات بدهی) می‌رویم و سپس pending را پاک می‌کنیم
            // تا با چرخش صفحه یا رویدادهای بعدی دوباره navigate نشود
            LaunchedEffect(pendingNavigation.value, isUnlocked) {
                val route = pendingNavigation.value
                if (route != null && isUnlocked) {
                    navController.navigate(route)
                    pendingNavigation.value = null
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
                            // راهنمای اولیه (Onboarding) فقط در اولین اجرای اپ روی این دستگاه
                            // نمایش داده می‌شود؛ در اجراهای بعدی مستقیم به «خانه» می‌رویم
                            // (ر.ک. OnboardingPreferences).
                            startDestination = if (OnboardingPreferences.hasSeenOnboarding(context)) {
                                "home"
                            } else {
                                "onboarding"
                            },
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
                            composable("onboarding") {
                                OnboardingScreen(
                                    onFinished = {
                                        OnboardingPreferences.setOnboardingSeen(context)
                                        // چون در این حالت startDestination خودِ «onboarding» است،
                                        // popUpTo(startDestination) با inclusive=true دقیقاً همان
                                        // یک ورودی را از استک پاک می‌کند تا با دکمه‌ی برگشت
                                        // فیزیکی، کاربر دوباره به این صفحه برنگردد
                                        navController.navigate("home") {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                inclusive = true
                                            }
                                        }
                                    }
                                )
                            }

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

                            composable("dong_list") {
                                DongListScreen(navController)
                            }

                            composable(
                                route = "dong_group/{groupId}",
                                arguments = listOf(
                                    navArgument("groupId") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                DongGroupScreen(
                                    navController = navController,
                                    groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                                )
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

                            composable(
                                route = "debts?filterDate={filterDate}",
                                arguments = listOf(
                                    navArgument("filterDate") {
                                        type = NavType.StringType
                                        nullable = true
                                    }
                                )
                            ) { backStackEntry ->
                                DebtsScreen(
                                    navController = navController,
                                    filterDate = backStackEntry.arguments?.getString("filterDate")
                                )
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

    // onStop فقط وقتی صدا زده می‌شود که اکتیویتی واقعاً از دید کاربر خارج شده باشد
    // (رفتن به اپ دیگر، هوم‌اسکرین، و غیره) — برخلاف onPause/onResume که برای وقفه‌های
    // موقتی مثل دیالوگ‌های سیستمی (درخواست مجوز، صفحه‌ی تنظیمات زنگ دقیق) هم فایر می‌شوند.
    // اگر قفل را در onResume می‌گذاشتیم، همان دیالوگ‌های سیستمی که خود اپ در onCreate باز
    // می‌کند باعث می‌شدند کاربر بلافاصله دوباره صفحه‌ی اثرانگشت را ببیند، بدون این‌که واقعاً
    // از اپ خارج شده باشد.
    override fun onStop() {
        super.onStop()
        shouldLock.value = true
    }
}