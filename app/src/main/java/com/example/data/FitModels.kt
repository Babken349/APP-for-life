package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_logs")
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "Run", "Gym", "Bike", "Swim", "Yoga"
    val durationMinutes: Int,
    val distanceKm: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val xpEarned: Int,
    val photoUri: String? = null,
    val isGpsTracked: Boolean = false
)

@Entity(tableName = "badges")
data class BadgeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String, // "Distance", "Streak", "Speed", "Social", "Secret"
    val description: String,
    val triggerCondition: String,
    val xpReward: Int,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long = 0L,
    val iconName: String, // For rendering
    val visualColor: String // Hex tag
)

@Entity(tableName = "challenges")
data class ChallengeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val creatorName: String,
    val creatorAvatar: String,
    val isVerifiedCreator: Boolean = false,
    val entryFee: Double = 0.0, // 0.0 = Free
    val participantCount: Int = 142,
    val description: String,
    val durationDays: Int = 30,
    val xpReward: Int = 1000,
    val isInfluencer: Boolean = false,
    val isJoined: Boolean = false,
    val isCompleted: Boolean = false
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = "current_user",
    val username: String = "FitnessBeast",
    val fitnessGoal: String = "Build Strength", // "Lose Weight", "Build Strength", "Stay Active", "Run Far"
    val totalXp: Int = 0,
    val streakDays: Int = 0,
    val lastActiveTimestamp: Long = 0L,
    val isPremium: Boolean = false,
    val currentRankTier: Int = 1, // 1 to 10
    val healthKitConnected: Boolean = false,
    val revenueBalance: Double = 349.50 // Mock revenue split balance for B2B2C demo
)
