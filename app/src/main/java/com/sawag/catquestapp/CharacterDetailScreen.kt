package com.sawag.catquestapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sawag.catquestapp.ui.theme.CatQuestAppTheme

@Composable
fun CharacterDetailScreen(
    modifier: Modifier = Modifier,
    selectedBreed: String,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "選ばれた勇者猫！",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "血統: $selectedBreed",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onNavigateBack) {
            Text("もう一度選ぶ")
        }

    }
}

@Preview(showBackground = true)
@Composable
fun CharacterDetailScreenPreview() {
    CatQuestAppTheme { // あなたのアプリのテーマ関数名に置き換えてください
        CharacterDetailScreen(
            selectedBreed = "ロシアンブルー",
            onNavigateBack = {}
        )
    }
}