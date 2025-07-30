package com.sawag.catquestapp // あなたのパッケージ名

import android.app.Application
import android.util.Log
import com.sawag.catquestapp.data.AppDatabase
import com.sawag.catquestapp.data.user.UserRepository
import com.sawag.catquestapp.ui.viewmodel.UserViewModelFactory
import kotlinx.coroutines.CoroutineScope // ← 必要
import kotlinx.coroutines.Dispatchers   // ← 必要
import kotlinx.coroutines.SupervisorJob // ← 必要

class CatQuestApplication : Application() {

    // アプリケーション全体のコルーチンスコープ
    // SupervisorJobを使うと、子コルーチンが失敗してもスコープ全体はキャンセルされない
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO) // ← applicationScope を定義

    // AppDatabaseのインスタンス (lazy初期化)
    // getDatabase に applicationScope を渡す
    val database: AppDatabase by lazy {
        Log.d(TAG, "AppDatabase lazy initializer CALLED")
        AppDatabase.getDatabase(this, applicationScope) // ← scope を渡す
    }

    // UserRepositoryのインスタンス (lazy初期化)
    val userRepository by lazy { UserRepository(database.userDao()) }

    // UserViewModelFactoryのインスタンス (lazy初期化)
    val userViewModelFactory by lazy { UserViewModelFactory(userRepository) }

    companion object {
        private const val TAG = "CatQuestApp_Lifecycle"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "CatQuestApplication: onCreate() - ENTRY POINT")

        // AppDatabaseの初期化をトリガーするために、一度プロパティにアクセスする
        // これは lazy 初期化の特性上、最初のアクセスでインスタンスが生成されるため
        try {
            val dbInstance = database
            Log.d(TAG, "AppDatabase instance obtained in onCreate: $dbInstance (triggered by access)")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing database in onCreate", e)
        }
    }
}


//package com.sawag.catquestapp // あなたのパッケージ名
//
//import android.app.Application
//import android.util.Log
//import com.sawag.catquestapp.data.AppDatabase
//import com.sawag.catquestapp.data.user.UserRepository
//import com.sawag.catquestapp.ui.viewmodel.UserViewModelFactory
//// import kotlinx.coroutines.CoroutineScope // applicationScope を使わない場合は不要になる可能性
//// import kotlinx.coroutines.Dispatchers // 同上
//// import kotlinx.coroutines.SupervisorJob // 同上
//
//class CatQuestApplication : Application() {
//
//    // アプリケーション全体のコルーチンスコープ
//    // SupervisorJobを使うと、子コルーチンが失敗してもスコープ全体はキャンセルされない
//    // ★ AppDatabaseの初期化にscopeを渡さなくなったため、このscopeの必要性も再検討
//    // ★ もし他の非同期処理でアプリレベルのスコープが必要な場合は残す
//    // val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
//
//    // AppDatabaseのインスタンス (lazy初期化)
//    val database: AppDatabase by lazy {
//        Log.d(TAG, "AppDatabase lazy initializer CALLED")
//        // ★ AppDatabase.getDatabase の呼び出しから applicationScope を削除
//        AppDatabase.getDatabase(this)
//    }
//
//    // UserRepositoryのインスタンス (lazy初期化)
//    val userRepository by lazy { UserRepository(database.userDao()) }
//
//    // UserViewModelFactoryのインスタンス (lazy初期化)
//    val userViewModelFactory by lazy { UserViewModelFactory(userRepository) }
//
//    companion object {
//        private const val TAG = "CatQuestApp_Lifecycle"
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//        Log.d(TAG, "CatQuestApplication: onCreate() - ENTRY POINT")
//
//        // AppDatabaseの初期化をトリガーするために、一度プロパティにアクセスする
//        try {
//            val dbInstance = database // databaseプロパティにアクセスして初期化をトリガー
//            Log.d(TAG, "AppDatabase instance obtained in onCreate: $dbInstance")
//        } catch (e: Exception) {
//            Log.e(TAG, "Error initializing database in onCreate", e)
//        }
//    }
//}
