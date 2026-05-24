package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ActivityEntity
import com.example.data.UserProfileEntity
import com.example.ui.theme.*

/**
 * Б) ПОЛНЫЙ КОД DashboardScreen.kt
 *
 * Главный экран фитнес-мотивации (Dashboard), стилизованный под премиальную темную дизайн-систему.
 * Отображает интерактивные XP-метрики, динамический прогресс-бар рангов, круговые индикаторы,
 * биопараметры пользователя и ленту импортированных физических активностей.
 * Оснащен анимированным Ambient Glow, подстраивающимся под текущую ауру цвета ранга пользователя.
 */
@Composable
fun FitQuestDashboardScreen(
    profile: UserProfileEntity?,
    activities: List<ActivityEntity>,
    viewModel: FitViewModel
) {
    val nonNullProfile = profile ?: UserProfileEntity()
    val activeRank = viewModel.getRankInfo(nonNullProfile.currentRankTier)
    
    // Поиск следующего ранга для подсчета XP-цели
    val nextRank = viewModel.rankTiersList.firstOrNull { it.tier == nonNullProfile.currentRankTier + 1 }
    val xpTarget = nextRank?.xpThreshold ?: (activeRank.xpThreshold + 5000)
    val xpCurrent = nonNullProfile.totalXp
    
    // Процент опыта до следующей ступени
    val prevThreshold = activeRank.xpThreshold
    val diffTotal = (xpTarget - prevThreshold).coerceAtLeast(1)
    val diffUser = (xpCurrent - prevThreshold).coerceAtLeast(0)
    val xpFrac = (diffUser.toFloat() / diffTotal.toFloat()).coerceIn(0f, 1f)

    // Плавная анимация шкалы прогресса XP
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(xpCurrent) {
        animatedProgress.animateTo(
            targetValue = xpFrac,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
        )
    }

    // Анимация пульсации аурового свечения (Ambient Glow) по цвету текущего ранга
    val rankColor = remember(activeRank.hexColor) {
        try {
            Color(android.graphics.Color.parseColor(activeRank.hexColor))
        } catch (e: Exception) {
            Color(0xFF00F5FF)
        }
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "AuraPulse")
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuraScale"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        // Шаг 1: Никнейм пользователя и статус серии дней (Streak) с эффектом сияния
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "ЭНЕРГЕТИЧЕСКАЯ АУРА АТЛЕТА",
                        color = rankColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace // Имитация Russo One / Oswald стиля заголовка
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = nonNullProfile.username,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 26.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
                
                // Бейдж Стрик Огня (Копирайтинг тренировок)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF221100))
                        .border(1.dp, Color(0xFFFF9900).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Серия тренировок",
                        tint = Color(0xFFFF9900),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${nonNullProfile.streakDays} ДН. СЕРИЯ",
                        color = Color(0xFFFF9900),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Шаг 2: Карточка игрового прогресса (Glassmorphic XP Card с Ambient Glow)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        // Динамическое рисование задней ауры текущего ранга
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(rankColor.copy(alpha = 0.15f * auraScale), Color.Transparent),
                                radius = size.minDimension * 0.9f
                            ),
                            center = center
                        )
                    }
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0x9E121216))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Круговой крутящийся XP-индикатор
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { animatedProgress.value },
                            modifier = Modifier.fillMaxSize(),
                            color = rankColor,
                            strokeWidth = 8.dp,
                            trackColor = Color.White.copy(alpha = 0.08f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$xpCurrent",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                            Text(
                                text = "XP",
                                color = SubtitleGrey,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeRank.title,
                            color = rankColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier.testTag("user_rank_label")
                        )
                        Text(
                            text = "УРОВЕНЬ ${activeRank.tier} • ИЕРАРХИЯ РАНГОВ",
                            color = SubtitleGrey,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (nextRank != null) {
                            Text(
                                text = "До ранга ${nextRank.title}:",
                                color = SubtitleGrey,
                                fontSize = 12.sp
                            )
                            LinearProgressIndicator(
                                progress = { animatedProgress.value },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .padding(vertical = 1.dp),
                                color = rankColor,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Осталось ${nextRank.xpThreshold - xpCurrent} XP",
                                color = SubtitleGrey.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        } else {
                            Text(
                                text = "👑 ВЫ ДОСТИГЛИ СВЕРХРАНГА! ИКОНА ТИТАНОВ",
                                color = Color(0xFFFF9900),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Шаг 3: Биометрические виджеты (Активные минуты, калории, рубли создателя)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Виджет активного времени трекера
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x9E121216))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = "Минуты",
                            tint = Color(0xFF00F5FF),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Активность", color = SubtitleGrey, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = "240 мин", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                }

                // Виджет калорийности тренировок
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x9E121216))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Калории",
                            tint = Color(0xFFFF2266),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Калории", color = SubtitleGrey, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = "3 180 ккал", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                }
            }
        }

        // Премиальные возможности RuStore в России
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF1E1E30), Color(0xFF0A0A1F))
                        )
                    )
                    .border(1.dp, rankColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "Премиум",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "FitQuest Premium",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (nonNullProfile.isPremium) "Премиум доступ активен! РФ" else "Доступ к закрытым челленджам звезд",
                                color = SubtitleGrey,
                                fontSize = 11.sp
                            )
                        }
                    }
                    if (!nonNullProfile.isPremium) {
                        Button(
                            onClick = {
                                viewModel.setPremium(true)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = rankColor),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "299 ₽",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        IconButton(onClick = { viewModel.setPremium(false) }) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Активен",
                                tint = Color(0xFF10B981)
                            )
                        }
                    }
                }
            }
        }

        // Заголовок ленты тренировок
        item {
            Text(
                text = "ЛЕНТА АКТИВНОСТИ",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        // Список выполненных пользователем тренировок
        if (activities.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x9E121216))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Нет тренировок",
                            tint = SubtitleGrey,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Ещё нет записанных занятий\nНажми '+' снизу чтобы начать!",
                            color = SubtitleGrey,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(activities.sortedByDescending { it.timestamp }) { activity ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x9E121216))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Кастомная иконка в зависимости от вида спорта
                    val (icon, tintColor) = when (activity.type.lowercase()) {
                        "run" -> Pair(Icons.Default.DirectionsRun, Color(0xFF00F5FF))
                        "gym" -> Pair(Icons.Default.FitnessCenter, Color(0xFFFF9900))
                        "bike" -> Pair(Icons.Default.DirectionsBike, Color(0xFFFF2266))
                        "swim" -> Pair(Icons.Default.Pool, Color(0xFF0EA5E9))
                        "yoga" -> Pair(Icons.Default.Spa, Color(0xFF10B981))
                        else -> Pair(Icons.Default.SportsScore, Color.White)
                    }

                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(tintColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = activity.type,
                            tint = tintColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (activity.type.lowercase()) {
                                "run" -> "Беговое кардио"
                                "gym" -> "Силовая тренировка"
                                "bike" -> "Велозаезд Апекс"
                                "swim" -> "Плавание на износ"
                                "yoga" -> "Дзен Стретчинг"
                                else -> activity.type
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "${activity.durationMinutes} мин • ${activity.distanceKm} км " +
                                    if (activity.isGpsTracked) "• [Real GPS]" else "• [Health Connect]",
                            color = SubtitleGrey,
                            fontSize = 12.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "+${activity.xpEarned} XP",
                            color = tintColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "УСПЕШНО",
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
