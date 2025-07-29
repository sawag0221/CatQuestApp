//package com.sawag.catquestapp.data.monster // または適切なパッケージ
//
//import androidx.room.Entity
//import androidx.room.PrimaryKey
//import kotlinx.serialization.Serializable // kotlinx.serialization を使う場合
//
//@Serializable // JSONからのデシリアライズのため
//@Entity(tableName = "monster_table")
//data class MonsterEntity(
//    @PrimaryKey(autoGenerate = false)
//    val id: Int,
//    val name: String,
//    val type: String, // ★ 追加: モンスターのタイプ (例: "ねこ", "ねずみ")
//    val imageResName: String,
//    val hp: Int,
//    val attack: Int,
//    val defense: Int,
//    val experience: Int,
//    val gold: Int,
//    val description: String
//)
