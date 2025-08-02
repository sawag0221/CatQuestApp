package com.sawag.catquestapp.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken // Monster用
import com.sawag.catquestapp.data.monster.MonsterDao
import com.sawag.catquestapp.data.monster.MonsterEntity
import com.sawag.catquestapp.data.user.UserDao
import com.sawag.catquestapp.data.user.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import java.io.InputStreamReader // Monster用

// --- データベースのバージョン ---
private const val CURRENT_DATABASE_VERSION = 4    // ★★★ バージョンを4に設定 ★★★

@Database(
    entities = [UserEntity::class, MonsterEntity::class], // MonsterEntityも既存なら含める
    version = CURRENT_DATABASE_VERSION,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun monsterDao(): MonsterDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private lateinit var applicationContextInternal: Context

        private const val TAG = "AppDatabase"
        private const val PREFS_NAME = "db_population_prefs"
        private fun getMonsterDataPopulatedKey(version: Int) =
            "monster_data_populated_for_v_$version"

        private fun getUserDataPopulatedKey(version: Int) =
            "user_data_populated_for_v_$version" // ★ User用フラグキー

        // --- マイグレーション定義 ---
        val MIGRATION_1_2 = object : Migration(1, 2) { /* ... (既存のまま) ... */
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i("MIGRATION_1_2", "Starting migration from version 1 to 2.")
                db.execSQL("DELETE FROM monster_table")
                Log.i("MIGRATION_1_2", "Cleared all data from MonsterEntity table.")
                if (::applicationContextInternal.isInitialized) {
                    val prefs = applicationContextInternal.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit().remove(getMonsterDataPopulatedKey(2)).apply()
                    Log.i("MIGRATION_1_2", "Cleared monster data populated flag for new version 2.")
                } else {
                    Log.e("MIGRATION_1_2", "ApplicationContextInternal not initialized. Cannot clear SharedPreferences flag.")
                }
                Log.i("MIGRATION_1_2", "Finished migration from version 1 to 2.")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) { /* ... (既存のまま) ... */
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i("MIGRATION_2_3", "Starting migration from version 2 to 3.")
                db.execSQL("DELETE FROM monster_table")
                Log.i("MIGRATION_2_3", "Cleared all data from monster_table for repopulation in version 3.")
                if (::applicationContextInternal.isInitialized) {
                    val prefs = applicationContextInternal.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit().remove(getMonsterDataPopulatedKey(3)).apply()
                    Log.i("MIGRATION_2_3", "Cleared monster data populated flag for new version 3.")
                } else {
                    Log.e("MIGRATION_2_3", "ApplicationContextInternal not initialized. Cannot clear SharedPreferences flag for version 3.")
                }
                Log.i("MIGRATION_2_3", "Finished migration from version 2 to 3.")
            }
        }

        // ★★★ バージョン 3 から 4 へのマイグレーション ★★★
// (user_table に level, breed, xp, cat_coins カラム追加)
// 前提: バージョン3のuser_tableはidとnameのみ
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i("MIGRATION_3_4", "Starting migration from version 3 to 4 for user_table.")
                // user_table に level カラムを追加 (INTEGER, NOT NULL, DEFAULT 1)
                db.execSQL("ALTER TABLE user_table ADD COLUMN level INTEGER NOT NULL DEFAULT 1")
                // user_table に breed カラムを追加 (TEXT, NOT NULL, DEFAULT 'null')
                db.execSQL("ALTER TABLE user_table ADD COLUMN breed TEXT NOT NULL DEFAULT 'null'")
                // user_table に xp カラムを追加 (INTEGER, NOT NULL, DEFAULT 0)
                db.execSQL("ALTER TABLE user_table ADD COLUMN xp INTEGER NOT NULL DEFAULT 0")
                // user_table に cat_coins カラムを追加 (INTEGER, NOT NULL, DEFAULT 0)
                db.execSQL("ALTER TABLE user_table ADD COLUMN cat_coins INTEGER NOT NULL DEFAULT 0")
                Log.i(
                    "MIGRATION_3_4",
                    "Added 'level', 'breed', 'xp', and 'cat_coins' columns to user_table."
                )

                // モンスターテーブルに関する処理 (もしバージョン3->4でモンスターに変更があれば)
                // 例: モンスターテーブルをクリアして再投入フラグをリセットする場合
                // db.execSQL("DELETE FROM monster_table")
                // Log.i("MIGRATION_3_4", "Cleared monster_table for repopulation in version 4.")
                // if (::applicationContextInternal.isInitialized) {
                //     val prefs = applicationContextInternal.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                //     prefs.edit().remove(getMonsterDataPopulatedKey(4)).apply()
                //     Log.i("MIGRATION_3_4", "Cleared monster data populated flag for new version 4.")
                // }
                Log.i("MIGRATION_3_4", "Finished migration from version 3 to 4.")
            }
        }

        fun getDatabase(context: Context, applicationScope: CoroutineScope): AppDatabase {
            if (!::applicationContextInternal.isInitialized) {
                applicationContextInternal = context.applicationContext
                Log.d(TAG, "ApplicationContext initialized in getDatabase.")
            }
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cat_quest_database"
                )
                    .addCallback(
                        AppDatabaseCallback(
                            applicationContextInternal,
                            applicationScope,
                            CURRENT_DATABASE_VERSION
                        )
                    )
                    // ★★★ 全てのマイグレーションパスを登録 ★★★
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .setQueryCallback(
                        object : QueryCallback {
                            override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
                                Log.v("RoomQuery", "SQL Query: $sqlQuery SQL Args: $bindArgs")
                            }
                        },
                        Dispatchers.IO.asExecutor()
                    )
                    // .fallbackToDestructiveMigration() // マイグレーションテスト時はコメントアウト推奨
                    .build()
                    .also {
                        INSTANCE = it
                        Log.d(
                            TAG,
                            "Database instance created/opened with version $CURRENT_DATABASE_VERSION."
                        )
                    }
            }
        }
    }

    // AppDatabaseCallback の内容は前回提示したものから変更なしでOK
// populateInitialUserIfNeeded は UserEntity のデフォルト値を使って全カラムを持つユーザーを作成する
    private class AppDatabaseCallback(
        private val context: Context,
        private val scope: CoroutineScope,
        private val currentAppDbVersion: Int // これはビルド時のDBバージョン (4)
    ) : RoomDatabase.Callback() {

        private val TAG_CALLBACK = "AppDatabaseCallback"

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // DBが新規作成される (バージョン4で)
            Log.d(
                TAG_CALLBACK,
                "onCreate CALLED! Database version on disk: ${db.version}. App expects: $currentAppDbVersion. Populating initial data."
            )
            INSTANCE?.let { database ->
                scope.launch {
                    populateInitialUserIfNeeded(database.userDao(), "onCreate (DB v${db.version})")

                    // モンスターデータの初期投入 (バージョン4で初めて投入する場合)
                    if (db.version == currentAppDbVersion && !getMonstersPopulatedFlag(
                            context,
                            currentAppDbVersion
                        )
                    ) {
                        populateMonstersFromJson(
                            context,
                            database.monsterDao(),
                            "onCreate (DB v${db.version})"
                        )
                        setMonstersPopulatedFlag(context, db.version, true)
                    }
                }
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            Log.d(
                TAG_CALLBACK,
                "onOpen CALLED! Database version on disk: ${db.version}. App expects DB version: $currentAppDbVersion"
            )
            INSTANCE?.let { database ->
                scope.launch {
                    // モンスターデータの再投入ロジック (既存のままで良い場合が多い)
                    if (db.version == currentAppDbVersion && !getMonstersPopulatedFlag(
                            context,
                            currentAppDbVersion
                        )
                    ) {
                        Log.i(
                            TAG_CALLBACK,
                            "onOpen: Monster data for v$currentAppDbVersion not populated. (Re)populating."
                        )
                        populateMonstersFromJson(
                            context,
                            database.monsterDao(),
                            "onOpen (v$currentAppDbVersion - repopulate)"
                        )
                        setMonstersPopulatedFlag(context, currentAppDbVersion, true)
                    } else if (getMonstersPopulatedFlag(context, db.version)) {
                        Log.i(
                            TAG_CALLBACK,
                            "onOpen: Monster data for v${db.version} already populated."
                        )
                    }
                }
            }
        }

        private suspend fun populateInitialUserIfNeeded(userDao: UserDao, trigger: String) {
            val userCount = userDao.getUserCount()
            if (userCount == 0) {
                Log.i(
                    TAG_CALLBACK,
                    "[$trigger] No users found in user_table. Populating initial user."
                )
                try {
                    userDao.insertUser(UserEntity()) // 全カラムのデフォルト値が入る
                    Log.i(
                        TAG_CALLBACK,
                        "[$trigger] Initial user populated successfully with all columns for v$currentAppDbVersion."
                    )
                } catch (e: Exception) {
                    Log.e(TAG_CALLBACK, "[$trigger] Error populating initial user data", e)
                }
            } else {
                Log.i(
                    TAG_CALLBACK,
                    "[$trigger] User(s) already exist in user_table (count: $userCount). No initial user needed."
                )
            }
        }

        // (populateMonstersFromJson, setMonstersPopulatedFlag, getMonstersPopulatedFlag は変更なし)
        // ... (これらの関数は変更なしなので省略) ...
        private suspend fun populateMonstersFromJson(
            context: Context,
            monsterDao: MonsterDao,
            trigger: String
        ) {
            Log.d(TAG_CALLBACK, "[$trigger] Attempting to populate monsters from JSON.")
            try {
                val gson = Gson()
                context.assets.open("monsters.json").use { inputStream ->
                    InputStreamReader(inputStream).use { reader ->
                        val monsterListType = object : TypeToken<List<MonsterEntity>>() {}.type
                        val monsters: List<MonsterEntity> = gson.fromJson(reader, monsterListType)
                        if (monsters.isNotEmpty()) {
                            monsterDao.insertAllMonsters(monsters)
                            Log.i(
                                TAG_CALLBACK,
                                "[$trigger] Successfully populated ${monsters.size} monsters from JSON."
                            )
                        } else {
                            Log.w(
                                TAG_CALLBACK,
                                "[$trigger] No monsters found in JSON or JSON is empty."
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG_CALLBACK, "[$trigger] Error populating monsters from JSON", e)
            }
        }

        private fun setMonstersPopulatedFlag(context: Context, version: Int, populated: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(getMonsterDataPopulatedKey(version), populated).apply()
            Log.d(TAG_CALLBACK, "Monster populated flag for v$version set to: $populated")
        }

        private fun getMonstersPopulatedFlag(context: Context, version: Int): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val flag = prefs.getBoolean(getMonsterDataPopulatedKey(version), false)
            Log.d(TAG_CALLBACK, "Monster populated flag for v$version read as: $flag")
            return flag
        }
    }
}
