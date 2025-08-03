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
import com.sawag.catquestapp.UserRegistrationScreen
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
    const val USER_REGISTRATION_SCREEN = "user_registration" // 追加
    const val BREED_SELECTION_SCREEN = "breed_selection"
    const val DUNGEON_SELECTION_SCREEN = "dungeon_selection" // 変更
    const val COMBAT_SCREEN = "combat_screen"             // 追加
    const val CHARACTER_CREATION_FLOW_ROUTE = "character_creation_flow" // ネストされたグラフのルート名
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

@Composable
fun CatQuestAppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val application = LocalContext.current.applicationContext as CatQuestApplication
    // UserViewModel を Activity/Application スコープで取得
    val userViewModel: UserViewModel = viewModel(
        factory = UserViewModel.provideFactory(application.userRepository)
    )

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestinations.WELCOME_SCREEN,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppDestinations.WELCOME_SCREEN) {
                CatQuestWelcomeScreen(
                    userViewModel = userViewModel,
                    onNavigateToRegistration = {
                        navController.navigate(AppDestinations.USER_REGISTRATION_SCREEN)
                        // WelcomeScreen には戻れないように popUpTo はしないでおくか、
                        // するなら character_creation_flow 完了時にまとめて消すなど検討
                    },
                    onNavigateToCharacterCreation = {
                        navController.navigate(AppDestinations.CHARACTER_CREATION_FLOW_ROUTE) {
                            popUpTo(AppDestinations.WELCOME_SCREEN) { inclusive = true }
                        }
                    }
                )
            }

            composable(AppDestinations.USER_REGISTRATION_SCREEN) {
                UserRegistrationScreen(
                    userViewModel = userViewModel,
                    onNavigateToCharacterCreation = {
                        navController.navigate(AppDestinations.CHARACTER_CREATION_FLOW_ROUTE) {
                            // 登録画面とウェルカム画面をスタックから消す
                            popUpTo(AppDestinations.WELCOME_SCREEN) { inclusive = true }
                        }
                    },
                    onNavigateBack = {
                        navController.popBackStack() // WelcomeScreen に戻る
                    }
                )
            }

            navigation(
                route = AppDestinations.CHARACTER_CREATION_FLOW_ROUTE,
                startDestination = AppDestinations.BREED_SELECTION_SCREEN
            ) {
                composable(AppDestinations.BREED_SELECTION_SCREEN) { backStackEntry ->
                    // ネストされたナビゲーショングラフ内でViewModelを共有する場合
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(AppDestinations.CHARACTER_CREATION_FLOW_ROUTE)
                    }
                    val characterCreationViewModel: UserViewModel =
                        viewModel( // ここは UserViewModel で良いか検討
                            viewModelStoreOwner = parentEntry,
                            factory = UserViewModel.provideFactory(application.userRepository)
                        )
                    BreedSelectionScreen(
                        userViewModel = characterCreationViewModel, // またはグローバルの userViewModel
                        onNavigateToNextScreen = {
                            navController.navigate(AppDestinations.DUNGEON_SELECTION_SCREEN)
                        }
                    )
                }
                composable(AppDestinations.DUNGEON_SELECTION_SCREEN) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(AppDestinations.CHARACTER_CREATION_FLOW_ROUTE)
                    }
                    val userViewModelForDungeon: UserViewModel = viewModel(
                        viewModelStoreOwner = parentEntry,
                        factory = UserViewModel.provideFactory(application.userRepository)
                    )
                    DungeonSelectionScreen(
                        userViewModel = userViewModelForDungeon,
                        onNavigateToCombat = { dungeonId ->
                            val playerBreedName =
                                userViewModelForDungeon.currentUser.value?.breed ?: "不明な猫"
                            navController.navigate("${AppDestinations.COMBAT_SCREEN}/$dungeonId/$playerBreedName")
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                // COMBAT_SCREEN の定義 ...
                composable(
                    route = "${AppDestinations.COMBAT_SCREEN}/{dungeonId}/{playerBreedName}",
                    arguments = listOf(
                        navArgument("dungeonId") { type = NavType.StringType },
                        navArgument("playerBreedName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    // ... CombatScreen の実装
                    val dungeonId =
                        backStackEntry.arguments?.getString("dungeonId") ?: "unknown_dungeon"
                    val playerBreedName =
                        backStackEntry.arguments?.getString("playerBreedName") ?: "不明な猫"
                    CombatScreen(
                        dungeonId = dungeonId,
                        playerBreedName = playerBreedName,
                        onCombatEnd = {
                            navController.popBackStack(
                                AppDestinations.DUNGEON_SELECTION_SCREEN,
                                inclusive = false
                            )
                        },
                        onRunAway = {
                            navController.popBackStack(
                                AppDestinations.DUNGEON_SELECTION_SCREEN,
                                inclusive = false
                            )
                        }
                    )
                }
            }
        }
    }
}


//@Preview(showBackground = true)
//@Composable
//fun DefaultPreview() {
//    CatQuestAppTheme {
//        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
//            // Previewでは、CatQuestWelcomeScreenを直接表示するように変更すると良いでしょう
//            CatQuestWelcomeScreen(onStartAdventureClick = {})
//        }
//    }
//}