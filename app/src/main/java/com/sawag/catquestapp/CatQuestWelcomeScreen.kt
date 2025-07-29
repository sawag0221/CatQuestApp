package com.sawag.catquestapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sawag.catquestapp.ui.theme.CatQuestAppTheme

// CatQuestWelcomeScreen を少し変更して、クリックイベントを上に伝える
@Composable
fun CatQuestWelcomeScreen(
    modifier: Modifier = Modifier,
    onStartAdventureClick: () -> Unit // ボタンクリック時のコールバック
) {
    // Column を使って要素を縦に並べます
    Column(
        modifier = modifier
            .fillMaxSize() // 画面全体に広げる
            .padding(16.dp), // 内側に余白
        horizontalAlignment = Alignment.CenterHorizontally, // 水平方向中央揃え
        verticalArrangement = Arrangement.Center // 垂直方向中央揃え
    ) {
        Text(
            text = "CAT QUEST",
            style = MaterialTheme.typography.headlineLarge // 少し大きなタイトル文字
        )
        Spacer(modifier = Modifier.height(16.dp)) // 縦のスペース
        Text(
            text = "冒険を始めよう！",
            style = MaterialTheme.typography.bodyLarge // 少し大きな本文文字
        )
        Spacer(modifier = Modifier.height(32.dp)) // ボタンとの距離を少し広め

        // 「冒険を始める」ボタンを追加
        Button(
            // TODO: ボタンがクリックされたときの処理をここに書く
            // 例えば、血統選択画面に遷移するなど
            onClick = onStartAdventureClick // コールバックを呼び出す
        ) {
            Text("冒険を始める")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CatQuestWelcomeScreenPreview() {
    CatQuestAppTheme {
        // Scaffold の padding を考慮してプレビューする場合
        Scaffold { innerPadding ->
            CatQuestWelcomeScreen(modifier = Modifier.padding(innerPadding),
                onStartAdventureClick = {})
        }
        // 単純にプレビューする場合
        // CatQuestWelcomeScreen()
    }
}