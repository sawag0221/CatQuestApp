package com.sawag.catquestapp.data.monster // MonsterEntityと同じ場所で良いでしょう

import kotlinx.serialization.Serializable

@Serializable
data class MonsterListContainer(
    val version: Int,
    val monsters: List<MonsterEntity> // JSONの "monsters" 配列に対応
)
