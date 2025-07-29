package com.sawag.catquestapp // ★ あなたのパッケージ名に合わせてください

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf // ★ 追加
import androidx.compose.runtime.remember // ★ 追加
import androidx.compose.runtime.setValue // ★ 追加
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext // ★ 追加
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sawag.catquestapp.ui.theme.CatQuestAppTheme

// アプリケーション全体で共有、またはこの機能群で共有するJsonインスタンス
// このファイルや、関連するファイルをまとめたパッケージのトップレベルに定義するのが一般的
private val appJson = Json {
    ignoreUnknownKeys = true
    // 他に必要な共通設定があればここに追加
// JSONファイルの構造に対応するデータクラス
}

@Serializable // Kotlinx Serializationでパースするために必要
data class BreedJsonData(
    val id: Int, // JSONファイルにidフィールドがあることを想定
    val name: String,
    val imageName: String
)

// 画面表示や内部ロジックで使用するデータクラス (リソースIDを持つ)
// (これは既存のBreedDataと同じなので、そちらを調整してもOK)
data class BreedDisplayData(
    val id: Int, // idも持つように変更
    val name: String,
    val imageResId: Int
)

// ★ キャラクター画像を上下にアニメーションさせるコンポーザブル
@SuppressLint("UseOfNonLambdaOffsetOverload")
@Composable
fun AnimatedCharacterImage(
    imageResId: Int,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "character_bobbing")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -8f, // 上に移動する量 (dp単位で調整) - 少し控えめに
        targetValue = 8f,  // 下に移動する量 (dp単位で調整) - 少し控えめに
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing), // 少しゆっくりめに
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )

    Image(
        painter = painterResource(id = imageResId),
        contentDescription = contentDescription,
        modifier = modifier.offset(y = offsetY.dp) // Y軸方向にオフセットを適用
    )
}

// assetsからJSONを読み込みパースする関数
fun loadBreedsFromJson(context: Context): List<BreedJsonData> {
    val jsonString: String
    try {
        // assetsフォルダ内のbreeds.jsonを読み込む
        jsonString = context.assets.open("breeds.json").bufferedReader().use { it.readText() }
    } catch (ioException: IOException) {
        ioException.printStackTrace()
        Log.e("LoadBreeds", "Error reading breeds.json", ioException)
        return emptyList() // エラー時は空リストを返すなど、適切なエラーハンドリングを
    }

    return try {
        // 事前に定義したappJsonインスタンスを使用
        appJson.decodeFromString<List<BreedJsonData>>(jsonString)
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("LoadBreeds", "Error parsing breeds.json", e)
        emptyList()
    }
}

// BreedJsonDataのリストをBreedDisplayDataのリストに変換する関数
// (imageNameから実際のリソースIDを取得)
@SuppressLint("DiscouragedApi")
fun convertToDisplayData(
    context: Context,
    jsonDataList: List<BreedJsonData>
): List<BreedDisplayData> {
    return jsonDataList.mapNotNull { jsonData -> // mapNotNullでリソースが見つからない場合はnullを返し、結果から除外
        val imageResId = context.resources.getIdentifier(
            jsonData.imageName,
            "drawable",
            context.packageName
        )
        if (imageResId != 0) { // リソースIDが0の場合は見つからなかったことを意味する
            BreedDisplayData(jsonData.id, jsonData.name, imageResId)
        } else {
            Log.w("ConvertToDisplay", "Drawable resource not found for: ${jsonData.imageName}")
            null
        }
    }
}

// 新しい血統画面の composable
@Composable
fun BreedSelectionScreen(
    modifier: Modifier = Modifier,
    onBreedSelected: (String) -> Unit
) {
    val context = LocalContext.current // ★ Contextを取得
    // ★ JSONから読み込んだデータを保持する状態
    var breeds by remember { mutableStateOf<List<BreedDisplayData>>(emptyList()) }

    // ★ 副作用としてJSONデータを読み込む
    // LaunchedEffectのキーにUnitを指定すると、コンポーザブルが最初にコンポジションされたときに一度だけ実行される
    LaunchedEffect(Unit) {
        val jsonData = loadBreedsFromJson(context)
        breeds = convertToDisplayData(context, jsonData)
        if (breeds.isEmpty()) {
            Log.e(
                "BreedSelectionScreen",
                "No breeds loaded, check JSON file and drawable resources."
            )
        }
    }

    // ★ breedsが空の場合はローディング表示などをするか、早期リターンする (任意)
    if (breeds.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("猫ちゃんデータを読み込み中...")
            // ここにCircularProgressIndicatorなどを置いても良い
        }
        return // breedsが空のうちはPagerなどを表示しない
    }

    // Pager の状態を管理 (breeds.sizeに依存するため、breedsが初期化された後に宣言)
    val pagerState = rememberPagerState(pageCount = { breeds.size })

    LaunchedEffect(pagerState, breeds) { // breedsもキーに含めることで、breedsが変更された場合も再実行
        if (breeds.isNotEmpty()) { // breedsが空でないことを確認
            snapshotFlow { pagerState.currentPage }.collect { page ->
                if (page < breeds.size) { // ページインデックスが範囲内か確認
                    Log.d("BreedSelection", "Current page: ${breeds[page].name}")
                }
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.cat_room_background_placeholder),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(
                            radius = 16.dp,
                            edgeTreatment = BlurredEdgeTreatment.Rectangle
                        )
                    } else {
                        Modifier.drawWithContent {
                            drawContent()
                            drawRect(Color.Black.copy(alpha = 0.30f))
                        }
                    }
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "血統を選んでにゃ！",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(24.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 32.dp)
            ) { pageIndex ->
                // ★ bounds check
                if (pageIndex < breeds.size) {
                    val breed = breeds[pageIndex]
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        AnimatedCharacterImage(
                            imageResId = breed.imageResId, // BreedDisplayDataのimageResIdを使用
                            contentDescription = breed.name,
                            modifier = Modifier
                                .size(200.dp)
                                .padding(8.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = breed.name,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            color = Color.Black
                        )
                    }
                } else {
                    // データが存在しない場合のフォールバックUI（通常は発生しないはず）
                    Log.w("HorizontalPager", "Index out of bounds for breeds list: $pageIndex")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                Modifier
                    .height(24.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(breeds.size) { iteration ->
                    val color =
                        if (pagerState.currentPage == iteration) Color.DarkGray else Color.LightGray
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    // ★ bounds check
                    if (pagerState.currentPage < breeds.size) {
                        val selectedBreedName = breeds[pagerState.currentPage].name
                        Log.d("BreedSelection", "$selectedBreedName が選択されました")
                        onBreedSelected(selectedBreedName) // ここではnameを渡しているが、idを渡す方が良い場合もある
                    }
                },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("この子にする！")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BreedSelectionScreenPreview() {
    CatQuestAppTheme {
        // プレビューではJSON読み込みが難しいため、ダミーデータを使うか、
        // BreedSelectionScreenに直接ダミーリストを渡せるように引数を変更するなどの工夫が必要
        // ここでは、プレビューがクラッシュしないように、onBreedSelectedのみ渡す
        BreedSelectionScreen(
            onBreedSelected = { selectedBreed ->
                Log.d("Preview", "Breed selected in preview: $selectedBreed")
            }
        )
        // または、プレビュー用にダミーのBreedDisplayDataリストを作成して渡す
        // val dummyBreeds = listOf(
        //     BreedDisplayData(1, "プレビュー猫1", R.drawable.american_shorthair_placeholder),
        //     BreedDisplayData(2, "プレビュー猫2", R.drawable.scottish_fold_placeholder)
        // )
        // BreedSelectionScreen(breeds = dummyBreeds, onBreedSelected = { ... })
        // ※ その場合、BreedSelectionScreenの引数に breeds: List<BreedDisplayData> を追加する必要がある
    }
}


//data class BreedData(val name: String, val imageResId: Int)
//
//// ★ キャラクター画像を上下にアニメーションさせるコンポーザブル
//@Composable
//fun AnimatedCharacterImage(
//    imageResId: Int,
//    contentDescription: String,
//    modifier: Modifier = Modifier
//) {
//    val infiniteTransition = rememberInfiniteTransition(label = "character_bobbing")
//    val offsetY by infiniteTransition.animateFloat(
//        initialValue = -8f, // 上に移動する量 (dp単位で調整) - 少し控えめに
//        targetValue = 8f,  // 下に移動する量 (dp単位で調整) - 少し控えめに
//        animationSpec = infiniteRepeatable(
//            animation = tween(durationMillis = 1800, easing = LinearEasing), // 少しゆっくりめに
//            repeatMode = RepeatMode.Reverse
//        ),
//        label = "offsetY"
//    )
//
//    Image(
//        painter = painterResource(id = imageResId),
//        contentDescription = contentDescription,
//        modifier = modifier.offset(y = offsetY.dp) // Y軸方向にオフセットを適用
//    )
//}
//
//
//// 新しい血統画面の composable
//// @OptIn(ExperimentalFoundationApi::class) // HorizontalPagerが安定版になったため不要な場合は削除
//@Composable
//fun BreedSelectionScreen(
//    modifier: Modifier = Modifier,
//    onBreedSelected: (String) -> Unit
//) {
//    val breeds = listOf(
//        BreedData("アメリカンショートヘア", R.drawable.american_shorthair_placeholder),
//        BreedData("スコティッシュフォールド", R.drawable.scottish_fold_placeholder),
//        BreedData("ロシアンブルー", R.drawable.russian_blue_placeholder),
//        BreedData("マンチカン", R.drawable.munchkin_placeholder),
//        BreedData("三毛様", R.drawable.mikesama_placeholder)
//    )
//
//    val pagerState = rememberPagerState(pageCount = { breeds.size })
//
//    LaunchedEffect(pagerState) {
//        snapshotFlow { pagerState.currentPage }.collect { page ->
//            Log.d("BreedSelection", "Current page: ${breeds[page].name}")
//        }
//    }
//
//    Box( // ★ Boxで全体を囲み、背景画像を設定
//        modifier = modifier.fillMaxSize()
//    ) {
//        // 背景画像
//        Image(
//            painter = painterResource(id = R.drawable.cat_room_background_placeholder), // ★ 背景画像のリソースID (仮)
//            contentDescription = "Background",
//            contentScale = ContentScale.Crop, // 画像をどのように表示するか
//            modifier = Modifier
//                .fillMaxSize()
//                .then(
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                        Modifier.blur(
//                            radius = 16.dp,
//                            edgeTreatment = BlurredEdgeTreatment.Rectangle
//                        )
//                    } else {
//                        // Android 12未満の場合の代替 (例: 半透明オーバーレイ)
//                        Modifier.drawWithContent {
//                            drawContent()
//                            drawRect(Color.Black.copy(alpha = 0.30f)) // 15%くらいの黒など、調整してみてください
//                        }
//                    }
//                )
//        )
//
//        // 背景の上に重ねるコンテンツ
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(16.dp),
//            horizontalAlignment = Alignment.CenterHorizontally,
//        ) {
//            Text(
//                "血統を選んでにゃ！",
//                style = MaterialTheme.typography.headlineMedium,
//                color = Color.Black // ★ 背景によっては文字色を調整 (例: 黒)
//                // 必要であれば、テキストに影をつけるなども検討 (modifier = Modifier.shadow(...))
//            )
//            Spacer(modifier = Modifier.height(24.dp))
//
//            HorizontalPager(
//                state = pagerState,
//                modifier = Modifier
//                    .weight(1f)
//                    .fillMaxWidth(),
//                contentPadding = PaddingValues(horizontal = 32.dp)
//            ) { pageIndex ->
//                val breed = breeds[pageIndex]
//                Column(
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                    verticalArrangement = Arrangement.Center,
//                    modifier = Modifier.fillMaxSize()
//                ) {
//                    // ★ 上下アニメーションするキャラクター画像に変更
//                    AnimatedCharacterImage(
//                        imageResId = breed.imageResId,
//                        contentDescription = breed.name,
//                        modifier = Modifier
//                            .size(200.dp)
//                            .padding(8.dp)
//                    )
//                    Spacer(modifier = Modifier.height(16.dp))
//                    Text(
//                        text = breed.name,
//                        style = MaterialTheme.typography.titleLarge,
//                        textAlign = TextAlign.Center,
//                        color = Color.Black // ★ 背景によっては文字色を調整 (例: 黒)
//                    )
//                }
//            }
//
//            Spacer(modifier = Modifier.height(24.dp))
//
//            // ページインジケータ
//            Row(
//                Modifier
//                    .height(24.dp) // 少し高さを抑える
//                    .fillMaxWidth(),
//                horizontalArrangement = Arrangement.Center,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                repeat(breeds.size) { iteration ->
//                    val color =
//                        if (pagerState.currentPage == iteration) Color.DarkGray else Color.LightGray
//                    Box(
//                        modifier = Modifier
//                            .padding(horizontal = 4.dp) // 少し間隔をあける
//                            .clip(CircleShape)
//                            .background(color)
//                            .size(12.dp) // 少し小さくする
//                    )
//                }
//            }
//
//            Spacer(modifier = Modifier.height(16.dp)) // ボタンとの間隔
//
//            Button(
//                onClick = {
//                    val selectedBreedName = breeds[pagerState.currentPage].name
//                    Log.d("BreedSelection", "$selectedBreedName が選択されました")
//                    onBreedSelected(selectedBreedName)
//                },
//                modifier = Modifier.fillMaxWidth(0.8f)
//            ) {
//                Text("この子にする！")
//            }
//        }
//    }
//}
//
//@Preview(showBackground = true)
//@Composable
//fun BreedSelectionScreenPreview() {
//    CatQuestAppTheme {
//        BreedSelectionScreen(
//            onBreedSelected = { selectedBreed ->
//                Log.d("Preview", "Breed selected in preview: $selectedBreed")
//            }
//        )
//    }
//}
