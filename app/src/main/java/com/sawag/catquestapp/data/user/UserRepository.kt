//package com.sawag.catquestapp.data.user
//
//import kotlinx.coroutines.flow.Flow
//
//class UserRepository(private val userDao: UserDao) {
//
//    fun getUser(id: Int = 1): Flow<UserEntity?> {
//        return userDao.getUser(id)
//    }
//
//    suspend fun upsertUser(userEntity: UserEntity) {
//        userDao.upsertUser(userEntity)
//    }
//
//    // UserDaoのメソッド名変更と引数変更に対応
//    suspend fun levelUpUser(
//        newLevel: Int,
//        newXp: Long,
//        newNextLevelXp: Long,
//        newHp: Int,
//        newMaxHp: Int,
//        newMp: Int,
//        newMaxMp: Int,
//        newAtk: Int,
//        newDef: Int,
//        userId: Int = 1 // idも渡すように変更
//    ) {
//        userDao.updateStatsAfterLevelUp(
//            id = userId, // idを指定
//            newLevel = newLevel,
//            newXp = newXp,
//            newNextLevelXp = newNextLevelXp,
//            newHp = newHp, // 全回復したHP
//            newMaxHp = newMaxHp,
//            newMp = newMp,   // 全回復したMP
//            newMaxMp = newMaxMp,
//            newAtk = newAtk,
//            newDef = newDef
//        )
//    }
//
//    // UserDaoのメソッド名変更に対応
//    suspend fun updateXp(newXp: Long, userId: Int = 1) {
//        userDao.updateXp(id = userId, newXp = newXp)
//    }
//
//    // UserDaoのメソッド名変更に対応
//    suspend fun updateHp(newHp: Int, userId: Int = 1) {
//        userDao.updateHp(id = userId, newHp = newHp)
//    }
//
//    // MP更新用のメソッドを追加
//    suspend fun updateMp(newMp: Int, userId: Int = 1) {
//        userDao.updateMp(id = userId, newMp = newMp)
//    }
//}

package com.sawag.catquestapp.data.user

import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {

    suspend fun addUser(user: UserEntity) {
        userDao.insertUser(user)
    }

    fun getUser(userId: Int): Flow<UserEntity?> {
        return userDao.getUserById(userId)
    }

    fun getAllUsers(): Flow<List<UserEntity>> {
        return userDao.getAllUsers()
    }
}