package com.sawag.catquestapp.ui // MainActivityのパッケージをuiに修正

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.launch
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController // NavControllerの型として使用
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
//import androidx.privacysandbox.tools.core.generator.build
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sawag.catquestapp.BreedSelectionScreen
import com.sawag.catquestapp.CatQuestWelcomeScreen
import com.sawag.catquestapp.CombatScreen
import com.sawag.catquestapp.DungeonSelectionScreen
import com.sawag.catquestapp.data.AppDatabase
import com.sawag.catquestapp.data.user.UserEntity
import com.sawag.catquestapp.data.user.UserRepository
import com.sawag.catquestapp.ui.theme.CatQuestAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import androidx.activity.viewModels // ★ by viewModels を使うためにインポート
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import com.sawag.catquestapp.CatQuestApplication // ★ Applicationクラスをインポート
import com.sawag.catquestapp.ui.theme.CatQuestAppTheme
import com.sawag.catquestapp.ui.viewmodel.UserUiState
import com.sawag.catquestapp.ui.viewmodel.UserViewModel

// import com.sawag.catquestapp.ui.CatQuestWelcomeScreen // 正しいパスに修正
// import com.sawag.catquestapp.ui.BreedSelectionScreen // 正しいパスに修正
// import com.sawag.catquestapp.ui.DungeonSelectionScreen // 新規作成する画面
// import com.sawag.catquestapp.ui.CombatScreen // 新規作成する画面

// 画面のルートを定義
object AppDestinations {
    const val WELCOME_SCREEN = "welcome"
    const val BREED_SELECTION_SCREEN = "breed_selection"
    const val DUNGEON_SELECTION_SCREEN = "dungeon_selection" // 変更
    const val COMBAT_SCREEN = "combat_screen"             // 追加
}


class MainActivity : ComponentActivity() {

    // ViewModelの取得 (Applicationクラス経由でFactoryを使用)
    private val userViewModel: UserViewModel by viewModels {
        (application as CatQuestApplication).userViewModelFactory
    }

    companion object {
        private const val TAG = "MainActivity_Lifecycle" // Logcatで見分けやすいようにタグ名を変更
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity: onCreate()") // タグ名を変更

        // ★ ViewModelで特定のユーザーをロードする処理をトリガーする (例: ユーザーID 1)
        // ★ この処理はViewModelの設計によります。
        // ★ もしViewModelのinitブロックでロード処理が始まらない場合や、
        // ★ 特定のアクションでロードを開始する必要がある場合に記述します。
        // ★ 例えば、userViewModel.loadUser(1) のようなメソッドを呼び出すなど。
        // ★ ここでは、ViewModelが初期化時に最初のユーザーをロードするか、
        // ★ あるいはデフォルトユーザーを表示するロジックを持っていると仮定します。
        // ★ もし特定のユーザーIDを指定して表示したい場合は、
        // ★ userViewModelにそのためのメソッドを用意し、ここで呼び出してください。

        setContent {
            CatQuestAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // ★ UserViewModelからUI状態を収集
                    val userUiState by userViewModel.uiState.collectAsState()
                    Log.d(TAG, "Observed UserUiState: $userUiState") // ★ 状態変化をログに出力

                    // ★ UserScreenを直接呼び出し、収集した状態を渡す
                    UserScreen(uiState = userUiState)
                }
            }
        }
    }
}

//class MainActivity : ComponentActivity() {
//
//    // ViewModelの取得 (Applicationクラス経由でFactoryを使用)
//    private val userViewModel: UserViewModel by viewModels {
//        (application as CatQuestApplication).userViewModelFactory
//    }
//
//    companion object {
//        private const val TAG = "MainActivity"
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        Log.d("MainActivity", "MainActivity: onCreate()")
//        setContent {
//            CatQuestAppTheme {
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
//                ) {
//                    // ここで CatQuestAppNavigation を呼び出します
//                    CatQuestAppNavigation()
//                }
//            }
//        }
//    }
//}



@Composable
fun UserScreen(uiState: UserUiState) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (uiState) {
            is UserUiState.Loading -> {
                Log.d("UserScreen", "Displaying Loading state")
                CircularProgressIndicator()
            }
            is UserUiState.Success -> {
                val user = uiState.user
                Log.d("UserScreen", "Displaying Success state for user: ${user.name}")
                Text("User Loaded: ${user.name} (ID: ${user.id})")
                // ここでユーザー情報を使ったUIを構築
            }
            is UserUiState.Error -> {
                Log.e("UserScreen", "Displaying Error state: ${uiState.message}")
                Text("Error: ${uiState.message}")
            }
        }
    }
}

@Composable
fun CatQuestAppNavigation() { // NavHostControllerを引数に取るように変更も可能
    val navController = rememberNavController()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestinations.WELCOME_SCREEN,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ウェルカム画面
            composable(AppDestinations.WELCOME_SCREEN) {
                CatQuestWelcomeScreen( // この画面を別途作成 (内容はほぼ同じでOK)
                    onStartAdventureClick = {
                        navController.navigate(AppDestinations.BREED_SELECTION_SCREEN)
                    }
                )
            }

            // 血統選択画面
            composable(AppDestinations.BREED_SELECTION_SCREEN) {
                BreedSelectionScreen( // BreedSelectionScreen.kt から import
                    onBreedSelected = { selectedBreedName -> // 引数名を selectedBreedName に変更
                        // 選択された猫の情報を次の画面に渡す
                        // 例: 品種名を引数として渡す
                        navController.navigate("${AppDestinations.DUNGEON_SELECTION_SCREEN}/$selectedBreedName")
                    }
                )
            }

            // ダンジョン選択画面
            composable(
                route = "${AppDestinations.DUNGEON_SELECTION_SCREEN}/{breedName}", // breedName を受け取る
                arguments = listOf(navArgument("breedName") { type = NavType.StringType })
            ) { backStackEntry ->
                val breedName = backStackEntry.arguments?.getString("breedName") ?: "不明な猫"
                DungeonSelectionScreen( // この画面を新規作成
                    selectedBreedName = breedName,
                    onDungeonSelected = { dungeonId ->
                        // ダンジョンIDと品種名を戦闘画面に渡す
                        navController.navigate("${AppDestinations.COMBAT_SCREEN}/$dungeonId/$breedName")
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // 戦闘画面
            composable(
                route = "${AppDestinations.COMBAT_SCREEN}/{dungeonId}/{breedName}", // dungeonIdとbreedNameを受け取る
                arguments = listOf(
                    navArgument("dungeonId") { type = NavType.StringType },
                    navArgument("breedName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val dungeonId = backStackEntry.arguments?.getString("dungeonId") ?: "unknown_dungeon"
                val breedName = backStackEntry.arguments?.getString("breedName") ?: "不明な猫"
                CombatScreen( // この画面を新規作成
                    dungeonId = dungeonId,
                    playerBreedName = breedName,
                    onCombatEnd = { /* combatResult -> */ // 戦闘結果に応じて処理
                        // 例: ダンジョン選択に戻る
                        navController.popBackStack(AppDestinations.DUNGEON_SELECTION_SCREEN, inclusive = false)
                        // または、結果画面に遷移するなど
                        // if (combatResult == "win") navController.navigate("game_clear")
                        // else navController.navigate("game_over")
                    },
                    onRunAway = { // 逃げる場合の処理
                        navController.popBackStack(AppDestinations.DUNGEON_SELECTION_SCREEN, inclusive = false)
                    }
                )
            }
            // TODO: ゲームクリア画面、ゲームオーバー画面などもここに追加
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CatQuestAppTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            // Previewで特定の画面を見たい場合は、その画面のComposableを直接呼び出す
            // 例: CatQuestWelcomeScreen(onStartAdventureClick = {})
            // 例: BreedSelectionScreen(onBreedSelected = {})
            // 例: DungeonSelectionScreen(selectedBreedName = "三毛猫", onDungeonSelected = {}, onNavigateBack = {})
            Text("Main Activity Preview (No specific screen selected for preview)")
        }
    }
}