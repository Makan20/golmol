package com.ordibehesht.finance.data.repository

object AppResetManager {
    // باگ رفع‌شده: قبلاً این تابع suspend نبود، و هر clearAll() زیرمجموعه هم خودش async
    // (scope.launch جدا) بود — یعنی caller (SettingsScreen) بلافاصله بعد از فراخوانی
    // resetAllData() ادامه می‌داد و وضعیت «موفق» را نشان می‌داد، در حالی که پاک‌سازی واقعی
    // دیتابیس ممکن بود هنوز در حال اجرا باشد. حالا این تابع و همه‌ی clearAll()های زیرمجموعه
    // suspend هستند، پس caller با await واقعی می‌تواند مطمئن شود پاک‌سازی کامل تمام شده.
    suspend fun resetAllData() {
        TransactionRepository.clearAll()
        AccountRepository.clearAll()
        ShoppingItemRepository.clearAll()
        DebtRepository.clearAll()
        DebtPaymentRepository.clearAll()
        DebtNotificationRepository.clearAll()
        com.ordibehesht.finance.dong.DongRepository.clearAll()
    }
}