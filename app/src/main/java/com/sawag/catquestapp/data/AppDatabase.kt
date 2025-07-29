//package com.sawag.catquestapp.data
//
//import android.content.Context
//import android.util.Log
//// import androidx.compose.foundation.layout.size // 未使用の可能性
//import androidx.room.Database
//import androidx.room.Room
//import androidx.room.RoomDatabase
//// import androidx.room.withTransaction // Monster関連で使っていたのでコメントアウトの可能性
//import androidx.sqlite.db.SupportSQLiteDatabase
//import com.sawag.catquestapp.data.user.UserDao
//import com.sawag.catquestapp.data.user.UserEntity
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.asExecutor
//import kotlinx.coroutines.launch
//// import kotlinx.serialization.json.Json // Monster関連で使っていたのでコメントアウトの可能性
//// import kotlinx.serialization.decodeFromString // Monster関連で使っていたのでコメントアウトの可能性
//import java.io.IOException
//
//@Database(
//    entities = [UserEntity::class], // UserEntity のみ
//    version = 1, // ★ スキーマを単純化するためバージョンを初期値(例:1)に戻す
//    exportSchema = false
//)
//abstract class AppDatabase : RoomDatabase() {
//
//    abstract fun userDao(): UserDao
//    // abstract fun monsterDao(): MonsterDao // MonsterDao 削除
//
//    // --- コールバッククラス (内容は大幅に削減) ---
//    private class SimpleAppCallback(
//        private val context: Context, // Context は残す (将来的に使う可能性があるため)
//        private val scope: CoroutineScope, // Scope は残す
//        private val dbProvider: suspend () -> AppDatabase // dbProvider は残す
//    ) : RoomDatabase.Callback() {
//
//        private val TAG = "SimpleAppCallback"
//
//        override fun onCreate(db: SupportSQLiteDatabase) {
//            super.onCreate(db)
//            Log.d(TAG, "onCreate CALLED - Database CREATED") // ログメッセージをシンプルに
//
//            // scope.launch(Dispatchers.IO) {
//            //     Log.d(TAG, "onCreate - Coroutine LAUNCHED (Monster logic removed)")
//            //     // モンスター関連のロード処理はすべて削除
//            // }
//        }
//
//        override fun onOpen(db: SupportSQLiteDatabase) {
//            super.onOpen(db)
//            Log.d(TAG, "onOpen CALLED - Database OPENED") // ログメッセージをシンプルに
//
//            // scope.launch(Dispatchers.IO) {
//            //     Log.d(TAG, "onOpen - Coroutine LAUNCHED (Monster logic removed)")
//            //     // モンスター関連のバージョンチェック処理はすべて削除
//            // }
//        }
//
//        // loadMonstersFromJsonAndInsert メソッド全体を削除またはコメントアウト
//        /*
//        private suspend fun loadMonstersFromJsonAndInsert(...) { ... }
//        */
//
//        // checkMonsterDataVersionAndReloadIfNeeded メソッド全体を削除またはコメントアウト
//        /*
//        private suspend fun checkMonsterDataVersionAndReloadIfNeeded(...) { ... }
//        */
//
//        // loadJsonStringFromAsset はユーティリティとして残しても良いが、現時点では呼ばれない
//        /*
//        private fun loadJsonStringFromAsset(context: Context, fileName: String): String? {
//            return try {
//                context.assets.open(fileName).bufferedReader().use { it.readText() }
//            } catch (ioException: IOException) {
//                Log.e(TAG, "Error reading $fileName from assets", ioException)
//                null
//            }
//        }
//        */
//
//        // SharedPreferences関連のメソッドも削除またはコメントアウト
//        /*
//        private fun getLastLoadedMonsterJsonVersion(context: Context): Int { ... }
//        private fun saveLastLoadedMonsterJsonVersion(context: Context, version: Int) { ... }
//        */
//
//        // companion object 内の定数も Monster 関連のものは削除またはコメントアウト
//        /*
//        companion object {
//            private const val MONSTERS_JSON_FILE_NAME = "monsters.json"
//            private const val APP_DATA_VERSIONS_PREFS = "app_data_versions"
//            private const val MONSTER_JSON_VERSION_KEY = "monster_json_version"
//        }
//        */
//    }
//
//    companion object {
//        @Volatile
//        private var INSTANCE: AppDatabase? = null
//
//        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
//            return INSTANCE ?: synchronized(this) {
//                INSTANCE ?: buildDatabase(context, scope).also { INSTANCE = it }
//            }
//        }
//
//        private fun buildDatabase(context: Context, scope: CoroutineScope): AppDatabase {
//            lateinit var tempDbInstanceHolder: AppDatabase
//            val dbProviderLambda: suspend () -> AppDatabase = { tempDbInstanceHolder }
//
//            val instance = Room.databaseBuilder(
//                context.applicationContext,
//                AppDatabase::class.java,
//                "cat_quest_database"
//            )
//                // ★ コールバックの登録は残す (コールバック自体が呼ばれるかの確認のため)
//                .addCallback(SimpleAppCallback(context.applicationContext, scope, dbProviderLambda))
//                // fallbackToDestructiveMigration は、バージョンを下げた場合にDBを再作成するために一時的に有効にするのもあり
//                .fallbackToDestructiveMigration() // ★ バージョン変更で再作成を許可
//                .setQueryCallback(
//                    object : RoomDatabase.QueryCallback {
//                        override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
//                            Log.v("RoomQuery", "SQL Query: $sqlQuery SQL Args: $bindArgs")
//                        }
//                    },
//                    Dispatchers.IO.asExecutor()
//                )
//                .build()
//
//            tempDbInstanceHolder = instance
//            return instance
//        }
//    }
//}
//


package com.sawag.catquestapp.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
// import androidx.sqlite.db.SupportSQLiteDatabase // Callback を使わないので不要
import com.sawag.catquestapp.data.user.UserDao // ★ UserDao をインポート
import com.sawag.catquestapp.data.user.UserEntity // ★ UserEntity をインポート
// import kotlinx.coroutines.CoroutineScope // getDatabase の引数から削除する場合は不要
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

@Database(
    entities = [UserEntity::class], // UserEntity のみ
    version = 1,                   // ★ 初期バージョン
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // ★ scope 引数を削除し、よりシンプルに
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        // ★ scope 引数を削除
        private fun buildDatabase(context: Context): AppDatabase {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "cat_quest_database"
            )
                // .addCallback(...) // コールバックは使用しない
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
            return instance
        }
    }
}

