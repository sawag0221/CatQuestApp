//package com.sawag.catquestapp.data.monster // または repository パッケージ
//
//import kotlinx.coroutines.flow.Flow
//
//class MonsterRepository(private val monsterDao: MonsterDao) {
//
//    fun getAllMonsters(): Flow<List<MonsterEntity>> {
//        return monsterDao.getAllMonsters()
//    }
//
//    fun getMonsterById(id: Int): Flow<MonsterEntity?> { // ★ suspendを削除し、戻り値を Flow<MonsterEntity?> に変更
//        return monsterDao.getMonsterById(id)
//    }
//
//    // 必要に応じて他のメソッド
//}