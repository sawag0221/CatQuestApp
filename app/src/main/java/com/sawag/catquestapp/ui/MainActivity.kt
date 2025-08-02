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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.navigation
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

//     ViewModelの取得 (Applicationクラス経由でFactoryを使用)
//    private val userViewModel: UserViewModel by viewModels {
//        (application as CatQuestApplication).userViewModelFactory
//    }

    companion object {
        private const val TAG = "MainActivity_Lifecycle" // Logcatで見分けやすいようにタグ名を変更
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity: onCreate()")

        setContent {
            CatQuestAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // ★ ここを CatQuestAppNavigation の呼び出しに戻します
                    CatQuestAppNavigation()
                }
            }
        }
    }
}


//@Composable
//fun UserScreen(uiState: UserUiState) {
//    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//        when (uiState) {
//            is UserUiState.Loading -> {
//                Log.d("UserScreen", "Displaying Loading state")
//                CircularProgressIndicator()
//            }
//            is UserUiState.Success -> {
//                val user = uiState.user
//                Log.d("UserScreen", "Displaying Success state for user: ${user.name}")
//                Text("User Loaded: ${user.name} (ID: ${user.id})")
//                // ここでユーザー情報を使ったUIを構築
//            }
//            is UserUiState.Error -> {
//                Log.e("UserScreen", "Displaying Error state: ${uiState.message}")
//                Text("Error: ${uiState.message}")
//            }
//        }
//    }
//}

@Composable
fun CatQuestAppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val application = LocalContext.current.applicationContext as CatQuestApplication

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestinations.WELCOME_SCREEN, // アプリ全体の開始点は Welcome
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppDestinations.WELCOME_SCREEN) {
                CatQuestWelcomeScreen(
                    onStartAdventureClick = {
                        // "character_creation_flow" グラフに遷移
                        navController.navigate("character_creation_flow") {
                            // Welcome画面には戻らないようにする場合
                            popUpTo(AppDestinations.WELCOME_SCREEN) { inclusive = true }
                        }
                    }
                )
            }

            // ★ UserViewModel を共有する画面群のナビゲーショングラフ
            navigation(
                startDestination = AppDestinations.BREED_SELECTION_SCREEN,
                route = "character_creation_flow" // ★ このグラフのルート名
            ) {
                composable(AppDestinations.BREED_SELECTION_SCREEN) { backStackEntry ->
                    // "character_creation_flow" グラフにスコープされた ViewModel を取得
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry("character_creation_flow")
                    }
                    val userViewModel: UserViewModel = viewModel(
                        viewModelStoreOwner = parentEntry, // ★ グラフのルートにスコープ
                        factory = UserViewModel.provideFactory(application.userRepository)
                    )

                    BreedSelectionScreen(
                        onNavigateToNextScreen = {
                            navController.navigate(AppDestinations.DUNGEON_SELECTION_SCREEN)
                            // ここでの popUpTo は、この "character_creation_flow" 内での挙動を考える
                            // 例えば BREED_SELECTION_SCREEN には戻らないようにするなど
                            // popUpTo(AppDestinations.BREED_SELECTION_SCREEN) { inclusive = true }
                        },
                        userViewModel = userViewModel
                    )
                }

                composable(AppDestinations.DUNGEON_SELECTION_SCREEN) { backStackEntry ->
                    // 同じく "character_creation_flow" グラフにスコープされた ViewModel を取得
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry("character_creation_flow")
                    }
                    val userViewModel: UserViewModel = viewModel(
                        viewModelStoreOwner = parentEntry, // ★ グラフのルートにスコープ
                        factory = UserViewModel.provideFactory(application.userRepository)
                    )

                    DungeonSelectionScreen(
                        userViewModel = userViewModel,
                        onNavigateToCombat = { dungeonId ->
                            val playerBreedName =
                                userViewModel.currentUser.value?.breed ?: "不明な猫"
                            navController.navigate("${AppDestinations.COMBAT_SCREEN}/$dungeonId/$playerBreedName")
                        },
                        onNavigateBack = { navController.popBackStack() } // このグラフ内で戻る
                    )
                }

                // COMBAT_SCREEN など、userViewModel を共有したい他の画面もこのグラフ内に追加
                composable(
                    route = "${AppDestinations.COMBAT_SCREEN}/{dungeonId}/{playerBreedName}",
                    arguments = listOf(
                        navArgument("dungeonId") { type = NavType.StringType },
                        navArgument("playerBreedName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry("character_creation_flow")
                    }
//                    val userViewModel: UserViewModel = viewModel(
//                        viewModelStoreOwner = parentEntry,
//                        factory = UserViewModel.provideFactory(application.userRepository)
//                    )
                    val dungeonId = backStackEntry.arguments?.getString("dungeonId")
                        ?: "unknown_dungeon" // ★ String で取得
                    val playerBreedName =
                        backStackEntry.arguments?.getString("playerBreedName") ?: "不明な猫"

                    CombatScreen(
                        dungeonId = dungeonId,                // String を渡す
                        playerBreedName = playerBreedName,    // String を渡す
                        onCombatEnd = {
                            // 例: ダンジョン選択画面に戻るなど
                            navController.popBackStack(
                                AppDestinations.DUNGEON_SELECTION_SCREEN,
                                inclusive = false
                            )
                        },
                        onRunAway = {
                            // 例: ダンジョン選択画面に戻るなど
                            navController.popBackStack(
                                AppDestinations.DUNGEON_SELECTION_SCREEN,
                                inclusive = false
                            )
                        }
                        // viewModel は CombatScreen 側で default viewModel() で取得するので、ここでは渡さない
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CatQuestAppTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            // Previewでは、CatQuestWelcomeScreenを直接表示するように変更すると良いでしょう
            CatQuestWelcomeScreen(onStartAdventureClick = {})
        }
    }
}