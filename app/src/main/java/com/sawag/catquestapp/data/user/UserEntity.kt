// UserEntity.kt (再掲 - この状態であることを想定)
package com.sawag.catquestapp.data.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_table")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "management_no")
    val id: Int = 0,

    @ColumnInfo(name = "name", defaultValue = "プレイヤー") // 初期からあったと仮定
    val name: String = "プレイヤー",

    @ColumnInfo(name = "level", defaultValue = "1")      // ★追加するカラム
    val level: Int = 1,

    @ColumnInfo(name = "breed", defaultValue = "'null'")   // ★追加するカラム
    val breed: String = "null",

    @ColumnInfo(name = "xp", defaultValue = "0")         // ★追加するカラム (experiencePointsに対応)
    val experiencePoints: Int = 0,

    @ColumnInfo(name = "cat_coins", defaultValue = "0")  // ★追加するカラム (catCoinsに対応)
    val catCoins: Int = 0
)

