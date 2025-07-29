package com.sawag.catquestapp.data.monster

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MonsterDao {

    /**
     * 全てのモンスターをID昇順で取得します。
     * Flowを使用しているため、データが変更されると自動的に新しいリストが通知されます。
     */
    @Query("SELECT * FROM monster_table ORDER BY id ASC")
    fun getAllMonsters(): Flow<List<MonsterEntity>>

    /**
     * 指定されたIDのモンスターを1体取得します。
     * Flowを使用しているため、データが変更されると自動的に新しいエンティティが通知されます。
     * @param monsterId 取得するモンスターのID
     * @return 指定されたIDのモンスター。見つからない場合はnull。
     */
    @Query("SELECT * FROM monster_table WHERE id = :monsterId")
    fun getMonsterById(monsterId: Int): Flow<MonsterEntity?>

    /**
     * モンスターを1体データベースに挿入します。
     * 既に同じIDのモンスターが存在する場合は、新しいデータで置き換えられます。
     * @param monster 挿入するモンスターエンティティ
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonster(monster: MonsterEntity)

    /**
     * 複数のモンスターを一度にデータベースに挿入します。
     * 既に同じIDのモンスターが存在する場合は、新しいデータで置き換えられます。
     * JSONから初期データを読み込む際などに使用します。
     * @param monsters 挿入するモンスターエンティティのリスト
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMonsters(monsters: List<MonsterEntity>)

    // --- 必要に応じて以下のメソッドのコメントを解除して実装してください ---

    // /**
    //  * モンスターの情報を更新します。
    //  * @param monster 更新するモンスターエンティティ
    //  */
    // @Update
    // suspend fun updateMonster(monster: MonsterEntity)

    // /**
    //  * 指定されたIDのモンスターを削除します。
    //  * @param monsterId 削除するモンスターのID
    //  */
    // @Query("DELETE FROM monster_table WHERE id = :monsterId")
    // suspend fun deleteMonsterById(monsterId: Int)

    // /**
    //  * 全てのモンスターを削除します。
    //  */
    // @Query("DELETE FROM monster_table")
    // suspend fun deleteAllMonsters()
}
