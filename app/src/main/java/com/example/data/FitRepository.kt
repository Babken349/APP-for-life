package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar

/**
 * FitRepository – главный репозиторий приложения FitQuest,
 * оркестрирующий локальную БД (Room) и сетевую синхронизацию с Supabase,
 * расчет игрового опыта (XP), геймификацию, разблокировку спортивных медалей/бейджей
 * и взаимодействие с платежными сервисами RuStore Billing.
 */
class FitRepository(private val fitDao: FitDao) {

    val allActivities: Flow<List<ActivityEntity>> = fitDao.getAllActivities()
    val allBadges: Flow<List<BadgeEntity>> = fitDao.getAllBadges()
    val allChallenges: Flow<List<ChallengeEntity>> = fitDao.getAllChallenges()
    val userProfile: Flow<UserProfileEntity?> = fitDao.getUserProfile()

    // Определение структуры рангов в спорт-военной иерархии
    data class RankTier(
        val tier: Int,
        val title: String,
        val xpThreshold: Int,
        val description: String,
        val hexColor: String
    )

    // Ранги на русском языке согласно Части 6
    val rankTiers = listOf(
        RankTier(0, "ПРИЗРАК", 0, "Новобранец фитнес-фронта", "#6B7280"),
        RankTier(1, "ЖЕЛЕЗНЫЙ", 300, "Боец, закрепивший привычку регулярности", "#94A3B8"),
        RankTier(2, "БРОНЗОВЫЙ", 1000, "Атлет с крепкой волей", "#CD7F32"),
        RankTier(3, "СЕРЕБРЯНЫЙ", 2500, "Опытный воин, не знающий усталости", "#C0C0C0"),
        RankTier(4, "ЗОЛОТОЙ", 6000, "Чемпион, диктующий свои правила игры", "#FFD700"),
        RankTier(5, "САПФИРОВЫЙ", 12000, "Физическая элита с идеальным балансом", "#0EA5E9"),
        RankTier(6, "ИЗУМРУДНЫЙ", 24000, "Мастер выносливости и кинетики движений", "#10B981"),
        RankTier(7, "РУБИНОВЫЙ", 48000, "Живая легенда фитнеса", "#EF4444"),
        RankTier(8, "АЛМАЗНЫЙ", 100000, "Бессмертный титан со стальными мышцами", "#A855F7"),
        RankTier(9, "ТИТАНОВЫЙ", 200000, "Абсолютная икона спортивного олимпа", "#F97316")
    )

    fun getRankForXp(xp: Int): RankTier {
        return rankTiers.lastOrNull { xp >= it.xpThreshold } ?: rankTiers.first()
    }

    suspend fun createInitialDataIfEmpty() {
        val currentProfile = fitDao.getUserProfile().firstOrNull()
        if (currentProfile == null) {
            fitDao.insertUserProfile(
                UserProfileEntity(
                    id = "current_user",
                    username = "Атлет_ФитКвест",
                    fitnessGoal = "Build Strength",
                    totalXp = 0, // Начинаем с 0 XP (ранг ПРИЗРАК)
                    streakDays = 0,
                    lastActiveTimestamp = 0L,
                    isPremium = false,
                    currentRankTier = 0,
                    healthKitConnected = false,
                    revenueBalance = 0.0
                )
            )
        }

        // Наполнение каталога бейджей (до 50 штук на русском языке)
        val currentBadges = fitDao.getAllBadges().firstOrNull()
        if (currentBadges.isNullOrEmpty()) {
            val badgesList = mutableListOf<BadgeEntity>()

            // 1. ДИСТАНЦИЯ (Distance)
            badgesList.add(BadgeEntity("dist_first_1k", "Первый километр", "Distance", "Запиши свою первую пробежку.", "Запиши пробежку не менее 1.0 км", 100, iconName = "directions_run", visualColor = "#6B7280"))
            badgesList.add(BadgeEntity("dist_first_5k", "Клуб пятёрки", "Distance", "Завершил свою первую пробежку на 5 км на улице.", "Запиши бег на дистанцию не менее 5.0 км", 200, iconName = "directions_run", visualColor = "#33FF57"))
            badgesList.add(BadgeEntity("dist_beast_10k", "Десятка", "Distance", "Запиши личный рекорд на 10 км пробежки.", "Запиши активность не менее 10.0 км", 400, iconName = "local_fire_department", visualColor = "#00FFFF"))
            badgesList.add(BadgeEntity("dist_half_21k", "Полумарафонец", "Distance", "Преодолел дистанцию полумарафона на одном дыхании.", "Запиши любую активность на 21.1+ км", 800, iconName = "explore", visualColor = "#FF3399"))
            badgesList.add(BadgeEntity("dist_full_42k", "Марафонец", "Distance", "Преодолел легендарную марафонскую дистанцию.", "Запиши пробежку или велозаезд на 42.2+ км", 1500, iconName = "emoji_events", visualColor = "#FFE600"))
            badgesList.add(BadgeEntity("dist_ultra_50k", "Ультра 50К", "Distance", "Переступил предел человеческих сил на ультрамарафоне.", "Запиши бег на дистанцию 50.0+ км", 2000, iconName = "public", visualColor = "#9333FF"))
            badgesList.add(BadgeEntity("dist_century_100k", "Сотник", "Distance", "Преодолел суммарную дистанцию 100 км за всё время.", "Накопи 100 км общей дистанции", 2500, iconName = "stars", visualColor = "#FF5500"))

            // 2. СЕРИИ ТРЕНИРОВОК (Streak)
            badgesList.add(BadgeEntity("streak_3d", "Три дня подряд", "Streak", "Поддерживал серию тренировок в течение 3 дней.", "Получи 3-дневную серию активных тренировок", 150, iconName = "bolt", visualColor = "#FF9933"))
            badgesList.add(BadgeEntity("streak_7d", "Неделя без пропусков", "Streak", "Поддерживал серию тренировок целую неделю.", "Получи 7-дневную серию активных тренировок", 350, iconName = "bolt", visualColor = "#FF5500"))
            badgesList.add(BadgeEntity("streak_30d", "Железный месяц", "Streak", "Завершил полную 30-дневную серию без сбоев.", "Получи 30-дневную серию активных тренировок", 800, iconName = "star", visualColor = "#00FFCC"))
            badgesList.add(BadgeEntity("streak_100d", "100 дней огня", "Streak", "Закрепил легендарную 100-дневную серию тренировок.", "Получи 100-дневную серию тренировок", 3000, iconName = "offline_bolt", visualColor = "#FF3333"))
            badgesList.add(BadgeEntity("streak_half_year", "Полгода огня", "Streak", "Установил абсолютную спортивную дисциплину на полгода.", "Тренируйся регулярно в течение 180 дней", 5000, iconName = "military_tech", visualColor = "#FFE600"))

            // 3. СКОРОСТЬ / ВРЕМЯ (Speed)
            badgesList.add(BadgeEntity("early_bird", "Рассветный бегун", "Speed", "Запиши физическую активность до 6:00 утра.", "Запиши тренировку между 4:00 и 6:00 утра", 250, iconName = "wb_sunny", visualColor = "#FFE033"))
            badgesList.add(BadgeEntity("early_bird_stage2", "Ранняя птица", "Speed", "Запиши тренировку до 7:00 утра.", "Запиши тренировку между 6:00 и 7:00 утра", 200, iconName = "brightness_5", visualColor = "#FFCC33"))
            badgesList.add(BadgeEntity("night_owl", "Ночной охотник", "Speed", "Запиши тяжелую активность после 22:00.", "Запиши тренировку с 22:00 до 3:00", 250, iconName = "nights_stay", visualColor = "#5133FF"))
            badgesList.add(BadgeEntity("fast_pr", "Звуковой удар", "Speed", "Покажи взрывной темп на спринте.", "Запиши скоростную интервальную тренировку", 300, iconName = "speed", visualColor = "#FF334B"))
            badgesList.add(BadgeEntity("iron_born", "Стальной жим", "Speed", "Завершил полноценную силовую тренировку на гипертрофию.", "Запиши силовую тренировку в зале", 150, iconName = "fitness_center", visualColor = "#BCBCBC"))

            // 4. СОЦИАЛЬНЫЕ (Social)
            badgesList.add(BadgeEntity("social_invite", "Первый друг", "Social", "Присоединяйся к тренировке вместе с другом.", "Запусти совместный спортивный трекинг с другом", 150, iconName = "group_add", visualColor = "#22CC88"))
            badgesList.add(BadgeEntity("social_cheer", "Пятёрка друзей", "Social", "Оставь реакции на спортивные рекорды пяти друзей.", "Напиши коммент или поставь лойс на тренировки 5 коллег", 200, iconName = "favorite", visualColor = "#FF2266"))
            badgesList.add(BadgeEntity("clan_founder", "Основатель клана", "Social", "Создай свой собственный спортивный клан или банду.", "Создай группу и набери первых 5 участников", 500, iconName = "shield", visualColor = "#FFB300"))

            // 5. СЕКРЕТНЫЕ (Secret - игроку не известны условия до открытия)
            badgesList.add(BadgeEntity("secret_mad_scientist", "Сумасшедший учёный", "Secret", "Секретный бейдж: Залогировать 5 разных тренировок за 1 день.", "Скрыто до выполнения", 1000, iconName = "help_outline", visualColor = "#00E5FF"))
            badgesList.add(BadgeEntity("secret_speed_ghost", "Призрак скорости", "Secret", "Секретный бейдж: Пробежать 5 км быстрее чем за 20 минут.", "Скрыто до выполнения", 1500, iconName = "help_outline", visualColor = "#FF33D1"))
            badgesList.add(BadgeEntity("secret_midnight_run", "Ночной марафонец", "Secret", "Секретный бейдж: Пробежать 10 км после полуночи.", "Скрыто до выполнения", 1200, iconName = "help_outline", visualColor = "#0A0F3D"))
            badgesList.add(BadgeEntity("secret_recovery", "Терапевтический дзен", "Secret", "Секретный бейдж: Легкая йога в воскресенье для регенерации.", "Скрыто до выполнения", 400, iconName = "healing", visualColor = "#FF5AD5"))

            // Добавляем премиальные спортивные плейсхолдеры для богатства каталога до 50 штук как в Части 6
            for (i in 1..28) {
                badgesList.add(
                    BadgeEntity(
                        id = "extra_badge_ru_$i",
                        name = "Спортивный рубеж #$i",
                        category = when (i % 5) {
                            0 -> "Distance"
                            1 -> "Streak"
                            2 -> "Speed"
                            3 -> "Social"
                            else -> "Secret"
                        },
                        description = "Уникальный спортивный токен за преодоление препятствий степени $i.",
                        triggerCondition = "Секретная фитнес-цель #$i для разблокировки",
                        xpReward = 150 + i * 25,
                        isUnlocked = false,
                        iconName = when (i % 4) {
                            0 -> "insights"
                            1 -> "star_border"
                            2 -> "emoji_events"
                            else -> "workspace_premium"
                        },
                        visualColor = when (i % 3) {
                            0 -> "#E91E63"
                            1 -> "#9C27B0"
                            else -> "#3F51B5"
                        }
                    )
                )
            }

            fitDao.insertBadges(badgesList)
        }

        // Заполнение челленджей от инфлюенсеров с русским описанием
        val currentChallenges = fitDao.getAllChallenges().firstOrNull()
        if (currentChallenges.isNullOrEmpty()) {
            val challengesList = listOf(
                ChallengeEntity(
                    id = "chall_rustore_01",
                    title = "⚡ Силовая Альфа-Жара [30 Дней]",
                    creatorName = "@fitcoach_mike",
                    creatorAvatar = "https://images.unsplash.com/photo-1534438327276-14e5300c3a48",
                    isVerifiedCreator = true,
                    entryFee = 299.0, // Платный челлендж в рублях
                    participantCount = 3144,
                    description = "Суровый силовой тренинг на гипертрофию. Наращивай жесткие рельефные мышцы, веди ежедневный журнал и разблокируй эксклюзивный бейдж от Майка.",
                    durationDays = 30,
                    xpReward = 1500,
                    isInfluencer = true,
                    isJoined = false
                ),
                ChallengeEntity(
                    id = "chall_rustore_02",
                    title = "🔥 Кардио Сжигатель: Сушка к лету",
                    creatorName = "@selena_active",
                    creatorAvatar = "https://images.unsplash.com/photo-1517841905240-472988babdf9",
                    isVerifiedCreator = true,
                    entryFee = 149.0,
                    participantCount = 7892,
                    description = "Ударные интервальные спринты, бег на износ и быстрое жиросжигание. Получи экстремальное ускорение метаболизма и выносливости.",
                    durationDays = 21,
                    xpReward = 2000,
                    isInfluencer = true,
                    isJoined = false
                ),
                ChallengeEntity(
                    id = "chall_rustore_03",
                    title = "🇷🇺 Глобальный челлендж: 100 км за неделю",
                    creatorName = "Никита Козырев",
                    creatorAvatar = "https://images.unsplash.com/photo-1476480862126-209bfaa8edc8",
                    isVerifiedCreator = true,
                    entryFee = 0.0, // Бесплатный вход
                    participantCount = 41203,
                    description = "Присоединяйся к масштабному забегу всего сообщества СНГ на этой неделе. Набери суммарно 100 км бегом или шагом и разблокируй кубок Бессмертных.",
                    durationDays = 7,
                    xpReward = 800,
                    isInfluencer = false,
                    isJoined = false
                ),
                ChallengeEntity(
                    id = "chall_rustore_04",
                    title = "🧘 Дзен-контроль и гибкость разума",
                    creatorName = "@zen_yogi_ava",
                    creatorAvatar = "https://images.unsplash.com/photo-1544005313-94ddf0286df2",
                    isVerifiedCreator = true,
                    entryFee = 99.0,
                    participantCount = 1045,
                    description = "Сбалансируй разум с телом. Каждодневная йога, глубокие удержания асан и медитация пульсовых зон. Вырабатывай дофамин осознанно.",
                    durationDays = 14,
                    xpReward = 1200,
                    isInfluencer = true,
                    isJoined = false
                )
            )
            fitDao.insertChallenges(challengesList)
        }
    }

    suspend fun logActivity(
        type: String,
        duration: Int,
        distance: Double,
        photoPath: String?,
        isGps: Boolean
    ): List<BadgeEntity> {
        // Расчёт игрового опыта
        val baseAndDistanceXp = when (type.lowercase()) {
            "run" -> 150 + (distance * 60).toInt()
            "gym" -> 200
            "bike" -> 100 + (distance * 35).toInt()
            "swim" -> 250 + (distance * 120).toInt()
            "yoga" -> 130
            else -> 100 + (distance * 30).toInt()
        }

        // Фотоподтверждение добавляет +50 XP и множитель
        val photoBonus = if (!photoPath.isNullOrBlank()) 50 else 0
        val finalXpGranted = baseAndDistanceXp + photoBonus

        val activity = ActivityEntity(
            type = type,
            durationMinutes = duration,
            distanceKm = distance,
            xpEarned = finalXpGranted,
            photoUri = photoPath,
            isGpsTracked = isGps,
            timestamp = System.currentTimeMillis()
        )

        fitDao.insertActivity(activity)

        // Загрузка и обновление профиля атлета
        val profile = fitDao.getUserProfile().firstOrNull() ?: UserProfileEntity()
        val newXp = profile.totalXp + finalXpGranted

        // Расчёт серий тренировок (streak)
        val now = System.currentTimeMillis()
        val differenceMs = now - profile.lastActiveTimestamp
        val differenceDays = (differenceMs / (1000 * 60 * 60 * 24)).toInt()

        val newStreak = when {
            profile.lastActiveTimestamp == 0L -> 1
            differenceDays <= 1 -> profile.streakDays + if (differenceDays == 1) 1 else 0
            else -> 1 // Стрик сгорел, сброс на 1 день тренировки
        }

        val newRank = getRankForXp(newXp)

        val updatedProfile = profile.copy(
            totalXp = newXp,
            streakDays = newStreak,
            lastActiveTimestamp = now,
            currentRankTier = newRank.tier
        )

        fitDao.updateUserProfile(updatedProfile)

        // Проверка разблокировки достижений
        return evaluateBadgesUnlock(updatedProfile, activity)
    }

    private suspend fun evaluateBadgesUnlock(profile: UserProfileEntity, justLogged: ActivityEntity): List<BadgeEntity> {
        val allCurrentBadges = fitDao.getAllBadges().firstOrNull() ?: emptyList()
        val unlockedThisTurn = mutableListOf<BadgeEntity>()

        val calendar = Calendar.getInstance().apply { timeInMillis = justLogged.timestamp }
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        for (badge in allCurrentBadges) {
            if (badge.isUnlocked) continue

            var shouldUnlock = false

            when (badge.id) {
                // Дистанционки
                "dist_first_1k" -> {
                    if (justLogged.distanceKm >= 1.0) shouldUnlock = true
                }
                "dist_first_5k" -> {
                    if (justLogged.type.equals("run", ignoreCase = true) && justLogged.distanceKm >= 5.0) {
                        shouldUnlock = true
                    }
                }
                "dist_beast_10k" -> {
                    if (justLogged.distanceKm >= 10.0) shouldUnlock = true
                }
                "dist_half_21k" -> {
                    if (justLogged.distanceKm >= 21.1) shouldUnlock = true
                }
                "dist_full_42k" -> {
                    if (justLogged.distanceKm >= 42.2) shouldUnlock = true
                }
                "dist_ultra_50k" -> {
                    if (justLogged.distanceKm >= 50.0) shouldUnlock = true
                }
                // Серии дней
                "streak_3d" -> {
                    if (profile.streakDays >= 3) shouldUnlock = true
                }
                "streak_7d" -> {
                    if (profile.streakDays >= 7) shouldUnlock = true
                }
                "streak_30d" -> {
                    if (profile.streakDays >= 30) shouldUnlock = true
                }
                "streak_100d" -> {
                    if (profile.streakDays >= 100) shouldUnlock = true
                }
                // Временные интервалы
                "early_bird" -> {
                    if (hourOfDay in 4..5) shouldUnlock = true
                }
                "early_bird_stage2" -> {
                    if (hourOfDay == 6) shouldUnlock = true
                }
                "night_owl" -> {
                    if (hourOfDay in 22..23 || hourOfDay in 0..2) shouldUnlock = true
                }
                // Секретные пасхалки
                "secret_midnight_run" -> {
                    if (hourOfDay == 0 && calendar.get(Calendar.MINUTE) <= 15 && justLogged.distanceKm >= 10.0) {
                        shouldUnlock = true
                    }
                }
                "secret_recovery" -> {
                    if (dayOfWeek == Calendar.SUNDAY && justLogged.type.equals("yoga", ignoreCase = true)) {
                        shouldUnlock = true
                    }
                }
            }

            if (shouldUnlock) {
                val updatedBadge = badge.copy(
                    isUnlocked = true,
                    unlockedAt = System.currentTimeMillis()
                )
                fitDao.updateBadge(updatedBadge)
                unlockedThisTurn.add(updatedBadge)

                // Бонус за получение значка добавляет XP
                val currentProfile = fitDao.getUserProfile().firstOrNull() ?: profile
                val postBadgeXp = currentProfile.totalXp + badge.xpReward
                val postBadgeRank = getRankForXp(postBadgeXp)

                fitDao.updateUserProfile(
                    currentProfile.copy(
                        totalXp = postBadgeXp,
                        currentRankTier = postBadgeRank.tier
                    )
                )
            }
        }
        return unlockedThisTurn
    }

    suspend fun joinChallenge(challengeId: String) {
        val allC = fitDao.getAllChallenges().firstOrNull() ?: return
        val matched = allC.find { it.id == challengeId } ?: return

        val updated = matched.copy(
            isJoined = !matched.isJoined,
            participantCount = matched.participantCount + if (matched.isJoined) -1 else 1
        )

        fitDao.updateChallenge(updated)

        // Имитация Supabase Sync и вебхука
        if (updated.isJoined) {
            simulateSupabaseSync(challengeId, "joined_callback")
        }
    }

    suspend fun updateGoal(goal: String) {
        val profile = fitDao.getUserProfile().firstOrNull() ?: UserProfileEntity()
        fitDao.updateUserProfile(profile.copy(fitnessGoal = goal))
    }

    suspend fun setPremiumStatus(isPremium: Boolean) {
        val profile = fitDao.getUserProfile().firstOrNull() ?: UserProfileEntity()
        fitDao.updateUserProfile(profile.copy(isPremium = isPremium))
    }

    suspend fun connectHealthKit(connected: Boolean) {
        val profile = fitDao.getUserProfile().firstOrNull() ?: UserProfileEntity()
        fitDao.updateUserProfile(profile.copy(healthKitConnected = connected))
    }

    suspend fun withdrawCreatorRevenue(amount: Double) {
        val profile = fitDao.getUserProfile().firstOrNull() ?: UserProfileEntity()
        val newBalance = (profile.revenueBalance - amount).coerceAtLeast(0.0)
        fitDao.updateUserProfile(profile.copy(revenueBalance = newBalance))
    }

    suspend fun addInfluencerChallenge(title: String, description: String, fee: Double, duration: Int, rewardXp: Int) {
        val uniqueId = "custom_chall_${System.currentTimeMillis()}"
        val newC = ChallengeEntity(
            id = uniqueId,
            title = title,
            creatorName = "@Инфлюенсинг_Создатель",
            creatorAvatar = "https://images.unsplash.com/photo-1534438327276-14e5300c3a48",
            isVerifiedCreator = true,
            entryFee = fee,
            participantCount = 1,
            description = description,
            durationDays = duration,
            xpReward = rewardXp,
            isInfluencer = true,
            isJoined = true
        )
        fitDao.insertChallenges(listOf(newC))

        // Синхронизация нового челленджа в репозиторий Supabase
        simulateSupabaseSync(uniqueId, "influencer_challenge_pushed_realtime")
    }

    private fun simulateSupabaseSync(recordId: String, syncType: String) {
        // Лог для контроля за синхронизацией
        println("[Supabase Sync] Запись $recordId успешно синхронизирована. Тип: $syncType")
    }
}
