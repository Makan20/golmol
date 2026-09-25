package com.ordibehesht.finance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ordibehesht.finance.data.repository.AppResetManager
import com.ordibehesht.finance.data.repository.BackupManager
import com.ordibehesht.finance.ui.theme.BackgroundLight
import com.ordibehesht.finance.ui.theme.CardWhite
import com.ordibehesht.finance.ui.theme.DividerColor
import com.ordibehesht.finance.ui.theme.ExpensePurple
import com.ordibehesht.finance.ui.theme.IncomeGreen
import com.ordibehesht.finance.ui.theme.LightGreen
import com.ordibehesht.finance.ui.theme.PrimaryGreen
import com.ordibehesht.finance.ui.theme.TextPrimary
import com.ordibehesht.finance.ui.theme.TextSecondary
import com.ordibehesht.finance.ui.theme.TextTertiary
import com.ordibehesht.finance.ui.theme.Vazirmatn
import com.ordibehesht.finance.notification.AutoBackupScheduler
import com.ordibehesht.finance.ui.utils.AutoBackupPreferences
import com.ordibehesht.finance.ui.utils.PersianDateUtils
import com.ordibehesht.finance.ui.utils.ReportPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ResetStage { IDLE, LOADING, SUCCESS }
private enum class BackupStage { IDLE, LOADING, SUCCESS }
private enum class RestoreStage { IDLE, LOADING, SUCCESS }
private enum class DeleteFileStage { IDLE, LOADING, SUCCESS }

private fun formatTimeAgoPersian(epochMillis: Long): String {
    val diffMinutes = (System.currentTimeMillis() - epochMillis) / 60_000L
    return when {
        diffMinutes < 60 -> "کمتر از یک ساعت پیش"
        diffMinutes < 60 * 24 -> {
            val hours = diffMinutes / 60
            "${PersianDateUtils.toPersianDigits(hours.toString())} ساعت پیش"
        }
        else -> {
            val days = diffMinutes / (60 * 24)
            "${PersianDateUtils.toPersianDigits(days.toString())} روز پیش"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    var showBackupDialog by remember { mutableStateOf(false) }
    var backupStage by remember { mutableStateOf(BackupStage.IDLE) }
    var showResetDialog by remember { mutableStateOf(false) }
    var resetStage by remember { mutableStateOf(ResetStage.IDLE) }
    var showRestoreListDialog by remember { mutableStateOf(false) }
    var backupFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var pendingDeleteFile by remember { mutableStateOf<File?>(null) }
    // مثل pendingDeleteFile — قبل از بازگردانی واقعی یک بکاپ، اول تأیید کاربر گرفته می‌شود.
    // بازگردانی همه‌ی دیتای فعلی را با محتوای همان فایل جایگزین می‌کند و برگشت‌ناپذیر است، پس
    // نباید فقط با یک لمس اتفاقی اجرا شود
    var pendingRestoreFile by remember { mutableStateOf<File?>(null) }
    var restoreStage by remember { mutableStateOf(RestoreStage.IDLE) }
    var restoringFile by remember { mutableStateOf<File?>(null) }
    var deleteFileStage by remember { mutableStateOf(DeleteFileStage.IDLE) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var resultMessage by remember { mutableStateOf<String?>(null) }
    var resultIsError by remember { mutableStateOf(false) }

    var showAmountInReports by remember {
        mutableStateOf(ReportPreferences.isAmountModeEnabled(context))
    }
    var autoBackupEnabled by remember {
        mutableStateOf(AutoBackupPreferences.isEnabled(context))
    }
    var lastAutoBackupAt by remember {
        mutableStateOf(AutoBackupPreferences.getLastBackupAt(context))
    }

    fun defaultBackupFileName(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US)
        return "narges_backup_${sdf.format(Date())}.json"
    }

    fun refreshBackupList() {
        backupFiles = BackupManager.listBackupFiles(context)
    }

    fun runBackup() {
        scope.launch {
            try {
                backupStage = BackupStage.LOADING
                delay(700)
                withContext(Dispatchers.IO) {
                    BackupManager.saveBackupToFile(context, defaultBackupFileName())
                }
                backupStage = BackupStage.SUCCESS
                delay(1200)
                showBackupDialog = false
                backupStage = BackupStage.IDLE
            } catch (e: Exception) {
                showBackupDialog = false
                backupStage = BackupStage.IDLE
                resultIsError = true
                resultMessage = "خطا در تهیه پشتیبان"
            }
        }
    }

    fun openRestoreList() {
        refreshBackupList()
        showRestoreListDialog = true
    }

    fun runRestore(file: File) {
        if (restoreStage != RestoreStage.IDLE) return
        restoringFile = file
        scope.launch {
            try {
                restoreStage = RestoreStage.LOADING
                delay(700)
                withContext(Dispatchers.IO) { BackupManager.restoreFromFile(file) }
                restoreStage = RestoreStage.SUCCESS
                delay(900)
                showRestoreListDialog = false
                restoreStage = RestoreStage.IDLE
                restoringFile = null
                resultIsError = false
                resultMessage = "بازیابی اطلاعات با موفقیت انجام شد"
            } catch (e: Exception) {
                showRestoreListDialog = false
                restoreStage = RestoreStage.IDLE
                restoringFile = null
                resultIsError = true
                resultMessage = "فایل پشتیبان نامعتبر است"
            }
        }
    }

    fun deleteBackupFile(file: File) {
        if (deleteFileStage != DeleteFileStage.IDLE) return
        scope.launch {
            deleteFileStage = DeleteFileStage.LOADING
            delay(700)
            file.delete()
            refreshBackupList()
            deleteFileStage = DeleteFileStage.SUCCESS
            delay(900)
            pendingDeleteFile = null
            deleteFileStage = DeleteFileStage.IDLE
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "تنظیمات",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontFamily = Vazirmatn
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("more") }) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "بازگشت",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            FlowerDecoration(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 80.dp, end = 4.dp)
                    .size(60.dp)
            )
            FlowerDecoration(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 100.dp, start = 4.dp)
                    .size(50.dp),
                color = ExpensePurple.copy(alpha = 0.08f)
            )

            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(LightGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Payments,
                                        contentDescription = null,
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "نمایش گزارش‌ها بر اساس مبلغ",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = Vazirmatn
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "به‌جای درصد، مبلغ دقیق نمایش داده می‌شود",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextTertiary,
                                        fontFamily = Vazirmatn
                                    )
                                }
                            }

                            CompositionLocalProvider(
                                LocalLayoutDirection provides LayoutDirection.Ltr
                            ) {
                                Switch(
                                    checked = showAmountInReports,
                                    onCheckedChange = {
                                        showAmountInReports = it
                                        ReportPreferences.setAmountMode(context, it)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = CardWhite,
                                        checkedTrackColor = PrimaryGreen,
                                        uncheckedThumbColor = ExpensePurple.copy(alpha = 0.70f),
                                        uncheckedTrackColor = PrimaryGreen.copy(alpha = 0.0f)
                                    ),
                                    modifier = Modifier.scale(0.75f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // پشتیبان‌گیری خودکار هفتگی — دقیقاً هم‌الگو با کارت «نمایش گزارش‌ها بر
                    // اساس مبلغ» بالا (آیکون دایره‌ای رنگی + عنوان/توضیح + سوییچ LTR-wrapped
                    // با همان رنگ‌بندی). فعال/غیرفعال شدن، زمان‌بندی AlarmManager را هم
                    // بلافاصله ثبت/لغو می‌کند
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(LightGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.CloudUpload,
                                        contentDescription = null,
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "پشتیبان‌گیری خودکار هفتگی",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = Vazirmatn
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "هر هفته یک‌بار خودکار؛ حداکثر ۵ فایل اخیر نگه داشته می‌شود",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextTertiary,
                                        fontFamily = Vazirmatn
                                    )
                                    if (autoBackupEnabled && lastAutoBackupAt != null) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            "آخرین بکاپ خودکار: ${formatTimeAgoPersian(lastAutoBackupAt!!)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PrimaryGreen,
                                            fontFamily = Vazirmatn
                                        )
                                    }
                                }
                            }

                            CompositionLocalProvider(
                                LocalLayoutDirection provides LayoutDirection.Ltr
                            ) {
                                Switch(
                                    checked = autoBackupEnabled,
                                    onCheckedChange = { enabled ->
                                        autoBackupEnabled = enabled
                                        AutoBackupPreferences.setEnabled(context, enabled)
                                        if (enabled) {
                                            AutoBackupScheduler.scheduleNext(context)
                                        } else {
                                            AutoBackupScheduler.cancel(context)
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = CardWhite,
                                        checkedTrackColor = PrimaryGreen,
                                        uncheckedThumbColor = ExpensePurple.copy(alpha = 0.70f),
                                        uncheckedTrackColor = PrimaryGreen.copy(alpha = 0.0f)
                                    ),
                                    modifier = Modifier.scale(0.75f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                            SettingsMenuRow(
                                label = "تهیه پشتیبان",
                                icon = Icons.Outlined.CloudUpload,
                                onClick = { showBackupDialog = true }
                            )
                            HorizontalDivider(
                                color = DividerColor,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            SettingsMenuRow(
                                label = "بازیابی پشتیبان",
                                icon = Icons.Outlined.CloudDownload,
                                onClick = { openRestoreList() }
                            )
                            HorizontalDivider(
                                color = DividerColor,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            SettingsMenuRow(
                                label = "پاک کردن دیتا",
                                icon = Icons.Outlined.DeleteForever,
                                onClick = { showResetDialog = true }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // راهنمای اولیه (Onboarding) فقط یک‌بار در اولین اجرای اپ خودکار
                    // نمایش داده می‌شود (ر.ک. OnboardingPreferences در MainActivity)؛
                    // این گزینه به کاربر اجازه می‌دهد هر وقت خواست دوباره آن را ببیند
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                            SettingsMenuRow(
                                label = "نمایش دوباره‌ی راهنما",
                                icon = Icons.Outlined.Eco,
                                onClick = { navController.navigate("onboarding") }
                            )
                        }
                    }
                }
            }

            if (showBackupDialog) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    AlertDialog(
                        containerColor = CardWhite,
                        onDismissRequest = {
                            if (backupStage == BackupStage.IDLE) {
                                showBackupDialog = false
                            }
                        },
                        title = {
                            Text(
                                "تهیه پشتیبان",
                                fontFamily = Vazirmatn,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        text = {
                            Text(
                                "از اطلاعات فعلی شما یک نسخه پشتیبان تهیه می‌شود.",
                                fontFamily = Vazirmatn,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (backupStage == BackupStage.IDLE) {
                                        runBackup()
                                    }
                                },
                                enabled = backupStage == BackupStage.IDLE
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    when (backupStage) {
                                        BackupStage.LOADING -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp,
                                                color = PrimaryGreen
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "...در حال تهیه",
                                                color = ExpensePurple,
                                                fontFamily = Vazirmatn,
                                                fontSize = 13.sp
                                            )
                                        }

                                        BackupStage.SUCCESS -> {
                                            Icon(
                                                Icons.Outlined.CheckCircle,
                                                contentDescription = null,
                                                tint = IncomeGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "انجام شد",
                                                color = IncomeGreen,
                                                fontFamily = Vazirmatn,
                                                fontSize = 13.sp
                                            )
                                        }

                                        BackupStage.IDLE -> {
                                            Text(
                                                "تایید",
                                                color = ExpensePurple,
                                                fontFamily = Vazirmatn,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showBackupDialog = false },
                                enabled = backupStage == BackupStage.IDLE
                            ) {
                                Text("انصراف", fontFamily = Vazirmatn, fontSize = 13.sp)
                            }
                        }
                    )
                }
            }

            if (showResetDialog) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    AlertDialog(
                        containerColor = CardWhite,
                        onDismissRequest = {
                            if (resetStage == ResetStage.IDLE) showResetDialog = false
                        },
                        title = {
                            Text(
                                "پاک کردن همه اطلاعات",
                                fontFamily = Vazirmatn,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        text = {
                            Text(
                                "همه‌ی تراکنش‌ها، کارت‌ها، لیست خریدها، طلب و بدهی‌ها (و پرداخت‌ها و اعلان‌های مرتبط) برای همیشه حذف می‌شوند و این عمل غیرقابل بازگشت است.",
                                fontFamily = Vazirmatn,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (resetStage == ResetStage.IDLE) {
                                        scope.launch {
                                            resetStage = ResetStage.LOADING
                                            delay(700)
                                            AppResetManager.resetAllData()
                                            resetStage = ResetStage.SUCCESS
                                            delay(1200)
                                            showResetDialog = false
                                            resetStage = ResetStage.IDLE
                                        }
                                    }
                                },
                                enabled = resetStage == ResetStage.IDLE
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    when (resetStage) {
                                        ResetStage.LOADING -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp,
                                                color = ExpensePurple
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "...در حال حذف",
                                                color = ExpensePurple,
                                                fontFamily = Vazirmatn,
                                                fontSize = 13.sp
                                            )
                                        }

                                        ResetStage.SUCCESS -> {
                                            Icon(
                                                Icons.Outlined.CheckCircle,
                                                contentDescription = null,
                                                tint = IncomeGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "حذف شد",
                                                color = IncomeGreen,
                                                fontFamily = Vazirmatn,
                                                fontSize = 13.sp
                                            )
                                        }

                                        ResetStage.IDLE -> {
                                            Text(
                                                "حذف کن",
                                                color = ExpensePurple,
                                                fontFamily = Vazirmatn,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showResetDialog = false },
                                enabled = resetStage == ResetStage.IDLE
                            ) {
                                Text("انصراف", fontFamily = Vazirmatn, fontSize = 13.sp)
                            }
                        }
                    )
                }
            }

            if (showRestoreListDialog) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    AlertDialog(
                        containerColor = CardWhite,
                        onDismissRequest = { if (restoreStage == RestoreStage.IDLE) showRestoreListDialog = false },
                        title = {
                            Text(
                                "انتخاب فایل پشتیبان",
                                fontFamily = Vazirmatn,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        text = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            ) {
                                if (backupFiles.isEmpty()) {
                                    Text(
                                        "هیچ فایل پشتیبانی یافت نشد",
                                        fontFamily = Vazirmatn,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.TopCenter)
                                    )
                                } else {
                                    val sdf = remember {
                                        SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)
                                    }
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        backupFiles.forEach { file ->
                                            val isThisFileRestoring = restoringFile == file
                                            CompositionLocalProvider(
                                                LocalLayoutDirection provides LayoutDirection.Ltr
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    IconButton(
                                                        onClick = { pendingDeleteFile = file },
                                                        enabled = restoreStage == RestoreStage.IDLE,
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Outlined.Delete,
                                                            contentDescription = "حذف",
                                                            tint = ExpensePurple,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Column(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable(
                                                                enabled = restoreStage == RestoreStage.IDLE,
                                                                indication = null,
                                                                interactionSource = remember {
                                                                    MutableInteractionSource()
                                                                }
                                                            ) { pendingRestoreFile = file }
                                                    ) {
                                                        Text(
                                                            file.name,
                                                            fontFamily = Vazirmatn,
                                                            fontSize = 12.sp,
                                                            color = TextPrimary,
                                                            fontWeight = FontWeight.Bold,
                                                            textAlign = TextAlign.Right,
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                        // در حین بازیابی همین فایل، به‌جای تاریخ فایل،
                                                        // وضعیت «در حال بازیابی .../بازیابی شد» نشان
                                                        // داده می‌شود — دقیقاً هم‌الگو با دیالوگ‌های
                                                        // تهیه پشتیبان و پاک کردن اطلاعات
                                                        when {
                                                            isThisFileRestoring && restoreStage == RestoreStage.LOADING -> {
                                                                Text(
                                                                    "...در حال بازیابی",
                                                                    fontFamily = Vazirmatn,
                                                                    fontSize = 10.sp,
                                                                    color = ExpensePurple,
                                                                    textAlign = TextAlign.Right,
                                                                    modifier = Modifier.fillMaxWidth()
                                                                )
                                                            }
                                                            isThisFileRestoring && restoreStage == RestoreStage.SUCCESS -> {
                                                                Text(
                                                                    "بازیابی شد",
                                                                    fontFamily = Vazirmatn,
                                                                    fontSize = 10.sp,
                                                                    color = IncomeGreen,
                                                                    textAlign = TextAlign.Right,
                                                                    modifier = Modifier.fillMaxWidth()
                                                                )
                                                            }
                                                            else -> {
                                                                Text(
                                                                    sdf.format(Date(file.lastModified())),
                                                                    fontFamily = Vazirmatn,
                                                                    fontSize = 10.sp,
                                                                    color = TextTertiary,
                                                                    textAlign = TextAlign.Right,
                                                                    modifier = Modifier.fillMaxWidth()
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    when {
                                                        isThisFileRestoring && restoreStage == RestoreStage.LOADING -> {
                                                            CircularProgressIndicator(
                                                                modifier = Modifier.size(18.dp),
                                                                strokeWidth = 2.dp,
                                                                color = ExpensePurple
                                                            )
                                                        }
                                                        isThisFileRestoring && restoreStage == RestoreStage.SUCCESS -> {
                                                            Icon(
                                                                Icons.Outlined.CheckCircle,
                                                                contentDescription = null,
                                                                tint = IncomeGreen,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                        else -> {
                                                            Icon(
                                                                Icons.AutoMirrored.Outlined.InsertDriveFile,
                                                                contentDescription = null,
                                                                tint = PrimaryGreen,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            HorizontalDivider(color = DividerColor)
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = { showRestoreListDialog = false },
                                enabled = restoreStage == RestoreStage.IDLE
                            ) {
                                Text("بستن", fontFamily = Vazirmatn, fontSize = 13.sp)
                            }
                        }
                    )
                }
            }

            if (pendingDeleteFile != null) {
                val fileToDelete = pendingDeleteFile!!
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    AlertDialog(
                        containerColor = CardWhite,
                        onDismissRequest = { if (deleteFileStage == DeleteFileStage.IDLE) pendingDeleteFile = null },
                        title = {
                            Text(
                                "حذف فایل پشتیبان",
                                fontFamily = Vazirmatn,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        text = {
                            Text(
                                "فایل «${fileToDelete.name}» برای همیشه حذف می‌شود.",
                                fontFamily = Vazirmatn,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = { deleteBackupFile(fileToDelete) },
                                enabled = deleteFileStage == DeleteFileStage.IDLE
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    when (deleteFileStage) {
                                        DeleteFileStage.LOADING -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp,
                                                color = ExpensePurple
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "...در حال حذف",
                                                color = ExpensePurple,
                                                fontFamily = Vazirmatn,
                                                fontSize = 13.sp
                                            )
                                        }
                                        DeleteFileStage.SUCCESS -> {
                                            Icon(
                                                Icons.Outlined.CheckCircle,
                                                contentDescription = null,
                                                tint = IncomeGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "حذف شد",
                                                color = IncomeGreen,
                                                fontFamily = Vazirmatn,
                                                fontSize = 13.sp
                                            )
                                        }
                                        DeleteFileStage.IDLE -> {
                                            Text(
                                                "حذف کن",
                                                color = ExpensePurple,
                                                fontFamily = Vazirmatn,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { pendingDeleteFile = null },
                                enabled = deleteFileStage == DeleteFileStage.IDLE
                            ) {
                                Text("انصراف", fontFamily = Vazirmatn, fontSize = 13.sp)
                            }
                        }
                    )
                }
            }

            // تأیید قبل از بازگردانی — چون این کار همه‌ی دیتای فعلی (تراکنش‌ها، حساب‌ها،
            // طلب/بدهی، لیست خرید) را با محتوای فایل انتخاب‌شده جایگزین می‌کند و
            // برگشت‌ناپذیر است، دقیقاً هم‌سطح همان تأییدی که برای حذف فایل بکاپ داریم
            if (pendingRestoreFile != null) {
                val fileToRestore = pendingRestoreFile!!
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    AlertDialog(
                        containerColor = CardWhite,
                        onDismissRequest = { if (restoreStage == RestoreStage.IDLE) pendingRestoreFile = null },
                        title = {
                            Text(
                                "بازگردانی پشتیبان",
                                fontFamily = Vazirmatn,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        text = {
                            Text(
                                "همه‌ی اطلاعات فعلی (تراکنش‌ها، حساب‌ها، طلب و بدهی، لیست خرید) با محتوای فایل «${fileToRestore.name}» جایگزین می‌شود و این کار قابل بازگشت نیست.",
                                fontFamily = Vazirmatn,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    pendingRestoreFile = null
                                    runRestore(fileToRestore)
                                },
                                enabled = restoreStage == RestoreStage.IDLE
                            ) {
                                Text(
                                    "بازگردانی کن",
                                    color = ExpensePurple,
                                    fontFamily = Vazirmatn,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { pendingRestoreFile = null },
                                enabled = restoreStage == RestoreStage.IDLE
                            ) {
                                Text("انصراف", fontFamily = Vazirmatn, fontSize = 13.sp)
                            }
                        }
                    )
                }
            }

            if (resultMessage != null) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    AlertDialog(
                        containerColor = CardWhite,
                        onDismissRequest = { resultMessage = null },
                        title = {
                            Text(
                                if (resultIsError) "خطا" else "موفقیت",
                                fontFamily = Vazirmatn,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        text = {
                            Text(
                                resultMessage ?: "",
                                fontFamily = Vazirmatn,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { resultMessage = null }) {
                                Text(
                                    "باشه",
                                    color = if (resultIsError) ExpensePurple else PrimaryGreen,
                                    fontFamily = Vazirmatn,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsMenuRow(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(LightGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontFamily = Vazirmatn
            )
        }
        Icon(
            Icons.Outlined.ChevronLeft,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}
