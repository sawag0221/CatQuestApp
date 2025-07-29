package com.sawag.catquestapp

object ExperienceTable {
    // レベル -> そのレベルになるための必要"累積"経験値
    private val levelToExp: Map<Int, Int> = mapOf(
        1 to 0,
        2 to 20,
        3 to 50,
        4 to 100,
        5 to 180,
        // ... さらに多くのレベルを定義
    )

    fun getExperienceForLevel(level: Int): Int {
        return levelToExp[level] ?: Int.MAX_VALUE // 定義外のレベルは非常に大きな値
    }

    fun getMaxLevel(): Int {
        return levelToExp.keys.maxOrNull() ?: 1
    }
}

object PlayerStatsGrowth {
    // レベルごとのステータス上昇値や基本値を定義
    // 例: data class LevelUpBonus(val hpBonus: Int, val attackBonus: Int, val defenseBonus: Int)
    // fun getBonusForLevel(level: Int): LevelUpBonus { ... }

    // または、レベルに応じた基本ステータスを返す形でも良い
    fun getMaxHpForLevel(level: Int): Int = 40 + (level * 10)
    fun getAttackPowerForLevel(level: Int): Int = 8 + (level * 2)
    fun getDefensePowerForLevel(level: Int): Int = 4 + (level * 1)
}