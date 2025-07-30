package com.sawag.catquestapp.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sawag.catquestapp.data.monster.MonsterDao
import com.sawag.catquestapp.data.monster.MonsterEntity
import com.sawag.catquestapp.data.monster.MonsterListContainer
// import androidx.sqlite.db.SupportSQLiteDatabase // Callback を使わないので不要
import com.sawag.catquestapp.data.user.UserDao // ★ UserDao をインポート
import com.sawag.catquestapp.data.user.UserEntity // ★ UserEntity をインポート
import kotlinx.coroutines.CoroutineScope
// import kotlinx.coroutines.CoroutineScope // getDatabase の引数から削除する場合は不要
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@Database(
    entities = [UserEntity::class, MonsterEntity::class], // UserEntity のみ
    version = 4,                   // ★ 初期バージョン
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun monsterDao(): MonsterDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // アプリケーションのコルーチン
        private var applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        // ★ scope 引数を削除し、よりシンプルに
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        // ★ scope 引数を削除
        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "cat_quest_database"
            )
                .addCallback(AppDatabaseCallback(context, applicationScope))
                .fallbackToDestructiveMigration() // バージョン変更でDB再作成を許可 (開発中は便利)
                .setQueryCallback( // クエリログはデバッグに役立つので残す
                    object : RoomDatabase.QueryCallback {
                        override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
                            Log.v("RoomQuery", "SQL Query: $sqlQuery SQL Args: $bindArgs")
                        }
                    },
                    Dispatchers.IO.asExecutor()
                )
                .build()
        }
    }

    // ★ RoomDatabase.Callback を継承したクラスを定義
    private class AppDatabaseCallback(
        private val context: Context,
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            Log.d("AppDatabaseCallback", "onCreate CALLED! Database version: ${db.version}") // ★ ログ
            INSTANCE?.let { database ->
                Log.d("AppDatabaseCallback", "INSTANCE is available in onCreate. Populating monsters...") // ★ ログ
                scope.launch {
                    populateMonsters(context, database.monsterDao())
                }
            } ?: Log.e("AppDatabaseCallback", "INSTANCE IS NULL in onCreate! Cannot populate monsters.") // ★ ログ
        }

        suspend fun populateMonsters(context: Context, monsterDao: MonsterDao) {
            try {
                val jsonString: String
                context.assets.open("monsters.json").use { inputStream ->
                    jsonString = inputStream.bufferedReader().use { it.readText() }
                }

                // ignoreUnknownKeys = true を設定しておくと
                // JSONにデータクラスにないフィールドがあってもエラーにならない
                val monsterData = Json { ignoreUnknownKeys = true }
                    .decodeFromString<MonsterListContainer>(jsonString)

                monsterDao.insertAllMonsters(monsterData.monsters)
                Log.i("AppDatabaseCallback", "${monsterData.monsters.size} monsters populated.")
            } catch (e: Exception) {
                Log.e("AppDatabaseCallback", "Error populating monsters from JSON", e)
            }
        }
    }
}

