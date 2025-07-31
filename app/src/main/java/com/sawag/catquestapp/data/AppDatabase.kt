package com.sawag.catquestapp.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
// import androidx.room.migration.Migration // ★ 一旦不要なのでコメントアウト
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

// --- データベースの初期バージョン ---
private const val INITIAL_DATABASE_VERSION = 1 // ★★★ バージョンを1に設定 ★★★

@Database(
    entities = [UserEntity::class, MonsterEntity::class],
    version = INITIAL_DATABASE_VERSION,      // ★★★ バージョンを1に設定 ★★★
    exportSchema = true // スキーマのエクスポートは有効にしておくのがおすすめです
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
        private fun getMonsterDataPopulatedKey(version: Int) = "monster_data_populated_for_v_$version"

        // --- マイグレーション定義 ---
        // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
        // ★ ここにあった MIGRATION_6_7 や MIGRATION_7_8 の定義は一旦削除またはコメントアウト ★
        // ★ バージョン1から開始するため、初期状態ではマイグレーションは不要です。        ★
        // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★

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
                    // ★★★ Callbackには初期バージョンを渡すか、Callback内でdb.versionを参照する設計に依存 ★★★
                    .addCallback(AppDatabaseCallback(applicationContextInternal, applicationScope, INITIAL_DATABASE_VERSION))
                    // ★★★ addMigrations() は空にするか、呼び出し自体をコメントアウト ★★★
                    // .addMigrations(
                    //     // MIGRATION_6_7_CLEAR_REPOPULATE_MONSTERS,
                    //     // MIGRATION_7_8_ADD_RARITY_TO_MONSTERS
                    // )
                    .setQueryCallback( // クエリログはデバッグに役立つので残してもOK
                        object : QueryCallback {
                            override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
                                Log.v("RoomQuery", "SQL Query: $sqlQuery SQL Args: $bindArgs")
                            }
                        },
                        Dispatchers.IO.asExecutor()
                    )
                    // ★★★ fallbackToDestructiveMigration() を一時的に追加するのもあり ★★★
                    // .fallbackToDestructiveMigration() // 開発初期や大きな変更時は便利だが、本番では注意
                    .build()
                    .also {
                        INSTANCE = it
                        Log.d(TAG, "Database instance created.")
                    }
            }
        }
    }

    private class AppDatabaseCallback(
        private val context: Context,
        private val scope: CoroutineScope,
        private val currentAppDbVersion: Int // ★ アプリが期待するDBバージョン (ここでは INITIAL_DATABASE_VERSION)
    ) : RoomDatabase.Callback() {

        private val TAG_CALLBACK = "AppDatabaseCallback"

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // onCreate はDBが新規作成されたときに呼ばれる。このときの db.version は INITIAL_DATABASE_VERSION になるはず。
            Log.d(TAG_CALLBACK, "onCreate CALLED! Database version: ${db.version}. Populating initial data.")
            INSTANCE?.let { database ->
                scope.launch {
                    populateMonstersFromJson(context, database.monsterDao(), "onCreate (DB v${db.version})")
                    // 新規作成時は、その作成されたDBのバージョンに対してフラグを立てる
                    setMonstersPopulatedFlag(context, db.version, true)
                }
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            Log.d(TAG_CALLBACK, "onOpen CALLED! Database version: ${db.version}. App expects DB version: $currentAppDbVersion")

            // バージョン1のDBが開かれた場合、onCreateでデータ投入されているはずなので、
            // 基本的には onOpen での追加のデータ投入は不要かもしれない。
            // ただし、何らかの理由でonCreateでの投入が不完全だった場合や、
            // バージョン1でも特定の条件下で再投入したい場合のロジックはここに追加できる。
            INSTANCE?.let { database ->
                if (db.version == currentAppDbVersion && !getMonstersPopulatedFlag(context, currentAppDbVersion)) {
                    Log.i(
                        TAG_CALLBACK,
                        "onOpen: DB is v$currentAppDbVersion and not yet populated (onCreate might have issues or data cleared manually). Repopulating."
                    )
                    scope.launch {
                        populateMonstersFromJson(context, database.monsterDao(), "onOpen (v$currentAppDbVersion - repopulate)")
                        setMonstersPopulatedFlag(context, currentAppDbVersion, true)
                    }
                } else if (getMonstersPopulatedFlag(context, db.version)) {
                    Log.i(TAG_CALLBACK, "onOpen: Data for v${db.version} already populated.")
                } else {
                    Log.d(TAG_CALLBACK, "onOpen: No specific repopulation action for v${db.version}.")
                }
            }
        }

        private suspend fun populateMonstersFromJson(context: Context, monsterDao: MonsterDao, trigger: String) {
            // (populateMonstersFromJson の実装は変更なし)
            // ただし、MonsterEntityが「rarity」カラムを持つ状態であれば、
            // JSONファイルにもrarityがあるか、Entityでデフォルト値が設定されている必要がある点に注意。
            // バージョン1に戻す際に、MonsterEntityからもrarityを一旦削除するなら、この懸念はなくなる。
            Log.d(TAG_CALLBACK, "[$trigger] Attempting to populate monsters from JSON.")
            try {
                val gson = Gson()
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
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(getMonsterDataPopulatedKey(version), populated).apply()
            Log.d(TAG_CALLBACK, "Monster populated flag for v$version set to: $populated")
        }

        private fun getMonstersPopulatedFlag(context: Context, version: Int): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(getMonsterDataPopulatedKey(version), false)
        }
    }
}
