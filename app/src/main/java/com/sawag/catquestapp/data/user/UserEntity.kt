//package com.sawag.catquestapp.data.user
//
//import androidx.room.Entity
//import androidx.room.PrimaryKey
//
//@Entity(tableName = "user_table")
//data class UserEntity(
//    @PrimaryKey(autoGenerate = true)
//    val id: Int = 0,
//    var name: String = "プレイヤー",
//    var level: Int = 1,
//    var xp: Long = 0, // experiencePoints から xp に変更
//    var nextLevelXp: Long = 100, // nextLevelExperiencePoints から nextLevelXp に変更
//    var hp: Int = 100, // hitPoints から hp に変更
//    var maxHp: Int = 100, // maxHitPoints から maxHp に変更
//    var mp: Int = 50,  // mp を追加 (初期値は仮)
//    var maxMp: Int = 50, // maxMp を追加 (初期値は仮)
//    var attackPower: Int = 10,
//    var defensePower: Int = 5,
//    var gold: Int = 0
//)

package com.sawag.catquestapp.data.user

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_table")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // 主キー、自動生成
    val name: String
)
