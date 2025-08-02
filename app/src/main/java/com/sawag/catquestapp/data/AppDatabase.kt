package com.sawag.catquestapp.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration // ★ Migration をインポート
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

// --- データベースのバージョン ---
// private const val INITIAL_DATABASE_VERSION = 1 // 古いバージョンはコメントアウトまたは削除
// private const val CURRENT_DATABASE_VERSION = 2    // ★★★ バージョンを2に更新 ★★★ // 古いバージョン
private const val CURRENT_DATABASE_VERSION = 3    // ★★★ バージョンを3に更新 ★★★

@Database(
    entities = [UserEntity::class, MonsterEntity::class],
    version = CURRENT_DATABASE_VERSION,      // ★★★ 更新したバージョン(3)を設定 ★★★
    exportSchema = true // スキーマのエクスポートは有効にしておくのがおすすめです
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun monsterDao(): MonsterDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private lateinit var applicationContextInternal: Context // Contextを保持

        private const val TAG = "AppDatabase"
        private const val PREFS_NAME = "db_population_prefs"
        private fun getMonsterDataPopulatedKey(version: Int) = "monster_data_populated_for_v_$version"

        // --- マイグレーション定義 ---
        // ★ バージョン 1 から 2 へのマイグレーション ★
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i("MIGRATION_1_2", "Starting migration from version 1 to 2.")
                // 1. MonsterEntity テーブルのデータを全て削除
                db.execSQL("DELETE FROM monster_table")
                Log.i("MIGRATION_1_2", "Cleared all data from MonsterEntity table.")

                // 2. 新しいバージョン(2)のデータがまだ投入されていないことを示すために、
                //    対応するSharedPreferencesのフラグを削除(またはfalseに設定)します。
                if (::applicationContextInternal.isInitialized) {
                    val prefs = applicationContextInternal.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit().remove(getMonsterDataPopulatedKey(2)).apply() // 新しいバージョン2のフラグを削除
                    Log.i("MIGRATION_1_2", "Cleared monster data populated flag for new version 2.")
                } else {
                    Log.e("MIGRATION_1_2", "ApplicationContextInternal not initialized. Cannot clear SharedPreferences flag.")
                }
                Log.i("MIGRATION_1_2", "Finished migration from version 1 to 2.")
            }
        }

        // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
        // ★ バージョン 2 から 3 へのマイグレーションを定義 (レコード追加のみの対応) ★
        // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i("MIGRATION_2_3", "Starting migration from version 2 to 3.")
                // 1. MonsterEntity テーブルのデータを全て削除
                //    これにより、新しいJSONファイルの内容で完全に置き換えられる。
                db.execSQL("DELETE FROM monster_table")
                Log.i("MIGRATION_2_3", "Cleared all data from monster_table for repopulation in version 3.")

                // 2. 新しいバージョン(3)のデータがまだ投入されていないことを示すために、
                //    対応するSharedPreferencesのフラグを削除(またはfalseに設定)します。
                //    これにより、AppDatabaseCallbackのonOpenでデータが再投入されます。
                //    applicationContextInternal が初期化されていることを確認
                if (::applicationContextInternal.isInitialized) {
                    val prefs = applicationContextInternal.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    // バージョン3用のフラグをクリア (もし古いバージョンで同じキーを使っていた場合も考慮)
                    prefs.edit().remove(getMonsterDataPopulatedKey(3)).apply()
                    // 必要であれば、以前のバージョン(2)のフラグもクリアする (ディスクスペース節約のため)
                    // prefs.edit().remove(getMonsterDataPopulatedKey(2)).apply()
                    Log.i("MIGRATION_2_3", "Cleared monster data populated flag for new version 3.")
                } else {
                    Log.e("MIGRATION_2_3", "ApplicationContextInternal not initialized. Cannot clear SharedPreferences flag for version 3.")
                }
                Log.i("MIGRATION_2_3", "Finished migration from version 2 to 3.")
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
                    // ★★★ Callbackには更新したDBバージョン(3)を渡す ★★★
                    .addCallback(AppDatabaseCallback(applicationContextInternal, applicationScope, CURRENT_DATABASE_VERSION))
                    // ★★★ addMigrations() に定義したマイグレーションパスを追加 ★★★
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3) // ★ MIGRATION_2_3 を追加 ★
                    .setQueryCallback(
                        object : QueryCallback {
                            override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
                                Log.v("RoomQuery", "SQL Query: $sqlQuery SQL Args: $bindArgs")
                            }
                        },
                        Dispatchers.IO.asExecutor()
                    )
                    // .fallbackToDestructiveMigration() // 通常、マイグレーションを定義したらこれはコメントアウト
                    .build()
                    .also {
                        INSTANCE = it
                        Log.d(TAG, "Database instance created with version $CURRENT_DATABASE_VERSION.")
                    }
            }
        }
    }

    // AppDatabaseCallback クラスは変更なしでOK
    // (コンストラクタに渡される currentAppDbVersion が新しいバージョンになるため、
    //  内部のロジックは新しいバージョン番号で動作します)
    private class AppDatabaseCallback(
        private val context: Context,
        private val scope: CoroutineScope,
        private val currentAppDbVersion: Int // ★ アプリが期待するDBバージョン (ここでは CURRENT_DATABASE_VERSION)
    ) : RoomDatabase.Callback() {

        private val TAG_CALLBACK = "AppDatabaseCallback"

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // onCreate はDBが新規作成されたときに呼ばれる。このときの db.version は CURRENT_DATABASE_VERSION になるはず。
            Log.d(TAG_CALLBACK, "onCreate CALLED! Database version: ${db.version} (App expects $currentAppDbVersion). Populating initial data.")
            INSTANCE?.let { database ->
                // onCreateが呼ばれるのは本当に最初の最初だけなので、ここで投入されるデータはバージョン3の初期データ
                if (db.version == currentAppDbVersion && !getMonstersPopulatedFlag(context, currentAppDbVersion)) {
                    scope.launch {
                        populateMonstersFromJson(context, database.monsterDao(), "onCreate (DB v${db.version})")
                        setMonstersPopulatedFlag(context, db.version, true)
                    }
                } else {
                    Log.w(TAG_CALLBACK, "onCreate: Data population skipped. DB version: ${db.version}, App version: $currentAppDbVersion, Populated flag: ${getMonstersPopulatedFlag(context, db.version)}")
                }
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            Log.d(TAG_CALLBACK, "onOpen CALLED! Database version: ${db.version}. App expects DB version: $currentAppDbVersion")

            INSTANCE?.let { database ->
                // db.version は Room が開いた実際のDBのバージョン
                // currentAppDbVersion は Room に設定した最新のバージョン
                if (db.version == currentAppDbVersion && !getMonstersPopulatedFlag(context, currentAppDbVersion)) {
                    // この条件は、MIGRATION_X_Y でフラグがクリアされた場合や、
                    // 新規インストールで onCreate が呼ばれた後に onOpen が呼ばれる場合（フラグはまだfalse）に合致する。
                    Log.i(
                        TAG_CALLBACK,
                        "onOpen: DB is v$currentAppDbVersion and not yet populated for this version. (Re)populating."
                    )
                    scope.launch {
                        // マイグレーションでテーブルクリア済みのはずなので、ここでは単純に全件挿入
                        populateMonstersFromJson(context, database.monsterDao(), "onOpen (v$currentAppDbVersion - populate/repopulate)")
                        setMonstersPopulatedFlag(context, currentAppDbVersion, true)
                    }
                } else if (getMonstersPopulatedFlag(context, db.version)) {
                    Log.i(TAG_CALLBACK, "onOpen: Data for v${db.version} already populated.")
                } else {
                    // 古いバージョンのDBで、かつフラグもない場合など（通常はマイグレーションされるはず）
                    Log.d(TAG_CALLBACK, "onOpen: No specific repopulation action for v${db.version} (current app version: $currentAppDbVersion, populated flag: ${getMonstersPopulatedFlag(context, db.version)}).")
                }
            }
        }

        private suspend fun populateMonstersFromJson(context: Context, monsterDao: MonsterDao, trigger: String) {
            // (populateMonstersFromJson の実装は変更なし)
            Log.d(TAG_CALLBACK, "[$trigger] Attempting to populate monsters from JSON.")
            try {
                val gson = Gson()
                context.assets.open("monsters.json").use { inputStream -> // ★更新された monsters.json を読み込む
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
            val flag = prefs.getBoolean(getMonsterDataPopulatedKey(version), false)
            Log.d(TAG_CALLBACK, "Monster populated flag for v$version read as: $flag")
            return flag
        }
    }
}
