package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.BadgeEntity
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * C) ПОЛНЫЙ КОД LootBoxCelebrationDialog.kt
 *
 * Кинематографичный диалог открытия "Сундука достижений" (LootBox) при разблокировке спортивной медали/значка.
 * Содержит три фазы анимации:
 * 1. Интерактивный вибрирующий запертый сундук (LootBox)
 * 2. Взрыв световых лучей, сияние ауры (Canvas Particles)
 * 3. Появление увеличенной анимированной медали, счетчик XP и кнопка "Поделиться"
 */
@Composable
fun LootBoxCelebrationDialog(
    badgesToCelebrate: List<BadgeEntity>,
    onDismiss: () -> Unit
) {
    if (badgesToCelebrate.isEmpty()) return

    val currentBadge = badgesToCelebrate.first()
    var isChestOpened by remember { mutableStateOf(false) }
    
    // Анимационные контроллеры
    val scope = rememberCoroutineScope()
    val scaleAnim = remember { Animatable(0.9f) }
    val rotateAnim = remember { Animatable(0f) }
    
    // Бесконечное дрожание/вибрация сундука до открытия
    val vibrationTransition = rememberInfiniteTransition(label = "Vibration")
    val vibrateOffset by vibrationTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 80, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Vibrate"
    )

    // Анимация взрыва частиц (круги на холсте Canvas)
    val particlesCount = 20
    val particleRadiusList = remember { List(particlesCount) { Random.nextFloat() * 180f + 60f } }
    val particleAnglesList = remember { List(particlesCount) { Random.nextDouble() * 2 * Math.PI } }
    
    val particleExpansion = remember { Animatable(0f) }
    val badgeRevealScale = remember { Animatable(0.1f) }
    val xpCounterAnim = remember { Animatable(0f) }

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable { /* Блокировка кликов на задний фон */ },
            contentAlignment = Alignment.Center
        ) {
            
            // Кнопка принудительного закрытия в верхнем углу
            IconButton(
                onClick = { onDismiss() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Закрыть",
                    tint = Color.White
                )
            }

            // Фаза 1: Неоткрытый таинственный LootBox
            if (!isChestOpened) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "НАЙДЕН ТРОФЕЙ АТЛЕТА!",
                        color = Color(0xFFFF9900),
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ты разблокировал новое достижение. Коснись сундука, чтобы забрать награду!",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(48.dp))

                    // Анимированный вибрирующий сундук
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = vibrateOffset
                                rotationZ = vibrateOffset * 0.4f
                            }
                            .scale(scaleAnim.value)
                            .size(160.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFFFF9900), Color(0xFF884400))
                                )
                            )
                            .border(3.dp, Color(0xFFFFD700), RoundedCornerShape(32.dp))
                            .clickable {
                                // Триггер открытия сундука
                                isChestOpened = true
                                scope.launch {
                                    // 1. Взрыв частиц
                                    particleExpansion.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(1200, easing = EaseOutExpo)
                                    )
                                }
                                scope.launch {
                                    // 2. Взрывной масштаб медали
                                    badgeRevealScale.animateTo(
                                        targetValue = 1.1f,
                                        animationSpec = tween(600, easing = EaseOutBack)
                                    )
                                    badgeRevealScale.animateTo(
                                        targetValue = 1.0f,
                                        animationSpec = tween(200)
                                    )
                                }
                                scope.launch {
                                    // 3. Выкатывание цифр полученного XP
                                    delay(400)
                                    xpCounterAnim.animateTo(
                                        targetValue = currentBadge.xpReward.toFloat(),
                                        animationSpec = tween(1000, easing = FastOutSlowInEasing)
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CardGiftcard,
                                contentDescription = "Сундук",
                                tint = Color.White,
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "ОТКРЫТЬ",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            } else {
                // Фаза 2 и 3: Взрыв частиц и преподнесение разблокированной медали
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Рисование кругового взрыва лучей/частиц на холсте Canvas
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2
                        val cy = size.height / 2
                        
                        // Рисование расширяющегося кольца ударной волны
                        drawCircle(
                            color = Color(0xFFFF9900).copy(alpha = (1f - particleExpansion.value).coerceIn(0f, 1f)),
                            radius = particleExpansion.value * 400f,
                            center = androidx.compose.ui.geometry.Offset(cx, cy),
                            style = Stroke(width = 8.dp.toPx())
                        )

                        // Разлет отдельных искр/частиц в стороны
                        for (i in 0 until particlesCount) {
                            val angle = particleAnglesList[i]
                            val maxRadius = particleRadiusList[i]
                            val currentR = particleExpansion.value * maxRadius
                            
                            val px = cx + (currentR * Math.cos(angle)).toFloat()
                            val py = cy + (currentR * Math.sin(angle)).toFloat()

                            drawCircle(
                                color = Color(0xFFFFD700).copy(alpha = (1f - particleExpansion.value).coerceIn(0f, 1f)),
                                radius = (8f * (1f - particleExpansion.value)).coerceAtLeast(1f),
                                center = androidx.compose.ui.geometry.Offset(px, py)
                            )
                        }
                    }

                    // Медаль с плавным приближением (Zoom) и сиянием
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .scale(badgeRevealScale.value),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val badgeColor = try {
                            Color(android.graphics.Color.parseColor(currentBadge.visualColor))
                        } catch (e: Exception) {
                            Color(0xFF00F5FF)
                        }

                        // Свечение сзади медали
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .drawBehind {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(badgeColor.copy(alpha = 0.40f), Color.Transparent),
                                            radius = this@drawBehind.size.width * 0.9f
                                        ),
                                        center = this@drawBehind.center
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Иконка достижения в красивом обрамлении
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF13131F))
                                    .border(3.dp, badgeColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Звезда",
                                    tint = badgeColor,
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Поздравление и название медали
                        Text(
                            text = "ПОЗДРАВЛЯЕМ!",
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "«${currentBadge.name}»",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentBadge.description,
                            color = SubtitleGrey,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Динамически растущий счетчик полученного XP за медаль
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(badgeColor.copy(alpha = 0.12f))
                                .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "XP",
                                    tint = badgeColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "+${xpCounterAnim.value.toInt()} XP",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 24.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(48.dp))

                        // Кнопки шеринга и закрытия
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Кнопка закрытия
                            OutlinedButton(
                                onClick = { onDismiss() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Отлично",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            // Кнопка Поделиться триумфом
                            Button(
                                onClick = {
                                    // Симуляция системного шеринга рекорда
                                    println("Пользователь поделился открытием достижения: ${currentBadge.name}")
                                },
                                modifier = Modifier.weight(1.2f),
                                colors = ButtonDefaults.buttonColors(containerColor = badgeColor),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Поделиться",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
