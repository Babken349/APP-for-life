package com.example.ui

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FitViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FitRepository
    val billingManager: RuStoreBillingManager
    
    init {
        val database = FitDatabase.getDatabase(application)
        repository = FitRepository(database.fitDao)
        billingManager = RuStoreBillingManager(application, repository, viewModelScope)
        
        viewModelScope.launch {
            repository.createInitialDataIfEmpty()
        }
    }

    val activities: StateFlow<List<ActivityEntity>> = repository.allActivities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val badges: StateFlow<List<BadgeEntity>> = repository.allBadges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val challenges: StateFlow<List<ChallengeEntity>> = repository.allChallenges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Cinematic celebrations state
    val newlyUnlockedBadges = mutableStateOf<List<BadgeEntity>>(emptyList())
    val showRankUpCelebration = mutableStateOf<Pair<Boolean, Int>?>(null) // Pair(Show?, NewTier)

    private var cachedRankTier: Int? = null

    init {
        // Track rank progression levels to trigger the Level-Up cinematic overlay
        viewModelScope.launch {
            userProfile.collect { profile ->
                if (profile != null) {
                    val currentTier = profile.currentRankTier
                    val cached = cachedRankTier
                    if (cached != null && currentTier > cached) {
                        // Rank-up event!
                        showRankUpCelebration.value = Pair(true, currentTier)
                    }
                    cachedRankTier = currentTier
                }
            }
        }
    }

    fun getRankInfo(tier: Int): FitRepository.RankTier {
        return repository.rankTiers.firstOrNull { it.tier == tier } 
            ?: repository.rankTiers.first()
    }

    val rankTiersList = repository.rankTiers

    fun logWorkout(type: String, duration: Int, distance: Double, photoUri: String?, isGps: Boolean) {
        viewModelScope.launch {
            val newlyUnlocked = repository.logActivity(type, duration, distance, photoUri, isGps)
            if (newlyUnlocked.isNotEmpty()) {
                newlyUnlockedBadges.value = newlyUnlocked
            }
        }
    }

    fun toggleJoinChallenge(challengeId: String) {
        viewModelScope.launch {
            repository.joinChallenge(challengeId)
        }
    }

    fun updateFitnessGoal(goal: String) {
        viewModelScope.launch {
            repository.updateGoal(goal)
        }
    }

    fun setPremium(isPremium: Boolean) {
        viewModelScope.launch {
            repository.setPremiumStatus(isPremium)
        }
    }

    fun toggleHealthKit(connected: Boolean) {
        viewModelScope.launch {
            repository.connectHealthKit(connected)
        }
    }

    fun withdrawCreatorEarnings(amount: Double) {
        viewModelScope.launch {
            repository.withdrawCreatorRevenue(amount)
        }
    }

    fun launchCustomInfluencerChallenge(
        title: String,
        description: String,
        fee: Double,
        duration: Int,
        rewardXp: Int
    ) {
        viewModelScope.launch {
            repository.addInfluencerChallenge(title, description, fee, duration, rewardXp)
        }
    }

    fun clearBadgeCelebration() {
        newlyUnlockedBadges.value = emptyList()
    }

    fun clearRankUpCelebration() {
        showRankUpCelebration.value = null
    }
}
