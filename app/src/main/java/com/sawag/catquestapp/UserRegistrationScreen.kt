package com.sawag.catquestapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sawag.catquestapp.ui.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserRegistrationScreen(
    userViewModel: UserViewModel, // MainActivity から渡される ViewModel を想定
    onNavigateToCharacterCreation: () -> Unit,
    onNavigateBack: () -> Unit // 必要であれば戻る処理
) {
    var playerName by rememberSaveable { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) } // 更新処理中のローディング表示用

    // ViewModel からユーザー更新結果を監視する場合の例
    // LaunchedEffect(userViewModel.userUpdateResult) { ... } のような形も考えられる

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("プレイヤー名登録") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { // 戻るボタンの処理
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("あなたの名前を教えてください", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = playerName,
                onValueChange = {
                    playerName = it
                    if (it.isNotBlank()) showError = false
                },
                label = { Text("名前") },
                singleLine = true,
                isError = showError,
                modifier = Modifier.fillMaxWidth()
            )
            if (showError) {
                Text(
                    "名前を入力してください",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    if (playerName.isNotBlank()) {
                        isLoading = true
                        // ViewModel を通じてユーザー名を更新
                        userViewModel.updateUserNameAndSignalCompletion(
                            newName = playerName,
                            onResult = { success ->
                                isLoading = false
                                if (success) {
                                    onNavigateToCharacterCreation() // 成功したら次のフローへ
                                } else {
                                    // 更新失敗時の処理 (例: Toast表示、エラーメッセージ表示)
                                    showError = true // ここでは例として単純にエラー表示
                                    // 必要であれば、より具体的なエラーメッセージをViewModelから受け取る
                                }
                            }
                        )
                    } else {
                        showError = true
                    }
                },
                enabled = !isLoading, // ローディング中は無効化
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("これで決定！")
                }
            }
        }
    }
}
