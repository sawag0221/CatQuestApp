package com.sawag.catquestapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sawag.catquestapp.ui.theme.CatQuestAppTheme
import com.sawag.catquestapp.ui.viewmodel.UserUiState
import com.sawag.catquestapp.ui.viewmodel.UserViewModel

// CatQuestWelcomeScreen.kt
@Composable
fun CatQuestWelcomeScreen(
    userViewModel: UserViewModel, // Activity/Application スコープの ViewModel を想定
    onNavigateToRegistration: () -> Unit,
    onNavigateToCharacterCreation: () -> Unit
) {
    val userUiState by userViewModel.uiState.collectAsState() // 既存の UserUiState を使用

    // WelcomeScreen が表示されたときにユーザー情報をロード開始
    LaunchedEffect(Unit) {
        userViewModel.loadCurrentUser() // 確実に最新のユーザー情報を取得
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ... (タイトルなど)
        Text("Cat Quest へようこそ！", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        when (val state = userUiState) {
            is UserUiState.Loading -> {
                CircularProgressIndicator() // ユーザー情報ロード中
            }
            is UserUiState.Success -> {
                Button(
                    onClick = {
                        if (state.user.name == "プレイヤー") { // デフォルト名
                            onNavigateToRegistration()
                        } else {
                            onNavigateToCharacterCreation()
                        }
                    }
                ) {
                    Text("冒険を始める")
                }
            }
            is UserUiState.Error -> {
                Text("ユーザー情報の読み込みに失敗しました。", color = MaterialTheme.colorScheme.error)
                // エラー時でも進めるようにボタンを出すか、リトライ処理を促すかなど検討
                Button(onClick = { userViewModel.loadCurrentUser() }) { // 例: リトライボタン
                    Text("再試行")
                }
            }
        }
    }
}


//@Preview(showBackground = true)
//@Composable
//fun CatQuestWelcomeScreenPreview() {
//    CatQuestAppTheme {
//        // Scaffold の padding を考慮してプレビューする場合
//        Scaffold { innerPadding ->
//            CatQuestWelcomeScreen(modifier = Modifier.padding(innerPadding),
//                onStartAdventureClick = {})
//        }
//        // 単純にプレビューする場合
//        // CatQuestWelcomeScreen()
//    }
//}