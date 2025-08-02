package com.sawag.catquestapp.ui // パッケージ名は適宜調整

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sawag.catquestapp.ui.viewmodel.UserUiState // UserUiState をインポート
import com.sawag.catquestapp.ui.viewmodel.UserViewModel

// DungeonSelectionScreen の修正案
@Composable
fun DungeonSelectionScreen(
    userViewModel: UserViewModel, // ★ UserViewModel を受け取る
    onNavigateToCombat: (dungeonId: String) -> Unit, // ★ コールバック名を合わせる
    onNavigateBack: () -> Unit
) {
    val userUiState by userViewModel.uiState.collectAsState() // ViewModelのUI状態を監視

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, // 中央寄せを追加しても良い
        verticalArrangement = Arrangement.Center // 中央寄せを追加しても良い
    ) {
        Text("ダンジョン選択", style = MaterialTheme.typography.headlineLarge) // 少し大きく
        Spacer(modifier = Modifier.height(16.dp))

        when (val state = userUiState) { // UI状態によって表示を切り替える
            is UserUiState.Loading -> {
                CircularProgressIndicator()
                Text("ユーザー情報を読み込み中...")
            }
            is UserUiState.Success -> {
                val user = state.user
                Text(
                    "ようこそ、${user.breed} Lv.${user.level}！", // 血統とレベルを表示
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "所持コイン: ${user.catCoins} 🪙  経験値: ${user.experiencePoints} XP", // コインとXPも表示
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(32.dp))

                Button(onClick = { onNavigateToCombat("first_cave") /* 仮のダンジョンID */ }) {
                    Text("最初の洞窟に挑む")
                }
                Spacer(modifier = Modifier.height(16.dp))
                // 他のダンジョンへのボタンもここに追加可能
            }
            is UserUiState.Error -> {
                Text("エラー: ${state.message}", color = MaterialTheme.colorScheme.error)
                Text("ユーザー情報の読み込みに失敗しました。")
            }
        }

        Spacer(modifier = Modifier.weight(1f)) // 残りのスペースを埋める

        Button(onClick = onNavigateBack) {
            Text("猫を選び直す (またはタイトルへ)") // ボタンのテキストを少し調整
        }
    }
}
