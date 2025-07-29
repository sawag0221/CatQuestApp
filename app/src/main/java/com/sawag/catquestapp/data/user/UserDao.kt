//package com.sawag.catquestapp.data.user
//
//import androidx.room.Dao
//import androidx.room.Insert
//import androidx.room.OnConflictStrategy
//import androidx.room.Query
//import com.sawag.catquestapp.data.user.UserEntity
//import kotlinx.coroutines.flow.Flow
//
//@Dao
//interface UserDao {
//
//
//    @Query("SELECT COUNT(*) FROM user_table") // user_table は実際のテーブル名に合わせる
//    suspend fun getUserCount(): Int
//
//    @Query("SELECT * FROM user_table WHERE id = :id")
//    fun getUser(id: Int = 1): Flow<UserEntity?>
//
//    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
//    suspend fun upsertUser(userEntity: UserEntity)
//
//    // MPとMaxMPの更新もクエリに追加
//    @Query("UPDATE user_table SET level = :newLevel, xp = :newXp, nextLevelXp = :newNextLevelXp, hp = :newHp, maxHp = :newMaxHp, mp = :newMp, maxMp = :newMaxMp, attackPower = :newAtk, defensePower = :newDef WHERE id = :id")
//    suspend fun updateStatsAfterLevelUp(
//        id: Int = 1,
//        newLevel: Int,
//        newXp: Long,
//        newNextLevelXp: Long,
//        newHp: Int, // レベルアップ時に全回復したHP
//        newMaxHp: Int,
//        newMp: Int,  // レベルアップ時に全回復したMP
//        newMaxMp: Int,
//        newAtk: Int,
//        newDef: Int
//    )
//
//    // xp だけを更新する
//    @Query("UPDATE user_table SET xp = :newXp WHERE id = :id") // experiencePoints から xp に変更
//    suspend fun updateXp(id: Int = 1, newXp: Long) // メソッド名も updateExperiencePoints から updateXp に変更
//
//    // hp だけを更新する
//    @Query("UPDATE user_table SET hp = :newHp WHERE id = :id") // hitPoints から hp に変更
//    suspend fun updateHp(id: Int = 1, newHp: Int) // メソッド名も updateHitPoints から updateHp に変更
//
//    // mp だけを更新するメソッドも追加 (必要に応じて)
//    @Query("UPDATE user_table SET mp = :newMp WHERE id = :id")
//    suspend fun updateMp(id: Int = 1, newMp: Int)
//}

//package com.sawag.catquestapp.data.user
//
//import androidx.room.Dao
//import androidx.room.Insert
//import androidx.room.OnConflictStrategy
//import androidx.room.Query
//import androidx.room.Update
//import kotlinx.coroutines.flow.Flow
//
//@Dao
//interface UserDao {
//
//    // ユーザーを挿入または更新 (主キーが同じ場合は更新)
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun upsertUser(user: UserEntity)
//
//    // IDに基づいてユーザーを1件取得 (Flowで監視)
//    @Query("SELECT * FROM user_table WHERE id = :id")
//    fun getUserById(id: Int): Flow<UserEntity?> // 存在しない場合もあるのでnull許容
//
//    // 全ユーザーを取得 (Flowで監視) - 主にデバッグ用
//    @Query("SELECT * FROM user_table")
//    fun getAllUsers(): Flow<List<UserEntity>>
//
//    // ★ シンプル化のため、他のメソッド (levelUpUser, updateXp, updateHp, updateMpなど) は一旦コメントアウトまたは削除します
//    // 必要であれば、後で新しいシンプルなUserEntityに合わせて再実装します。
//}

// UserDao.kt
package com.sawag.catquestapp.data.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sawag.catquestapp.data.user.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM user_table WHERE id = :userId")
    fun getUserById(userId: Int): Flow<UserEntity?>

    @Query("SELECT * FROM user_table ORDER BY name ASC")
    fun getAllUsers(): Flow<List<UserEntity>>
}