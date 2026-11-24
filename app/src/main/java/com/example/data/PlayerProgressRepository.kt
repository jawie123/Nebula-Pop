package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nebula_pop_prefs")

enum class BoosterKind {
    BOMB,
    LIGHTNING,
    WILD
}

data class PlayerProgress(
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val colorblindEnabled: Boolean = false,
    val highestUnlockedSector: Int = 1,
    val sectorStars: Map<Int, Int> = emptyMap(),
    val bestSectorScore: Int = 0,
    val bestEndlessScore: Int = 0,
    val bombCount: Int = 3,
    val lightningCount: Int = 3,
    val wildCount: Int = 3
) {
    val totalStars: Int get() = sectorStars.values.sum()
    val maxPossibleStars: Int get() = TOTAL_SECTORS * 3

    fun countOf(kind: BoosterKind): Int = when (kind) {
        BoosterKind.BOMB -> bombCount
        BoosterKind.LIGHTNING -> lightningCount
        BoosterKind.WILD -> wildCount
    }

    companion object {
        const val TOTAL_SECTORS = 20
    }
}

class PlayerProgressRepository(private val context: Context) {

    private object Keys {
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val COLORBLIND_ENABLED = booleanPreferencesKey("colorblind_enabled")
        val HIGHEST_SECTOR = intPreferencesKey("highest_sector")
        val SECTOR_STARS = stringPreferencesKey("sector_stars")
        val BEST_SECTOR_SCORE = intPreferencesKey("best_sector_score")
        val BEST_ENDLESS_SCORE = intPreferencesKey("best_endless_score")
        val BOMB_COUNT = intPreferencesKey("bomb_count")
        val LIGHTNING_COUNT = intPreferencesKey("lightning_count")
        val WILD_COUNT = intPreferencesKey("wild_count")
    }

    val progressFlow: Flow<PlayerProgress> = context.dataStore.data.map { prefs ->
        PlayerProgress(
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            colorblindEnabled = prefs[Keys.COLORBLIND_ENABLED] ?: false,
            highestUnlockedSector = (prefs[Keys.HIGHEST_SECTOR] ?: 1)
                .coerceIn(1, PlayerProgress.TOTAL_SECTORS),
            sectorStars = decodeStars(prefs[Keys.SECTOR_STARS]),
            bestSectorScore = prefs[Keys.BEST_SECTOR_SCORE] ?: 0,
            bestEndlessScore = prefs[Keys.BEST_ENDLESS_SCORE] ?: 0,
            bombCount = prefs[Keys.BOMB_COUNT] ?: 3,
            lightningCount = prefs[Keys.LIGHTNING_COUNT] ?: 3,
            wildCount = prefs[Keys.WILD_COUNT] ?: 3
        )
    }

    suspend fun toggleSound() {
        context.dataStore.edit { prefs ->
            prefs[Keys.SOUND_ENABLED] = !(prefs[Keys.SOUND_ENABLED] ?: true)
        }
    }

    suspend fun toggleHaptics() {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAPTICS_ENABLED] = !(prefs[Keys.HAPTICS_ENABLED] ?: true)
        }
    }

    suspend fun toggleColorblind() {
        context.dataStore.edit { prefs ->
            prefs[Keys.COLORBLIND_ENABLED] = !(prefs[Keys.COLORBLIND_ENABLED] ?: false)
        }
    }

    suspend fun consumeBooster(kind: BoosterKind): Boolean {
        var consumed = false
        context.dataStore.edit { prefs ->
            val key = boosterKey(kind)
            val current = prefs[key] ?: 3
            if (current > 0) {
                prefs[key] = current - 1
                consumed = true
            }
        }
        return consumed
    }

    suspend fun grantBooster(kind: BoosterKind, amount: Int = 1) {
        context.dataStore.edit { prefs ->
            val key = boosterKey(kind)
            val current = prefs[key] ?: 3
            prefs[key] = (current + amount).coerceAtMost(99)
        }
    }

    suspend fun recordSectorClear(
        sector: Int,
        starsEarned: Int,
        score: Int,
        rewardBooster: BoosterKind?
    ) {
        context.dataStore.edit { prefs ->
            val currentMap = decodeStars(prefs[Keys.SECTOR_STARS]).toMutableMap()
            val prevStars = currentMap[sector] ?: 0
            if (starsEarned > prevStars) {
                currentMap[sector] = starsEarned.coerceIn(1, 3)
                prefs[Keys.SECTOR_STARS] = encodeStars(currentMap)
            }
            val currentHighest = prefs[Keys.HIGHEST_SECTOR] ?: 1
            if (sector >= currentHighest && sector < PlayerProgress.TOTAL_SECTORS) {
                prefs[Keys.HIGHEST_SECTOR] = sector + 1
            }
            val prevBest = prefs[Keys.BEST_SECTOR_SCORE] ?: 0
            if (score > prevBest) {
                prefs[Keys.BEST_SECTOR_SCORE] = score
            }
            if (rewardBooster != null) {
                val bKey = boosterKey(rewardBooster)
                prefs[bKey] = ((prefs[bKey] ?: 3) + 1).coerceAtMost(99)
            }
        }
    }

    suspend fun recordEndlessScore(score: Int) {
        context.dataStore.edit { prefs ->
            val prevBest = prefs[Keys.BEST_ENDLESS_SCORE] ?: 0
            if (score > prevBest) {
                prefs[Keys.BEST_ENDLESS_SCORE] = score
            }
        }
    }

    private fun boosterKey(kind: BoosterKind) = when (kind) {
        BoosterKind.BOMB -> Keys.BOMB_COUNT
        BoosterKind.LIGHTNING -> Keys.LIGHTNING_COUNT
        BoosterKind.WILD -> Keys.WILD_COUNT
    }

    private fun decodeStars(raw: String?): Map<Int, Int> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val sector = parts[0].toIntOrNull()
                val stars = parts[1].toIntOrNull()
                if (sector != null && stars != null) sector to stars.coerceIn(0, 3) else null
            } else {
                null
            }
        }.toMap()
    }

    private fun encodeStars(stars: Map<Int, Int>): String =
        stars.entries.sortedBy { it.key }.joinToString(",") { "${it.key}:${it.value}" }
}
