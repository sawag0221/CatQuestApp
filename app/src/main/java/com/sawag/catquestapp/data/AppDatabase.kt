package com.sawag.catquestapp.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sawag.catquestapp.data.monster.MonsterDao
import com.sawag.catquestapp.data.monster.MonsterEntity
import com.sawag.catquestapp.data.user.UserDao
import com.sawag.catquestapp.data.user.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import java.io.InputStreamReader

// --- バージョン管理 ---
private const val CURRENT_DATABASE_VERSION = 6 // ★ 今回上げる新しいバージョン (例: 5から6へ)
private const val PREVIOUS_VERSION_FOR_MONSTER_REPOPULATION = 5

@Database(
    entities = [UserEntity::class, MonsterEntity::class],
    version = CURRENT_DATABASE_VERSION,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun monsterDao(): MonsterDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private lateinit var applicationContextInternal: Context // ★ Contextを保持するプロパティ

        private const val TAG = "AppDatabase"
        private const val PREFS_NAME = "db_population_prefs"
        private fun getMonsterDataPopulatedKey(version: Int) = "monster_data_populated_for_v_$version"

        fun getDatabase(context: Context, applicationScope: CoroutineScope): AppDatabase {
            // ★ getDatabaseが呼ばれる際にContextを保持する
            // synchronizedブロックの外で初期化することで、不要なロック範囲を減らす
            if (!::applicationContextInternal.isInitialized) {
                applicationContextInternal = context.applicationContext
                Log.d(TAG, "ApplicationContext initialized in getDatabase.")
            }
            return INSTANCE ?: synchronized(this) {
                // ダブルチェックロッキングパターン
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, // ここは builder に渡す Context なので元のままでOK
                    AppDatabase::class.java,
                    "cat_quest_database"
                )
                    .addCallback(AppDatabaseCallback(applicationContextInternal, applicationScope)) // ★ Callback にも保持した Context を渡す
                    .addMigrations(MIGRATION_CLEAR_MONSTERS)
                    .setQueryCallback(
                        object : QueryCallback {
                            override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
                                Log.v("RoomQuery", "SQL Query: $sqlQuery SQL Args: $bindArgs")
                            }
                        },
                        Dispatchers.IO.asExecutor()
                    )
                    .build()
                    .also {
                        INSTANCE = it
                        Log.d(TAG, "Database instance created.")
                    }
            }
        }

        val MIGRATION_CLEAR_MONSTERS =
            object : Migration(PREVIOUS_VERSION_FOR_MONSTER_REPOPULATION, CURRENT_DATABASE_VERSION) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    Log.i(
                        TAG,
                        "MIGRATION_CLEAR_MONSTERS: Migrating from v$PREVIOUS_VERSION_FOR_MONSTER_REPOPULATION to v$CURRENT_DATABASE_VERSION. Clearing monster_table."
                    )
                    db.execSQL("DELETE FROM monster_table")
                    Log.i(TAG, "MIGRATION_CLEAR_MONSTERS: Cleared all data from monster_table.")

                    if (::applicationContextInternal.isInitialized) {
                        val prefs = applicationContextInternal.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        prefs.edit().remove(getMonsterDataPopulatedKey(CURRENT_DATABASE_VERSION)).apply()
                        Log.i(TAG, "MIGRATION_CLEAR_MONSTERS: Reset monster population flag for v$CURRENT_DATABASE_VERSION.")
                    } else {
                        Log.e(TAG, "MIGRATION_CLEAR_MONSTERS: ApplicationContext not initialized. Cannot reset population flag.")
                    }
                }
            }
    }

    private class AppDatabaseCallback(
        private val context: Context, // ★ ここは applicationContextInternal を受け取る
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        private val TAG_CALLBACK = "AppDatabaseCallback"

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            Log.d(TAG_CALLBACK, "onCreate CALLED! Database version: ${db.version}. Populating monsters from JSON for new DB.")
            INSTANCE?.let { database ->
                scope.launch {
                    populateMonstersFromJson(context, database.monsterDao(), "onCreate (DB v${db.version})")
                    setMonstersPopulatedFlag(context, db.version, true)
                }
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            Log.d(TAG_CALLBACK, "onOpen CALLED! Database version: ${db.version}")
            INSTANCE?.let { database ->
                val populatedAlreadyForThisVersion = getMonstersPopulatedFlag(context, CURRENT_DATABASE_VERSION)

                if (db.version == CURRENT_DATABASE_VERSION && !populatedAlreadyForThisVersion) {
                    Log.i(
                        TAG_CALLBACK,
                        "onOpen: DB is v$CURRENT_DATABASE_VERSION and not yet populated. Repopulating monster_table from JSON."
                    )
                    scope.launch {
                        populateMonstersFromJson(context, database.monsterDao(), "onOpen (after migration to v$CURRENT_DATABASE_VERSION)")
                        setMonstersPopulatedFlag(context, CURRENT_DATABASE_VERSION, true)
                    }
                } else if (db.version == CURRENT_DATABASE_VERSION && populatedAlreadyForThisVersion) {
                    Log.i(TAG_CALLBACK, "onOpen: Monster data for v$CURRENT_DATABASE_VERSION already populated. No action needed.")
                } else if (db.version < CURRENT_DATABASE_VERSION) {
                    Log.w(TAG_CALLBACK, "onOpen: Opened DB with older version ${db.version}. Expecting migration to v$CURRENT_DATABASE_VERSION.")
                } else {
                    Log.d(TAG_CALLBACK, "onOpen: DB version is ${db.version}. No specific action for monster repopulation needed for this version based on current logic.")
                }
            }
        }

        private suspend fun populateMonstersFromJson(context: Context, monsterDao: MonsterDao, trigger: String) {
            Log.d(TAG_CALLBACK, "[$trigger] Attempting to populate monsters from JSON.")
            try {
                val gson = Gson()
                // ★ Callbackに渡されたContextを使用
                context.assets.open("monsters.json").use { inputStream ->
                    InputStreamReader(inputStream).use { reader ->
                        val monsterListType = object : TypeToken<List<MonsterEntity>>() {}.type
                        val monsters: List<MonsterEntity> = gson.fromJson(reader, monsterListType)

                        if (monsters.isNotEmpty()) {
                            monsterDao.insertAllMonsters(monsters)
                            Log.i(TAG_CALLBACK, "[$trigger] Successfully populated ${monsters.size} monsters from JSON.")
                        } else {
                            Log.w(TAG_CALLBACK, "[$trigger] No monsters found in JSON or JSON is empty.")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG_CALLBACK, "[$trigger] Error populating monsters from JSON", e)
            }
        }

        private fun setMonstersPopulatedFlag(context: Context, version: Int, populated: Boolean) {
            // ★ Callbackに渡されたContextを使用
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(getMonsterDataPopulatedKey(version), populated).apply()
            Log.d(TAG_CALLBACK, "Monster populated flag for v$version set to: $populated")
        }

        private fun getMonstersPopulatedFlag(context: Context, version: Int): Boolean {
            // ★ Callbackに渡されたContextを使用
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(getMonsterDataPopulatedKey(version), false)
        }
    }
}




//package com.sawag.catquestapp.data
//
//import android.content.Context
//import android.util.Log
//import androidx.room.Database
//import androidx.room.Room
//import androidx.room.RoomDatabase
//import androidx.sqlite.db.SupportSQLiteDatabase
//import com.sawag.catquestapp.data.monster.MonsterDao
//import com.sawag.catquestapp.data.monster.MonsterEntity
//import com.sawag.catquestapp.data.monster.MonsterListContainer
//// import androidx.sqlite.db.SupportSQLiteDatabase // Callback を使わないので不要
//import com.sawag.catquestapp.data.user.UserDao // ★ UserDao をインポート
//import com.sawag.catquestapp.data.user.UserEntity // ★ UserEntity をインポート
//import kotlinx.coroutines.CoroutineScope
//// import kotlinx.coroutines.CoroutineScope // getDatabase の引数から削除する場合は不要
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.SupervisorJob
//import kotlinx.coroutines.asExecutor
//import kotlinx.coroutines.launch
//import kotlinx.serialization.json.Json
//
//@Database(
//    entities = [UserEntity::class, MonsterEntity::class], // UserEntity のみ
//    version = 4,                   // ★ 初期バージョン
//    exportSchema = false
//)
//abstract class AppDatabase : RoomDatabase() {
//
//    abstract fun userDao(): UserDao
//    abstract fun monsterDao(): MonsterDao
//
//    companion object {
//        @Volatile
//        private var INSTANCE: AppDatabase? = null
//
//        // アプリケーションのコルーチン
//        private var applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
//
//        // ★ scope 引数を削除し、よりシンプルに
//        fun getDatabase(context: Context): AppDatabase {
//            return INSTANCE ?: synchronized(this) {
//                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
//            }
//        }
//
//        // ★ scope 引数を削除
//        private fun buildDatabase(context: Context): AppDatabase {
//            return Room.databaseBuilder(
//                context.applicationContext,
//                AppDatabase::class.java,
//                "cat_quest_database"
//            )
//                .addCallback(AppDatabaseCallback(context, applicationScope))
//                .fallbackToDestructiveMigration() // バージョン変更でDB再作成を許可 (開発中は便利)
//                .setQueryCallback( // クエリログはデバッグに役立つので残す
//                    object : RoomDatabase.QueryCallback {
//                        override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
//                            Log.v("RoomQuery", "SQL Query: $sqlQuery SQL Args: $bindArgs")
//                        }
//                    },
//                    Dispatchers.IO.asExecutor()
//                )
//                .build()
//        }
//    }
//
//    // ★ RoomDatabase.Callback を継承したクラスを定義
//    private class AppDatabaseCallback(
//        private val context: Context,
//        private val scope: CoroutineScope
//    ) : RoomDatabase.Callback() {
//
//        override fun onCreate(db: SupportSQLiteDatabase) {
//            super.onCreate(db)
//            Log.d("AppDatabaseCallback", "onCreate CALLED! Database version: ${db.version}") // ★ ログ
//            INSTANCE?.let { database ->
//                Log.d("AppDatabaseCallback", "INSTANCE is available in onCreate. Populating monsters...") // ★ ログ
//                scope.launch {
//                    populateMonsters(context, database.monsterDao())
//                }
//            } ?: Log.e("AppDatabaseCallback", "INSTANCE IS NULL in onCreate! Cannot populate monsters.") // ★ ログ
//        }
//
//        suspend fun populateMonsters(context: Context, monsterDao: MonsterDao) {
//            try {
//                val jsonString: String
//                context.assets.open("monsters.json").use { inputStream ->
//                    jsonString = inputStream.bufferedReader().use { it.readText() }
//                }
//
//                // ignoreUnknownKeys = true を設定しておくと
//                // JSONにデータクラスにないフィールドがあってもエラーにならない
//                val monsterData = Json { ignoreUnknownKeys = true }
//                    .decodeFromString<MonsterListContainer>(jsonString)
//
//                monsterDao.insertAllMonsters(monsterData.monsters)
//                Log.i("AppDatabaseCallback", "${monsterData.monsters.size} monsters populated.")
//            } catch (e: Exception) {
//                Log.e("AppDatabaseCallback", "Error populating monsters from JSON", e)
//            }
//        }
//    }
//}
//
