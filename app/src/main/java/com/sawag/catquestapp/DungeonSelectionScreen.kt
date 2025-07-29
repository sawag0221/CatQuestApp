package com.sawag.catquestapp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// DungeonSelectionScreen の仮定義 (DungeonSelectionScreen.kt に作成)
@Composable
fun DungeonSelectionScreen(
    selectedBreedName: String,
    onDungeonSelected: (dungeonId: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text("ダンジョン選択", style = MaterialTheme.typography.headlineMedium)
        Text("選ばれた猫: $selectedBreedName")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onDungeonSelected("first_cave") /* 仮のダンジョンID */ }) {
            Text("最初の洞窟に挑む")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onNavigateBack) {
            Text("猫を選び直す")
        }
    }
}