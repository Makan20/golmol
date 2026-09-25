package com.ordibehesht.finance.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ordibehesht.finance.ui.theme.BackgroundLight
import com.ordibehesht.finance.ui.theme.ExpensePurple
import com.ordibehesht.finance.ui.theme.PrimaryGreen
import com.ordibehesht.finance.ui.theme.TextPrimary
import com.ordibehesht.finance.ui.theme.TextSecondary
import com.ordibehesht.finance.ui.theme.TextTertiary
import com.ordibehesht.finance.ui.theme.Vazirmatn
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val iconTint: Color,
    val iconBackground: Color,
    val title: String,
    val description: String
)

private val onboardingPages = listOf(
    OnboardingPage(
        icon = Icons.Outlined.Eco,
        iconTint = PrimaryGreen,
        iconBackground = PrimaryGreen.copy(alpha = 0.10f),
        title = "به اردیبهشت خوش اومدی",
        description = "دستیار ساده و شخصی‌ات برای مدیریت هزینه‌ها، بدهی‌ها و برنامه‌های مالی روزمره"
    ),
    OnboardingPage(
        icon = Icons.Outlined.BarChart,
        iconTint = ExpensePurple,
        iconBackground = ExpensePurple.copy(alpha = 0.10f),
        title = "تراکنش‌ها و گزارش‌ها",
        description = "درآمد و هزینه‌هاتو ثبت کن و روند خرج‌کردنت رو با نمودارهای ساده و شفاف ببین"
    ),
    OnboardingPage(
        icon = Icons.Outlined.ShoppingBag,
        iconTint = PrimaryGreen,
        iconBackground = PrimaryGreen.copy(alpha = 0.10f),
        title = "لیست خرید هوشمند",
        description = "چیزهایی که باید بخری رو اضافه کن و بعد از خرید، مستقیم به تراکنش‌هات تبدیلش کن"
    ),
    OnboardingPage(
        icon = Icons.Outlined.Groups,
        iconTint = ExpensePurple,
        iconBackground = ExpensePurple.copy(alpha = 0.10f),
        title = "دونگ: تقسیم هزینه‌ی گروهی",
        description = "هزینه‌ی سفر یا خرید گروهی رو بین دوستات تقسیم کن و ببین کی به کی بدهکاره"
    ),
    OnboardingPage(
        icon = Icons.Outlined.AccountBalanceWallet,
        iconTint = PrimaryGreen,
        iconBackground = PrimaryGreen.copy(alpha = 0.10f),
        title = "طلب و بدهی رو گم نکن",
        description = "هرکی بهت بدهکاره یا بهش بدهکاری رو با یادآوری سررسید، همیشه زیر نظر داشته باش"
    )
)

/**
 * صفحه‌ی راهنمای اولیه‌ی اپ — فقط یک‌بار (اولین اجرای اپ روی دستگاه) نمایش داده می‌شود
 * (ر.ک. OnboardingPreferences و MainActivity). طراحی و انیمیشن‌ها عمداً از همان زبان
 * بصری خودِ اپ گرفته شده‌اند: گل‌های تزئینی FlowerDecoration، دکمه‌ی بنفش گرد با آیکون
 * گلیِ FabFlowerIcon (هر دو از HomeScreen)، فونت Vazirmatn، و پالت رنگ اصلی پروژه —
 * تا این صفحه بخشی طبیعی از خودِ اپ به‌نظر برسد، نه یک کامپوننت جدا و بیگانه.
 */
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pageCount = onboardingPages.size
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pageCount - 1

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
        ) {
            // گل‌های تزئینی شناور پس‌زمینه — دقیقاً هم‌الگو با نسخه‌ی HTML preview که پسندیدید:
            // هر گل به‌طور بی‌نهایت (infinite loop) کمی بالا/پایین می‌رود و می‌چرخد، با
            // مدت‌زمان و دامنه‌ی کمی متفاوت برای هر کدام تا هم‌زمان و یکنواخت به‌نظر نرسند
            // (معادل Compose برای @keyframes floatSlow در CSS اصلی).
            FloatingFlowerDecoration(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 64.dp, start = 8.dp)
                    .size(64.dp),
                color = ExpensePurple.copy(alpha = 0.10f),
                floatRangeDp = 16f,
                rotationDegrees = 12f,
                durationMillis = 1800
            )
            FloatingFlowerDecoration(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 110.dp, end = 20.dp)
                    .size(36.dp),
                color = PrimaryGreen.copy(alpha = 0.12f),
                floatRangeDp = 14f,
                rotationDegrees = -14f,
                durationMillis = 2100,
                reverseDirection = true
            )
            FloatingFlowerDecoration(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 190.dp, start = 24.dp)
                    .size(44.dp),
                color = ExpensePurple.copy(alpha = 0.09f),
                floatRangeDp = 15f,
                rotationDegrees = 13f,
                durationMillis = 2400
            )

            // دکمه‌ی «رد کردن»: در صفحه‌ی آخر نیازی نیست (چون دکمه‌ی اصلی خودش «بزن بریم»
            // می‌شود)، پس با یک fade ساده محو می‌شود تا ناگهان ناپدید نشود
            val skipAlpha by animateFloatAsState(
                targetValue = if (isLastPage) 0f else 1f,
                animationSpec = tween(220),
                label = "skipAlpha"
            )
            Text(
                text = "رد کردن",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary,
                fontFamily = Vazirmatn,
                modifier = Modifier
                    // این Box زیرمجموعه‌ی CompositionLocalProvider(Rtl) است، پس در این
                    // بستر Start معادل راست بصری و End معادل چپ بصری می‌شود — TopEnd یعنی
                    // واقعاً بالا-چپ صفحه (همان‌جایی که طبق درخواست باید باشد)
                    .align(Alignment.TopEnd)
                    .padding(top = 20.dp, end = 20.dp)
                    .alpha(skipAlpha)
                    .clickable(enabled = !isLastPage) { onFinished() }
                    .padding(8.dp)
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(72.dp))

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { page ->
                    OnboardingPageContent(
                        page = onboardingPages[page],
                        // فقط وقتی این صفحه واقعاً صفحه‌ی فعلی pager است انیمیشن ورودش
                        // پخش می‌شود — وگرنه صفحات مجاور هم هر بار recomposition دوباره
                        // انیمیشن می‌گرفتند
                        isCurrent = pagerState.settledPage == page
                    )
                }

                OnboardingDots(pagerState = pagerState, pageCount = pageCount)

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .padding(bottom = 28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // طبق درخواست: «قبلی» باید سمت چپ صفحه باشد. چون این Row در بستر Rtl
                    // است، اولین آیتم آن به‌طور طبیعی سمت راست می‌افتد — پس «قبلی» را به
                    // انتهای Row منتقل می‌کنیم (که در Rtl سمت چپ می‌شود) و یک Spacer
                    // هم‌عرض آن را در ابتدای Row می‌گذاریم تا دکمه‌ی اصلی همچنان وسط بماند
                    Spacer(Modifier.width(60.dp))

                    Spacer(Modifier.weight(1f))

                    OnboardingNextButton(
                        isLastPage = isLastPage,
                        onClick = {
                            if (isLastPage) {
                                onFinished()
                            } else {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        }
                    )

                    Spacer(Modifier.weight(1f))

                    if (pagerState.currentPage > 0) {
                        Text(
                            text = "قبلی",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiary,
                            fontFamily = Vazirmatn,
                            modifier = Modifier
                                .widthIn(min = 60.dp)
                                .clickable {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                }
                                .padding(12.dp)
                        )
                    } else {
                        Spacer(Modifier.width(60.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage, isCurrent: Boolean) {
    // انیمیشن ورود آیکون: pop-in با کمی چرخش و overshoot، دقیقاً حس شیرین و بازیگوش
    // مشابه چرخش دکمه‌ی گلی FAB در بقیه‌ی اپ. با تغییر isCurrent (یعنی این صفحه به
    // صفحه‌ی فعال pager تبدیل می‌شود) هر بار از نو پخش می‌شود.
    val iconScale = remember(page) { androidx.compose.animation.core.Animatable(0.4f) }
    val iconRotation = remember(page) { androidx.compose.animation.core.Animatable(-25f) }
    val contentAlpha = remember(page) { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(isCurrent) {
        if (isCurrent) {
            iconScale.snapTo(0.4f)
            iconRotation.snapTo(-25f)
            contentAlpha.snapTo(0f)
            launch {
                iconScale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing)
                )
            }
            launch {
                iconRotation.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing)
                )
            }
            launch {
                contentAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 420, easing = LinearOutSlowInEasing)
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(168.dp)
                .scale(iconScale.value)
                .rotate(iconRotation.value)
                .background(page.iconBackground, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = page.iconTint,
                modifier = Modifier.size(72.dp)
            )
        }

        Spacer(Modifier.height(36.dp))

        Column(
            modifier = Modifier.alpha(contentAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = page.title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontFamily = Vazirmatn,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontFamily = Vazirmatn,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 300.dp)
            )
        }
    }
}

/**
 * دقیقاً معادل @keyframes floatSlow در نسخه‌ی HTML preview: گل با rememberInfiniteTransition
 * به‌طور بی‌نهایت بین حالت اولیه و translateY(-floatRangeDp) + rotate(rotationDegrees) نوسان
 * می‌کند، با repeatMode=Reverse (رفت‌وبرگشت نرم، نه پرش ناگهانی به نقطه‌ی شروع) — دقیقاً هم‌ارز
 * با ترکیب 0%→50%→100% در CSS keyframes اصلی.
 */
@Composable
private fun FloatingFlowerDecoration(
    modifier: Modifier = Modifier,
    color: Color,
    floatRangeDp: Float,
    rotationDegrees: Float,
    durationMillis: Int,
    reverseDirection: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "flowerFloat")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (reverseDirection) floatRangeDp else -floatRangeDp,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flowerFloatOffset"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = rotationDegrees,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flowerRotation"
    )

    FlowerDecoration(
        modifier = modifier
            .offset(y = floatOffset.dp)
            .rotate(rotation),
        color = color
    )
}

@Composable
private fun OnboardingDots(pagerState: PagerState, pageCount: Int) {
    // طبق درخواست: نقطه‌ی اول باید سمت چپ باشد و با «بعدی» به سمت راست حرکت کند — یعنی
    // دقیقاً برعکسِ چیدمان طبیعی یک Row در بستر Rtl (که اولین آیتم را راست می‌گذارد).
    // برای همین این Row عمداً به Ltr سوییچ می‌شود، مستقل از باقی صفحه.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pageCount) { index ->
                val isActive = pagerState.currentPage == index
                val dotWidth by animateFloatAsState(
                    targetValue = if (isActive) 24f else 8f,
                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                    label = "dotWidth"
                )
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(dotWidth.dp)
                        .background(
                            color = if (isActive) ExpensePurple else ExpensePurple.copy(alpha = 0.22f),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun OnboardingNextButton(isLastPage: Boolean, onClick: () -> Unit) {
    // چرخش کوچک و شیرین دکمه هر بار که بین «بعدی» و «بزن بریم» تغییر حالت می‌دهد —
    // یادآور چرخش ۳۶۰ درجه‌ی دکمه‌ی گلی FAB در بقیه‌ی صفحات اپ (ولی ملایم‌تر، چون این‌جا
    // فقط یک تغییر حالت است، نه واکنش به اسکرول پیوسته)
    val iconRotation by animateFloatAsState(
        targetValue = if (isLastPage) 90f else 0f,
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "nextIconRotation"
    )

    Box(
        modifier = Modifier
            .height(54.dp)
            .widthIn(min = 168.dp)
            // clip قبل از clickable می‌آید تا ripple/press-indication هم به همین شکل
            // گردِ دکمه محدود شود — وگرنه clickable روی مستطیل کامل قبل از کلیپ‌شدن اثر
            // می‌گذارد و هنگام لمس یک سایه‌ی مستطیلی بیرون از گوشه‌های گرد دیده می‌شود
            .clip(RoundedCornerShape(27.dp))
            .background(ExpensePurple)
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!isLastPage) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBackIos,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(16.dp)
                        // ArrowBackIos خودش auto-mirrored است — یعنی در بستر RTL از قبل
                        // به سمت راست اشاره می‌کند (جهت درستِ «بعدی»). rotate(180f) قبلی
                        // دوباره برش می‌گرداند به چپ، که دقیقاً برعکسِ خواسته بود؛ اینجا
                        // فقط چرخش لرزشی/فیدبک لمس (iconRotation) اعمال می‌شود
                        .rotate(iconRotation)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = if (isLastPage) "بزن بریم!" else "بعدی",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = Vazirmatn,
                textAlign = TextAlign.Center
            )
        }
    }
}
