package com.sawag.catquestapp.data.user

import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {

    // ユーザーを挿入または更新 (UPSERT)
    suspend fun upsertUser(user: UserEntity) {
        userDao.upsertUser(user) // UserDaoにupsertUserメソッドが必要
    }

    // ユーザーを挿入 (主に初期データ用や、明確に新規として扱いたい場合)
    suspend fun insertUser(user: UserEntity) {
        userDao.insertUser(user)
    }

    // 指定されたIDのユーザーを取得
    fun getUserById(userId: Int): Flow<UserEntity?> { // メソッド名をDaoと合わせる
        return userDao.getUserById(userId)
    }

    // 全てのユーザーを取得
    fun getAllUsers(): Flow<List<UserEntity>> {
        return userDao.getAllUsers()
    }

    // ★ ユーザーの血統を更新するメソッド
    suspend fun updateUserBreed(userId: Int, newBreed: String) {
        userDao.updateUserBreed(userId, newBreed) // UserDaoにこのメソッドが必要
    }

    // ★ ユーザーエンティティ全体を更新するメソッド (汎用的)
    suspend fun updateUser(user: UserEntity) {
        userDao.updateUser(user) // UserDaoにこのメソッドが必要 (Roomの@Updateアノテーション)
    }

    // 必要であれば削除メソッドなども追加
    // suspend fun deleteUser(user: UserEntity) {
    //     userDao.deleteUser(user)
    // }
}

