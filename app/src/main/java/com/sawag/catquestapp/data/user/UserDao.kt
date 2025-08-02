package com.sawag.catquestapp.data.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    // ... (既存の insertUser, upsertUser, getUserById, getAllUsers, updateUser はそのまま) ...
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity) // 既存のユーザーがいれば置き換える

    @Query("SELECT * FROM user_table WHERE management_no = :id")
    fun getUserById(id: Int): Flow<UserEntity?>

    @Query("SELECT * FROM user_table")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Update
    suspend fun updateUser(user: UserEntity)


    // ★ 新しいメソッド: 指定されたIDのユーザーの血統を更新する
    @Query("UPDATE user_table SET breed = :newBreed WHERE management_no = :userId")
    suspend fun updateUserBreed(userId: Int, newBreed: String)

    // ★ 新しいメソッド: ユーザー数をカウントする (初期データ投入判定用)
    @Query("SELECT COUNT(*) FROM user_table")
    suspend fun getUserCount(): Int
}
