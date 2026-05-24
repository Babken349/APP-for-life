package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.data.ActivityEntity
import com.example.data.BadgeEntity
import com.example.data.ChallengeEntity
import com.example.ui.FitViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                FitQuestApp()
            }
        }
    }
}

@Composable
fun FitQuestApp() {
    val viewModel: FitViewModel = viewModel()
    val activities by viewModel.activities.collectAsStateWithLifecycle()
    val badges by viewModel.badges.collectAsStateWithLifecycle()
    val challenges by viewModel.challenges.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()

    // Screen-level state: "onboarding_1", "onboarding_2", "onboarding_3", "onboarding_4", "main"
    var currentScreenState by remember { mutableStateOf("onboarding_1") }
    // Inside main: "dashboard", "badges", "challenges", "leaderboard", "creator"
    var activeTab by remember { mutableStateOf("dashboard") }

    // Dialog state for manual activity logging
    var showLogDialog by remember { mutableStateOf(false) }

    // If database loads and we see onboarding progress, check if we skip onboarding
    LaunchedEffect(profile) {
        if (profile != null && profile!!.healthKitConnected && currentScreenState.startsWith("onboarding")) {
            currentScreenState = "main"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // Aesthetic ambient aura background glow (Glassmorphism ambient source)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x1F00F5FF), Color.Transparent),
                            radius = 1200f
                        ),
                        center = androidx.compose.ui.geometry.Offset(100f, 200f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x1F10B981), Color.Transparent),
                            radius = 1000f
                        ),
                        center = androidx.compose.ui.geometry.Offset(this.size.width, this.size.height * 0.7f)
                    )
                }
        )

        // Screen routing
        Crossfade(targetState = currentScreenState, label = "ScreenTransition") { screen ->
            when (screen) {
                "onboarding_1" -> GoalSelectionScreen(
                    onNext = { goal ->
                        viewModel.updateFitnessGoal(goal)
                        currentScreenState = "onboarding_2"
                    }
                )
                "onboarding_2" -> HealthKitConnectionScreen(
                    onNext = { connected ->
                        viewModel.toggleHealthKit(connected)
                        currentScreenState = "onboarding_3"
                    }
                )
                "onboarding_3" -> QuickWinScreen(
                    onNext = {
                        // Log a first activity immediately to earn first badge
                        viewModel.logWorkout(
                            type = "Gym",
                            duration = 45,
                            distance = 0.0,
                            photoUri = "first_activity_proof.jpg",
                            isGps = false
                        )
                        currentScreenState = "onboarding_4"
                    }
                )
                "onboarding_4" -> SuggestedChallengesScreen(
                    challenges = challenges.take(3),
                    onJoinChallenge = { id -> viewModel.toggleJoinChallenge(id) },
                    onStart = {
                        currentScreenState = "main"
                    }
                )
                "main" -> {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent,
                        bottomBar = {
                            FitBottomNavigationBar(
                                activeTab = activeTab,
                                onTabSelected = { activeTab = it }
                            )
                        },
                        floatingActionButton = {
                            if (activeTab == "dashboard" || activeTab == "challenges") {
                                FloatingActionButton(
                                    onClick = { showLogDialog = true },
                                    containerColor = KineticFlame,
                                    contentColor = Color.White,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .testTag("fab_log_activity")
                                        .size(56.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Log Workout",
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (activeTab) {
                                "dashboard" -> DashboardTabContent(
                                    profile = profile,
                                    activities = activities,
                                    viewModel = viewModel
                                )
                                "badges" -> BadgeCatalogueTabContent(
                                    badges = badges,
                                    profile = profile
                                )
                                "challenges" -> ChallengesTabContent(
                                    challenges = challenges,
                                    onJoinChallenge = { viewModel.toggleJoinChallenge(it) }
                                )
                                "leaderboard" -> LeaderboardTabContent()
                                "creator" -> CreatorHubTabContent(
                                    profile = profile,
                                    challenges = challenges,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Cinematic "Loot-Box" Badge Unlock Overlay ---
        val newlyBadges = viewModel.newlyUnlockedBadges.value
        if (newlyBadges.isNotEmpty()) {
            LootBoxCelebrationDialog(
                unlockedBadges = newlyBadges,
                onDismiss = { viewModel.clearBadgeCelebration() }
            )
        }

        // --- Cinematic Rank-Up Level up celebration Screen ---
        val rankUpEvent = viewModel.showRankUpCelebration.value
        if (rankUpEvent != null && rankUpEvent.first) {
            val rankInfo = viewModel.getRankInfo(rankUpEvent.second)
            RankCelebrationScreen(
                rankInfo = rankInfo,
                onDismiss = { viewModel.clearRankUpCelebration() }
            )
        }

        // --- Manual activity logging Dialog sheet ---
        if (showLogDialog) {
            ActivityLogSheet(
                onDismiss = { showLogDialog = false },
                onSave = { type, duration, distance, isGps ->
                    viewModel.logWorkout(type, duration, distance, null, isGps)
                    showLogDialog = false
                }
            )
        }
    }
}

// ==========================================
// RESUABLE COMPONENTS: Glassmorphic Cards
// ==========================================
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderStroke: BorderStroke? = BorderStroke(1.dp, GlassBorder),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = GlassSurface),
        border = borderStroke,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

// ==========================================
// 1. ONBOARDING: Goal Selection
// ==========================================
@Composable
fun GoalSelectionScreen(onNext: (String) -> Unit) {
    val goals = listOf(
        Pair("Сила и рельеф", "Набор мышечной массы и силовые тренировки"),
        Pair("Снижение веса", "Сжигание жира и интенсивное кардио"),
        Pair("Выносливость", "Развитие выносливости в беге и велозаездах"),
        Pair("Активный тонус", "Ежедневная активность, растяжка и тонус")
    )
    var selectedGoal by remember { mutableStateOf("Сила и рельеф") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "FITQUEST",
                color = KineticFlame,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Выберите фитнес-цель",
                color = PremiumWhite,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Настройте свои испытания и получайте индивидуальные награды.",
                color = SubtitleGrey,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))

            goals.forEach { goal ->
                val isSelected = goal.first == selectedGoal
                Card(
                     modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { selectedGoal = goal.first },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0x3FF66F00) else GlassSurface
                    ),
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = if (isSelected) KineticFlame else GlassBorder
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (goal.first) {
                                "Сила и рельеф" -> Icons.Default.FitnessCenter
                                "Снижение веса" -> Icons.Default.LocalFireDepartment
                                "Выносливость" -> Icons.Default.DirectionsRun
                                else -> Icons.Default.Favorite
                            },
                            contentDescription = goal.first,
                            tint = if (isSelected) KineticFlame else SubtitleGrey,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = goal.first,
                                color = PremiumWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = goal.second,
                                color = SubtitleGrey,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = { onNext(selectedGoal) },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(54.dp)
                .testTag("onboarding_btn_1"),
            colors = ButtonDefaults.buttonColors(containerColor = KineticFlame),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Продолжить", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Далее")
        }
    }
}

// ==========================================
// 2. ONBOARDING: Health Connect
// ==========================================
@Composable
fun HealthKitConnectionScreen(onNext: (Boolean) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(30.dp))
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = "Sync",
                tint = NeonCyan,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Синхронизация биометрии",
                color = PremiumWhite,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Синхронизируйте FitQuest с Google Fit или Health Connect для автоматического импорта статистики бега, тренировок и шагов.",
                color = SubtitleGrey,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(40.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Security",
                        tint = CyberNeon,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Защищено и зашифровано",
                            color = PremiumWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Мы никогда не передаем ваши спортивные показатели или данные о здоровье третьим лицам.",
                            color = SubtitleGrey,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            Button(
                onClick = { onNext(true) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("onboarding_btn_2"),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "Connect", tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Подключить биометрию", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                onClick = { onNext(false) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Я буду вносить тренировки вручную", color = SubtitleGrey, fontSize = 14.sp)
            }
        }
    }
}

// ==========================================
// 3. ONBOARDING: Quick Win Activity
// ==========================================
@Composable
fun QuickWinScreen(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(30.dp))
            Icon(
                imageVector = Icons.Default.Celebration,
                contentDescription = "Quick Win",
                tint = CyberNeon,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Твоя первая победа!",
                color = PremiumWhite,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Каждому старту нужен мощный импульс. FitQuest дарит вам приветственный набор опыта в 150 XP! Это значительно ускорит ваш прогресс и повысит ранг.",
                color = SubtitleGrey,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "ПРИВЕТСТВЕННАЯ НАГРАДА",
                    color = CyberNeon,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = "Стартовый кинетический набор",
                    color = PremiumWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Bolt, contentDescription = "XP", tint = KineticFlame)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "+150 XP к прогрессу", color = KineticFlame, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.WorkspacePremium, contentDescription = "Badge", tint = NeonCyan)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Открывает трекер значка 'Железный катализатор'", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(54.dp)
                .testTag("onboarding_btn_3"),
            colors = ButtonDefaults.buttonColors(containerColor = CyberNeon),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Забрать награду", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Next", tint = Color.Black)
        }
    }
}

// ==========================================
// 4. ONBOARDING: Suggested Challenges
// ==========================================
@Composable
fun SuggestedChallengesScreen(
    challenges: List<ChallengeEntity>,
    onJoinChallenge: (String) -> Unit,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Вызовы 1-го дня",
                color = PremiumWhite,
                fontWeight = FontWeight.Black,
                fontSize = 26.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Присоединитесь к испытаниям блогеров, чтобы начать соревноваться в лиге.",
                color = SubtitleGrey,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 20.dp, top = 4.dp)
            )

            challenges.forEach { challenge ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = GlassSurface),
                    border = BorderStroke(1.dp, if (challenge.isJoined) HyperNeonGradientBrush() else SolidColor(GlassBorder))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(DarkSlate)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Аватар",
                                    tint = SubtitleGrey,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.Center)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = challenge.title,
                                    color = PremiumWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = challenge.creatorName,
                                        color = KineticFlame,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (challenge.isVerifiedCreator) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = "Проверен",
                                            tint = NeonCyan,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { onJoinChallenge(challenge.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (challenge.isJoined) CyberNeon else Color(0x33FFFFFF)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = if (challenge.isJoined) "Участвую" else "Вход: Бесплатно",
                                color = if (challenge.isJoined) Color.Black else PremiumWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(54.dp)
                .testTag("onboarding_btn_4"),
            colors = ButtonDefaults.buttonColors(containerColor = KineticFlame),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Войти в мир фитнеса", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Старт", tint = Color.White)
        }
    }
}

// ==========================================
// BOTTOM NAVIGATION BAR
// ==========================================
@Composable
fun FitBottomNavigationBar(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xEB0A0A0E),
        modifier = Modifier
            .navigationBarsPadding()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .border(0.5.dp, GlassBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
        tonalElevation = 10.dp
    ) {
        val navItems = listOf(
            Triple("dashboard", "Главная", Icons.Default.Home),
            Triple("badges", "Значки", Icons.Default.WorkspacePremium),
            Triple("challenges", "Испытания", Icons.Default.EmojiEvents),
            Triple("leaderboard", "Рейтинг", Icons.Default.Leaderboard),
            Triple("creator", "Авторам", Icons.Default.BarChart)
        )

        navItems.forEach { item ->
            val isSelected = activeTab == item.first
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(item.first) },
                icon = {
                    Icon(
                        imageVector = item.third,
                        contentDescription = item.second,
                        tint = if (isSelected) KineticFlame else SubtitleGrey
                    )
                },
                label = {
                    Text(
                        text = item.second,
                        color = if (isSelected) PremiumWhite else SubtitleGrey,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0x33FF5500)
                )
            )
        }
    }
}

// ==========================================
// TAB A: HOME DASHBOARD
// ==========================================
@Composable
fun DashboardTabContent(
    profile: com.example.data.UserProfileEntity?,
    activities: List<ActivityEntity>,
    viewModel: FitViewModel
) {
    com.example.ui.FitQuestDashboardScreen(
        profile = profile,
        activities = activities,
        viewModel = viewModel
    )
}

// ==========================================
// TAB B: ACHIEVEMENT BADGE CATALOGUE
// ==========================================
@Composable
fun BadgeCatalogueTabContent(
    badges: List<BadgeEntity>,
    profile: com.example.data.UserProfileEntity?
) {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Distance", "Streak", "Speed", "Social", "Secret")
    val filteredBadges = if (selectedCategory == "All") badges else badges.filter { it.category == selectedCategory }

    val unlockedCount = badges.count { it.isUnlocked }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Хранилище достижений",
            color = PremiumWhite,
            fontWeight = FontWeight.Black,
            fontSize = 24.sp
        )
        Text(
            text = "Разблокировано: $unlockedCount из ${badges.size} элитных значков",
            color = CyberNeon,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        ScrollableTabRow(
            selectedTabIndex = categories.indexOf(selectedCategory),
            containerColor = Color.Transparent,
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[categories.indexOf(selectedCategory)]),
                    color = KineticFlame
                )
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            categories.forEach { cat ->
                val displayName = when (cat) {
                    "All" -> "Все"
                    "Distance" -> "Дистанция"
                    "Streak" -> "Серии"
                    "Speed" -> "Скорость"
                    "Social" -> "Общение"
                    "Secret" -> "Секреты"
                    else -> cat
                }
                Tab(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    text = { Text(displayName, fontSize = 14.sp) },
                    selectedContentColor = PremiumWhite,
                    unselectedContentColor = SubtitleGrey
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("badge_grid_view")
        ) {
            items(filteredBadges) { badge ->
                var showBadgeDetailDialog by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .aspectRatio(0.85f)
                        .clickable { showBadgeDetailDialog = true },
                    colors = CardDefaults.cardColors(
                        containerColor = if (badge.isUnlocked) GlassSurface else Color(0x1A4F4F5A)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (badge.isUnlocked) Color(android.graphics.Color.parseColor(badge.visualColor)).copy(alpha = 0.5f) else GlassBorder
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (badge.isUnlocked) Color(android.graphics.Color.parseColor(badge.visualColor)).copy(alpha = 0.2f)
                                    else Color(0x11FFFFFF)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (badge.iconName) {
                                    "directions_run" -> Icons.Default.DirectionsRun
                                    "local_fire_department" -> Icons.Default.LocalFireDepartment
                                    "explore" -> Icons.Default.Explore
                                    "emoji_events" -> Icons.Default.EmojiEvents
                                    "public" -> Icons.Default.Public
                                    "bolt" -> Icons.Default.Bolt
                                    "star" -> Icons.Default.Star
                                    "offline_bolt" -> Icons.Default.OfflineBolt
                                    "wb_sunny" -> Icons.Default.WbSunny
                                    "nights_stay" -> Icons.Default.NightsStay
                                    "speed" -> Icons.Default.Speed
                                    "fitness_center" -> Icons.Default.FitnessCenter
                                    "pool" -> Icons.Default.Pool
                                    "directions_bike" -> Icons.Default.DirectionsBike
                                    "spa" -> Icons.Default.Spa
                                    "group_add" -> Icons.Default.GroupAdd
                                    "favorite" -> Icons.Default.Favorite
                                    "healing" -> Icons.Default.Healing
                                    else -> Icons.Default.MilitaryTech
                                },
                                contentDescription = badge.name,
                                tint = if (badge.isUnlocked) Color(android.graphics.Color.parseColor(badge.visualColor)) else SubtitleGrey,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = badge.name,
                            color = if (badge.isUnlocked) PremiumWhite else SubtitleGrey,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val badgeCategoryDisplayName = when (badge.category) {
                            "Distance" -> "Дистанция"
                            "Streak" -> "Серии"
                            "Speed" -> "Скорость"
                            "Social" -> "Общение"
                            "Secret" -> "Секрет"
                            else -> badge.category
                        }
                        Text(
                            text = badgeCategoryDisplayName,
                            color = SubtitleGrey,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (showBadgeDetailDialog) {
                    Dialog(onDismissRequest = { showBadgeDetailDialog = false }) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSlate),
                            border = BorderStroke(1.5.dp, KineticFlame),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    tint = if (badge.isUnlocked) Color(android.graphics.Color.parseColor(badge.visualColor)) else SubtitleGrey,
                                    modifier = Modifier.size(72.dp),
                                    contentDescription = badge.name
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = badge.name,
                                    color = PremiumWhite,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 22.sp,
                                    textAlign = TextAlign.Center
                                )
                                val dialogBadgeCategoryName = when (badge.category) {
                                    "Distance" -> "Дистанция"
                                    "Streak" -> "Серии"
                                    "Speed" -> "Скорость"
                                    "Social" -> "Общение"
                                    "Secret" -> "Секрет"
                                    else -> badge.category
                                }
                                Text(
                                    text = dialogBadgeCategoryName.uppercase(),
                                    color = KineticFlame,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = badge.description,
                                    color = PremiumWhite,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "УСЛОВИЕ ПОЛУЧЕНИЯ:",
                                        color = SubtitleGrey,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = badge.triggerCondition,
                                        color = NeonCyan,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { showBadgeDetailDialog = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = KineticFlame),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Закрыть", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB C: CHALLENGES
// ==========================================
@Composable
fun ChallengesTabContent(
    challenges: List<ChallengeEntity>,
    onJoinChallenge: (String) -> Unit
) {
    var filterByInfluencer by remember { mutableStateOf(false) }

    val filteredList = if (filterByInfluencer) challenges.filter { it.isInfluencer } else challenges

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Фитнес-Арена Блогеров",
                    color = PremiumWhite,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp
                )
                Text(
                    text = "Присоединяйтесь к испытаниям фитнес-блогеров и забирайте уникальные награды.",
                    color = SubtitleGrey,
                    fontSize = 13.sp
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (filterByInfluencer) "Испытания блогеров" else "Все испытания",
                    color = PremiumWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Только блогеры",
                        color = SubtitleGrey,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Switch(
                        checked = filterByInfluencer,
                        onCheckedChange = { filterByInfluencer = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = KineticFlame,
                            checkedTrackColor = Color(0x33FF5500)
                        )
                    )
                }
            }
        }

        items(filteredList) { chall ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GlassSurface),
                border = BorderStroke(1.dp, if (chall.isJoined) HyperNeonGradientBrush() else SolidColor(GlassBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(DarkSlate),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Аватар",
                                    tint = SubtitleGrey,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = chall.creatorName,
                                        color = KineticFlame,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    if (chall.isVerifiedCreator) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = "Проверенный блогер",
                                            tint = NeonCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${chall.participantCount} участников",
                                    color = SubtitleGrey,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Premium cost label
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (chall.entryFee > 0.0) Color(0x3300FF66) else Color(0x22FFFFFF))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (chall.entryFee > 0.0) "$${chall.entryFee} ВХОД" else "БЕСПЛАТНО",
                                color = if (chall.entryFee > 0.0) CyberNeon else PremiumWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = chall.title,
                        color = PremiumWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = chall.description,
                        color = SubtitleGrey,
                        fontSize = 13.sp,
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Bolt, contentDescription = "XP", tint = KineticFlame, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "${chall.xpReward} XP", color = KineticFlame, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Icon(imageVector = Icons.Default.Timelapse, contentDescription = "Days", tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "${chall.durationDays} дн.", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = { onJoinChallenge(chall.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (chall.isJoined) CyberNeon else KineticFlame
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (chall.isJoined) "Выйти" else "Участвовать",
                                color = if (chall.isJoined) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB D: LEADERBOARD
// ==========================================
@Composable
fun LeaderboardTabContent() {
    val friendsList = listOf(
        Triple("AlphaBeast_01", 1450, 4), // Username, XP, Streak (You)
        Triple("@coach_mike", 15200, 14),
        Triple("SoniaActive", 4800, 8),
        Triple("YogaFlowAva", 3200, 2),
        Triple("FlexPioneer", 900, 0)
    ).sortedByDescending { it.second }

    // State for keeping reactions counted per user row
    val fireReactions = remember { mutableStateMapOf<String, Int>() }
    val rocketReactions = remember { mutableStateMapOf<String, Int>() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Лига лидеров",
                    color = PremiumWhite,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp
                )
                Text(
                    text = "Следите за прогрессом друзей и отправляйте мотивирущие реакции.",
                    color = SubtitleGrey,
                    fontSize = 13.sp
                )
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "Info", tint = NeonCyan)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Дружеская поддержка и соревновательный дух повышают регулярность тренировок на 84% по сравнению с обычными одиночными занятиями.",
                        color = SubtitleGrey,
                        fontSize = 11.sp
                    )
                }
            }
        }

        items(friendsList.indices.toList()) { index ->
            val user = friendsList[index]
            val isCurrentUser = user.first == "AlphaBeast_01"

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrentUser) Color(0x2200E5FF) else GlassSurface
                ),
                border = BorderStroke(
                    width = 1.3.dp,
                    color = if (isCurrentUser) NeonCyan else GlassBorder
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(14.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "#${index + 1}",
                            color = when (index) {
                                0 -> Color(0xFFFFD700)
                                1 -> Color(0xFFC0C0C0)
                                2 -> Color(0xFFCD7F32)
                                else -> SubtitleGrey
                            },
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            modifier = Modifier.width(36.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(DarkSlate),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.first.take(1).uppercase(),
                                color = PremiumWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = user.first,
                                    color = PremiumWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                if (isCurrentUser) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ВЫ",
                                        color = NeonCyan,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Text(
                                text = "${user.second} XP всего • Серия: ${user.third} дн.",
                                color = SubtitleGrey,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Social cheering reactions
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Fire Button
                        val fireCount = fireReactions[user.first] ?: 12
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x22FF5500))
                                .clickable { fireReactions[user.first] = fireCount + 1 }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🔥", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "$fireCount",
                                    color = Color(0xFFFF5500),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Rocket Button
                        val rocketCount = rocketReactions[user.first] ?: 4
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x2200FF66))
                                .clickable { rocketReactions[user.first] = rocketCount + 1 }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🚀", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "$rocketCount",
                                    color = CyberNeon,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB E: CREATOR HUB & INFLUENCER DASHBOARD
// ==========================================
@Composable
fun CreatorHubTabContent(
    profile: com.example.data.UserProfileEntity?,
    challenges: List<ChallengeEntity>,
    viewModel: FitViewModel
) {
    val nonNullProfile = profile ?: com.example.data.UserProfileEntity()
    var creatorTitleInput by remember { mutableStateOf("") }
    var creatorDescInput by remember { mutableStateOf("") }
    var creatorFeeInput by remember { mutableStateOf("4.99") }
    var creatorDaysInput by remember { mutableStateOf("30") }
    
    val myBrandedChallenges = challenges.filter { it.creatorName == "@Creator_Me" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Центр авторов",
                        color = PremiumWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp
                    )
                    Text(
                        text = "Уровень инфлюенсера • Выплаты 70/30",
                        color = SubtitleGrey,
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x3300E5FF))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Verified, contentDescription = "Creator tier", tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ПОДТВЕРЖДЕНО", color = NeonCyan, fontWeight = FontWeight.Black, fontSize = 10.sp)
                    }
                }
            }
        }

        // Revenue stats dashboard
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = "НАКОПЛЕННЫЙ БАЛАНС ВЫПЛАТ", color = SubtitleGrey, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$${String.format("%.2f", nonNullProfile.revenueBalance)} USD",
                        color = CyberNeon,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )

                    Button(
                        onClick = { viewModel.withdrawCreatorEarnings(nonNullProfile.revenueBalance) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberNeon),
                        shape = RoundedCornerShape(10.dp),
                        enabled = nonNullProfile.revenueBalance > 0.0
                    ) {
                        Text("Выплата", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Всего участников: 1 402", color = SubtitleGrey, fontSize = 11.sp)
                    Text("Распределение комиссии: 70% автору", color = SubtitleGrey, fontSize = 11.sp)
                }
            }
        }

        // Section header to launch new sponsor/branded campaign
        item {
            Text(
                text = "Запуск фитнес-кампании",
                color = PremiumWhite,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        // Creation form fields
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = creatorTitleInput,
                    onValueChange = { creatorTitleInput = it },
                    label = { Text("Название (напр., Сушка 30 дней)") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0x33FFFFFF),
                        unfocusedContainerColor = Color(0x11FFFFFF),
                        focusedLabelColor = KineticFlame,
                        focusedTextColor = PremiumWhite,
                        unfocusedTextColor = PremiumWhite
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                TextField(
                    value = creatorDescInput,
                    onValueChange = { creatorDescInput = it },
                    label = { Text("Описание и правила выполнения") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0x33FFFFFF),
                        unfocusedContainerColor = Color(0x11FFFFFF),
                        focusedLabelColor = KineticFlame,
                        focusedTextColor = PremiumWhite,
                        unfocusedTextColor = PremiumWhite
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = creatorFeeInput,
                        onValueChange = { creatorFeeInput = it },
                        label = { Text("Взнос ($)") },
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color(0x11FFFFFF), focusedTextColor = PremiumWhite),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    TextField(
                        value = creatorDaysInput,
                        onValueChange = { creatorDaysInput = it },
                        label = { Text("Срок (дней)") },
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color(0x11FFFFFF), focusedTextColor = PremiumWhite),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val fee = creatorFeeInput.toDoubleOrNull() ?: 0.0
                        val days = creatorDaysInput.toIntOrNull() ?: 30
                        if (creatorTitleInput.isNotBlank()) {
                            viewModel.launchCustomInfluencerChallenge(
                                title = creatorTitleInput,
                                description = creatorDescInput,
                                fee = fee,
                                duration = days,
                                rewardXp = 1000 + (fee * 100).toInt()
                            )
                            creatorTitleInput = ""
                            creatorDescInput = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = KineticFlame),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Опубликовать кампанию", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Show creator's active deployed campaigns
        if (myBrandedChallenges.isNotEmpty()) {
            item {
                Text(
                    text = "Мои активные кампании",
                    color = PremiumWhite,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }

            items(myBrandedChallenges) { chall ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GlassSurface),
                    border = BorderStroke(1.dp, CyberNeon)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = chall.title, color = PremiumWhite, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text(text = "Взнос: $${chall.entryFee}", color = CyberNeon, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Text(text = chall.description, color = SubtitleGrey, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Активные участники: 1 атлет (вы сами)", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// MANUAL LOGGING ACTIVITY SHEET
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLogSheet(
    onDismiss: () -> Unit,
    onSave: (String, Int, Double, Boolean) -> Unit
) {
    var type by remember { mutableStateOf("Run") }
    var durationText by remember { mutableStateOf("30") }
    var distanceText by remember { mutableStateOf("5.0") }
    var isGpsTracked by remember { mutableStateOf(false) }

    val activityCategories = listOf("Run", "Gym", "Bike", "Swim", "Yoga")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF16161D)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Запись тренировки",
                color = PremiumWhite,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Gym, Bike, Swim toggle icons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                activityCategories.forEach { cat ->
                    val isSelected = type == cat
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .clickable { type = cat },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0x33FF5500) else Color(0x11FFFFFF)
                        ),
                        border = BorderStroke(width = 1.dp, color = if (isSelected) KineticFlame else GlassBorder)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (cat) {
                                    "Run" -> Icons.Default.DirectionsRun
                                    "Bike" -> Icons.Default.DirectionsBike
                                    "Swim" -> Icons.Default.Pool
                                    "Yoga" -> Icons.Default.Spa
                                    else -> Icons.Default.FitnessCenter
                                },
                                contentDescription = cat,
                                tint = if (isSelected) KineticFlame else SubtitleGrey,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            TextField(
                value = durationText,
                onValueChange = { durationText = it },
                label = { Text("Длительность (мин)") },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = PremiumWhite,
                    unfocusedTextColor = PremiumWhite,
                    focusedContainerColor = Color(0x11FFFFFF)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (type == "Run" || type == "Bike" || type == "Swim") {
                TextField(
                    value = distanceText,
                    onValueChange = { distanceText = it },
                    label = { Text("Дистанция (км)") },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = PremiumWhite,
                        unfocusedTextColor = PremiumWhite,
                        focusedContainerColor = Color(0x11FFFFFF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Симулировать GPS-трекинг", color = PremiumWhite, fontSize = 14.sp)
                    Switch(
                        checked = isGpsTracked,
                        onCheckedChange = { isGpsTracked = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyberNeon)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Image verification trigger simulation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x11FFFFFF))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Camera upload", tint = NeonCyan)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "Фотоподтверждение прикреплено", color = PremiumWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "Селфи-снимок тренировки добавит бонусный опыт.", color = SubtitleGrey, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val duration = durationText.toIntOrNull() ?: 30
                    val distance = distanceText.toDoubleOrNull() ?: 0.0
                    onSave(type, duration, distance, isGpsTracked)
                },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_workout_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = KineticFlame)
            ) {
                Text("Записать тренировку", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ==========================================
// CINEMATIC LOOT-BOX UNLOCK SHOW
// ==========================================
@Composable
fun LootBoxCelebrationDialog(
    unlockedBadges: List<BadgeEntity>,
    onDismiss: () -> Unit
) {
    com.example.ui.LootBoxCelebrationDialog(
        badgesToCelebrate = unlockedBadges,
        onDismiss = onDismiss
    )
}

// ==========================================
// CINEMATIC RANK-UP CELEBRATION
// ==========================================
@Composable
fun RankCelebrationScreen(
    rankInfo: com.example.data.FitRepository.RankTier,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070709)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "СТАТУС АТЛЕТА ПОВЫШЕН",
                    color = CyberNeon,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "НОВЫЙ РАНГ!",
                    color = PremiumWhite,
                    fontWeight = FontWeight.Black,
                    fontSize = 44.sp,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Huge Medal Render
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1E26))
                        .border(4.dp, Color(android.graphics.Color.parseColor(rankInfo.hexColor)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MilitaryTech,
                            contentDescription = "Rank up tier",
                            tint = Color(android.graphics.Color.parseColor(rankInfo.hexColor)),
                            modifier = Modifier.size(96.dp)
                        )
                        Text(
                            text = "УРОВЕНЬ ${rankInfo.tier}",
                            color = SubtitleGrey,
                             fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = rankInfo.title,
                    color = Color(android.graphics.Color.parseColor(rankInfo.hexColor)),
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = rankInfo.description,
                    color = SubtitleGrey,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = KineticFlame),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text("Отлично", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }
        }
    }
}

// ==========================================
// HELPERS: Gradients and cosmetics
// ==========================================
@Composable
fun HyperNeonGradientBrush() = Brush.linearGradient(
    colors = listOf(KineticFlame, CyberNeon, NeonCyan)
)
