package com.sawag.catquestapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sawag.catquestapp.ui.theme.CatQuestAppTheme // 独自のテーマ
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// --- ViewModel (戦闘ロジックと状態管理) ---
data class PlayerCharacterState(

    val name: String = "ミケ",
    var level: Int = 1,                 // ★追加: 現在のレベル
    var currentExperience: Int = 0,     // ★追加: 現在の累積経験値
    var currentHp: Int = 50,
    var maxHp: Int = 50,                // レベルアップで変動
    var attackPower: Int = 10,          // レベルアップで変動
    var defensePower: Int = 5,          // ★追加例: 防御力 (レベルアップで変動)
    // var magicPower: Int = 5,         // ★追加例: 魔力
    // val learnedSkills: MutableList<String> = mutableListOf(), // ★追加例: 習得特技
    val imageResId: Int = R.drawable.mikesama_placeholder

)

data class EnemyCharacterState(
    val name: String = "スライム",
    var currentHp: Int = 30,
    val maxHp: Int = 30,
    val attackPower: Int = 5,
    val experiencePoint: Int = 10,     // ★追加: 倒した時の獲得経験値
    // val gold: Int = 5,              // ★追加例: 倒した時の獲得ゴールド
    val imageResId: Int = R.drawable.ic_enemy_slime_placeholder // プレースホルダー画像
)

data class CombatScreenUiState(
    val currentFloor: Int = 1,
    val dungeonName: String = "最初の洞窟",
    val player: PlayerCharacterState = PlayerCharacterState(),
    val enemy: EnemyCharacterState? = EnemyCharacterState(), // 初期はnullでも良い
    val messageLog: List<String> = listOf("スライムがあらわれた！"),
    val isPlayerTurn: Boolean = true,
    val availableCommands: List<String> = listOf("たたかう", "ぼうぎょ", "にげる"),
    val showSubCommands: Boolean = false, // 例: 「とくぎ」選択後の特技リスト表示用
    val subCommandList: List<String> = emptyList(),
    val combatResultText: String? = null // "勝利！", "敗北..."
)

class CombatViewModel : ViewModel() {
//    現在の状態値を保持し、その値が変更されるとオブザーバー（監視しているもの）に通知する仕組みです (リアクティブプログラミングの一種)。
    private val _uiState = MutableStateFlow(CombatScreenUiState())
    val uiState: StateFlow<CombatScreenUiState> = _uiState.asStateFlow()

    private val messageLogInternal = mutableStateListOf<String>()

    init {
        // ViewModel初期化時に最初のメッセージを追加
        if (messageLogInternal.isEmpty()) {
            messageLogInternal.add("${_uiState.value.enemy?.name ?: "敵"}があらわれた！")
            _uiState.update { it.copy(messageLog = messageLogInternal.toList()) }
        }
    }


    fun initializeBattle(dungeonId: String, playerBreedName: String) {
        // 本来はdungeonIdやplayerBreedNameに応じてプレイヤーや敵のステータスを設定する
        // ここでは固定値で初期化
        val initialPlayer = PlayerCharacterState(
            name = playerBreedName,
            maxHp = 60,
            currentHp = 60,
            attackPower = 12
        )
        val initialEnemy =
            EnemyCharacterState(name = "ゴブリン", maxHp = 40, currentHp = 40, attackPower = 8)

        messageLogInternal.clear()
        messageLogInternal.add("${initialEnemy.name}があらわれた！")

        _uiState.update {
            it.copy(
                dungeonName = if (dungeonId == "first_cave") "最初の洞窟" else "謎のダンジョン",
                currentFloor = 1,
                player = initialPlayer,
                enemy = initialEnemy,
                messageLog = messageLogInternal.toList(),
                isPlayerTurn = true,
                combatResultText = null
            )
        }
    }


    fun onCommandSelected(command: String) {
        if (!_uiState.value.isPlayerTurn || _uiState.value.combatResultText != null) return

        when (command) {
            "たたかう" -> performPlayerAttack()
            "ぼうぎょ" -> performPlayerDefend()
            "にげる" -> attemptRunAway()
            // "とくぎ" -> showSkillList() // 特技選択へ
            else -> addMessageToLog("$command はまだつかえない！")
        }
    }

    private fun performPlayerAttack() {
        val player = _uiState.value.player
        var enemy = _uiState.value.enemy ?: return

        val damage = player.attackPower // 簡単のため固定ダメージ
        enemy = enemy.copy(currentHp = (enemy.currentHp - damage).coerceAtLeast(0))
        addMessageToLog("${player.name}のこうげき！")
        addMessageToLog("${enemy.name}に $damage のダメージ！")

        _uiState.update { it.copy(enemy = enemy) }

        if (enemy.currentHp <= 0) {
            winBattle()
        } else {
            endPlayerTurn()
        }
    }

    private fun performPlayerDefend() {
        addMessageToLog("${_uiState.value.player.name}はみをまもっている！")
        // 防御状態フラグを立てるなどの処理 (ここでは省略)
        endPlayerTurn()
    }

    private fun attemptRunAway() {
        // 50%の確率で成功する例
        if (Math.random() < 0.5) {
            addMessageToLog("${_uiState.value.player.name}はうまくにげだした！")
            _uiState.update { it.copy(combatResultText = "逃走成功") }
            // ここで onRunAway コールバックを呼び出す
        } else {
            addMessageToLog("しかし回り込まれてしまった！")
            endPlayerTurn()
        }
    }


    private fun endPlayerTurn() {
        _uiState.update { it.copy(isPlayerTurn = false) }
        // ここで敵のターン処理を遅延実行する (例: kotlinx.coroutines.delay)
        // 今回はUIデモなので省略し、手動で敵ターン開始を模倣
        // performEnemyAttack() // すぐに敵の攻撃が始まる場合はこちら
        addMessageToLog("（敵のターンを待っています...）") // UIデモ用
    }

    // デモ用に外部から敵のターンを開始する関数
    fun startEnemyTurnDemo() {
        if (_uiState.value.isPlayerTurn || _uiState.value.combatResultText != null) return
        performEnemyAttack()
    }


    private fun performEnemyAttack() {
        val enemy = _uiState.value.enemy ?: return
        var player = _uiState.value.player

        val damage = enemy.attackPower
        player = player.copy(currentHp = (player.currentHp - damage).coerceAtLeast(0))
        addMessageToLog("${enemy.name}のこうげき！")
        addMessageToLog("${player.name}は $damage のダメージをうけた！")

        _uiState.update { it.copy(player = player) }

        if (player.currentHp <= 0) {
            loseBattle()
        } else {
            _uiState.update { it.copy(isPlayerTurn = true) }
        }
    }
    // CombatViewModel 内

    private fun winBattle() {
        val enemy = _uiState.value.enemy ?: return
        var player = _uiState.value.player

        addMessageToLog("${enemy.name}をたおした！")

        // 経験値獲得
        val gainedExp = enemy.experiencePoint
        player = player.copy(currentExperience = player.currentExperience + gainedExp)
        addMessageToLog("${player.name}は $gainedExp のけいけんちをかくとく！")

        _uiState.update { it.copy(player = player) } // 先に経験値加算をUIに反映

        // レベルアップチェック
        var leveledUp = false
        var nextLevelExp = ExperienceTable.getExperienceForLevel(player.level + 1)

        while (player.currentExperience >= nextLevelExp && player.level < ExperienceTable.getMaxLevel()) {
            leveledUp = true
            val oldLevel = player.level
            val newLevel = oldLevel + 1

            // ステータス更新
            val newMaxHp = PlayerStatsGrowth.getMaxHpForLevel(newLevel)
            val newAttack = PlayerStatsGrowth.getAttackPowerForLevel(newLevel)
            val newDefense = PlayerStatsGrowth.getDefensePowerForLevel(newLevel)

            player = player.copy(
                level = newLevel,
                maxHp = newMaxHp,
                currentHp = newMaxHp, // HP全回復
                attackPower = newAttack,
                defensePower = newDefense
            )

            addMessageToLog("${player.name}はレベル ${newLevel} にあがった！")
            addMessageToLog("さいだいHPが ${newMaxHp - PlayerStatsGrowth.getMaxHpForLevel(oldLevel)} あがった！") // 差分を表示
            addMessageToLog(
                "こうげきが ${
                    newAttack - PlayerStatsGrowth.getAttackPowerForLevel(
                        oldLevel
                    )
                } あがった！"
            )
            // 他のステータス上昇メッセージも

            _uiState.update { it.copy(player = player) } // レベルアップごとにUI更新

            // 次のレベルアップ判定のために更新
            nextLevelExp = ExperienceTable.getExperienceForLevel(newLevel + 1)
        }


        if (leveledUp) {
            // レベルアップ後の最終状態をUIに反映（ループ内で更新済みだが念のため）
            _uiState.update { it.copy(player = player) }
        }

        addMessageToLog("${_uiState.value.player.name}はしょうりした！")
        _uiState.update { it.copy(combatResultText = "勝利！") }
    }

    private fun loseBattle() {
        addMessageToLog("${_uiState.value.player.name}はたおれてしまった...")
        _uiState.update { it.copy(combatResultText = "敗北...") }
    }

    private fun addMessageToLog(message: String) {
        messageLogInternal.add(message)
        _uiState.update { it.copy(messageLog = messageLogInternal.toList()) }
    }
}


// --- Composable ---
@Composable
fun CombatScreen(
    dungeonId: String,
    playerBreedName: String,
    onCombatEnd: () -> Unit,
    onRunAway: () -> Unit,
    viewModel: CombatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 画面表示時に戦闘初期化
    LaunchedEffect(dungeonId, playerBreedName) {
        viewModel.initializeBattle(dungeonId, playerBreedName)
    }


    CatQuestAppTheme { // アプリのテーマを適用
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black // 背景を黒に
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp) // 全体のパディング
            ) {
                // 上部: ダンジョン情報と敵グラフィック
                DungeonHeader(
                    dungeonName = uiState.dungeonName,
                    floor = uiState.currentFloor
                )
                EnemyArea(enemy = uiState.enemy)

                // 中部: メッセージウィンドウ
                MessageWindow(
                    messages = uiState.messageLog,
                    modifier = Modifier.weight(1f) // << ★ ここで weight を指定
                )

                // 下部: プレイヤーステータスとコマンドウィンドウ
                PlayerStatusAndCommands(
                    player = uiState.player,
                    commands = uiState.availableCommands,
                    onCommandSelected = viewModel::onCommandSelected,
                    enabled = uiState.isPlayerTurn && uiState.combatResultText == null,
                    combatResultText = uiState.combatResultText,
                    onResultDismiss = {
                        if (uiState.combatResultText == "逃走成功") onRunAway() else onCombatEnd()
                    },
                    // デモ用：敵のターン開始ボタン
                    onStartEnemyTurnDemo = if (!uiState.isPlayerTurn && uiState.combatResultText == null) {
                        { viewModel.startEnemyTurnDemo() }
                    } else null
                )
            }
        }
    }
}

@Composable
fun DungeonHeader(dungeonName: String, floor: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color.DarkGray.copy(alpha = 0.5f)) // 半透明の背景
            .border(BorderStroke(1.dp, Color.White))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$dungeonName B${floor}F",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EnemyArea(enemy: EnemyCharacterState?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp) // 敵の表示領域の高さ
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (enemy != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = enemy.imageResId),
                    contentDescription = enemy.name,
                    modifier = Modifier.size(120.dp), // 敵画像のサイズ
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(enemy.name, color = Color.White, fontSize = 16.sp)
                // HPバー (オプション)
                LinearProgressIndicator(
                    progress = { enemy.currentHp.toFloat() / enemy.maxHp.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(8.dp)
                        .clip(MaterialTheme.shapes.small),
                    color = Color.Red,
                    trackColor = Color.DarkGray,
                    strokeCap = StrokeCap.Round
                )
            }
        } else {
            Text("敵がいません", color = Color.Gray)
        }
    }
}

@Composable
fun MessageWindow(
    messages: List<String>,
    modifier: Modifier = Modifier // << ★ 呼び出し側から Modifier を受け取る引数を追加
) {
    val listState = rememberLazyListState()

    // 新しいメッセージが追加されたら一番下までスクロール
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = modifier // << ★ 受け取った modifier を適用
            .fillMaxWidth()
            // .weight(1f) // << ★ ここから削除
            .background(Color.Black)
            .border(BorderStroke(2.dp, Color.White))
            .padding(8.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize() // << ★ Box内でLazyColumnが全域を占めるように
        ) {
            items(messages) { message ->
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}


@Composable
fun PlayerStatusAndCommands(
    player: PlayerCharacterState,
    commands: List<String>,
    onCommandSelected: (String) -> Unit,
    enabled: Boolean,
    combatResultText: String?,
    onResultDismiss: () -> Unit,
    onStartEnemyTurnDemo: (() -> Unit)? // デモ用
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp) // プレイヤーステータスとコマンドウィンドウの高さ
            .padding(top = 8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // 左側: プレイヤーステータスウィンドウ
        PlayerStatusWindow(player = player, modifier = Modifier.weight(0.4f))

        Spacer(modifier = Modifier.width(8.dp))

        // 右側: コマンドウィンドウ
        if (combatResultText != null) {
            CombatResultWindow(
                resultText = combatResultText,
                onDismiss = onResultDismiss,
                modifier = Modifier.weight(0.6f)
            )
        } else {
            CommandWindow(
                commands = commands,
                onCommandSelected = onCommandSelected,
                enabled = enabled,
                onStartEnemyTurnDemo = onStartEnemyTurnDemo,
                modifier = Modifier.weight(0.6f)
            )
        }
    }
}

@Composable
fun PlayerStatusWindow(player: PlayerCharacterState, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxHeight(),
        border = BorderStroke(1.dp, Color.White),
        shape = MaterialTheme.shapes.medium, // 角を少し丸める
        colors = CardDefaults.cardColors(containerColor = Color.Blue.copy(alpha = 0.7f)) // ドラクエ風の青いウィンドウ
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceAround // 要素を均等に配置
        ) {
            Text(player.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row {
                Text("HP:", color = Color.White, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "${player.currentHp} / ${player.maxHp}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
//             MP表示などもここに追加可能
            LinearProgressIndicator(
                progress = { player.currentHp.toFloat() / player.maxHp.toFloat() },
                color = Color.Green,
                trackColor = Color.DarkGray.copy(alpha = 0.5f),
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
            )
//             プレイヤー画像 (オプション)
            Image(painter = painterResource(id = player.imageResId), contentDescription = "Player")
        }
    }
}

@Composable
fun CommandWindow(
    commands: List<String>,
    onCommandSelected: (String) -> Unit,
    enabled: Boolean,
    onStartEnemyTurnDemo: (() -> Unit)?, // デモ用
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxHeight(),
        border = BorderStroke(1.dp, Color.White),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceEvenly // ボタンを均等に配置
        ) {
            commands.chunked(2).forEach { rowCommands -> // 2列にする例
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    rowCommands.forEach { command ->
                        Button(
                            onClick = { onCommandSelected(command) },
                            enabled = enabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.DarkGray,
                                contentColor = Color.White,
                                disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
                            ),
                            border = BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Text(command, fontSize = 14.sp)
                        }
                    }
                }
            }
            // デモ用に敵のターンを開始するボタン (本来は不要)
            if (onStartEnemyTurnDemo != null) {
                Button(
                    onClick = onStartEnemyTurnDemo,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Magenta)
                ) {
                    Text("敵ターン開始(デモ)", fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun CombatResultWindow(
    resultText: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxHeight(),
        border = BorderStroke(1.dp, Color.White),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(resultText, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    }
}


// --- Preview ---
@Preview(showBackground = true, widthDp = 380, heightDp = 780)
@Composable
fun CombatScreenPreview() {
    // ViewModelのモックを作成するか、プレビュー用のダミーデータでUIを確認
    val previewViewModel = remember {
        val vm = CombatViewModel()
        // 必要に応じてvmの初期状態をプレビュー用に設定
        vm
    }
    // ダミーのコールバック
    val onCombatEnd = { println("戦闘終了プレビュー") }
    val onRunAway = { println("逃走プレビュー") }

    CombatScreen(
        dungeonId = "preview_cave",
        playerBreedName = "プレビュー猫",
        onCombatEnd = onCombatEnd,
        onRunAway = onRunAway,
        viewModel = previewViewModel
    )
}

@Preview
@Composable
fun MessageWindowPreview() {
    CatQuestAppTheme {
        Surface(color = Color.Black) {
            MessageWindow(
                messages = listOf(
                    "スライムがあらわれた！",
                    "プレビュー猫のこうげき！",
                    "スライムに10のダメージ！",
                    "スライムのこうげき！",
                    "プレビュー猫は5のダメージをうけた！"
                )
            )
        }
    }
}

@Preview
@Composable
fun PlayerStatusWindowPreview() {
    CatQuestAppTheme {
        PlayerStatusWindow(player = PlayerCharacterState(name = "タマ", currentHp = 25, maxHp = 50))
    }
}

@Preview
@Composable
fun CommandWindowPreview() {
    CatQuestAppTheme {
        CommandWindow(
            commands = listOf("たたかう", "ぼうぎょ", "とくぎ", "どうぐ", "にげる"),
            onCommandSelected = {},
            enabled = true,
            onStartEnemyTurnDemo = {}
        )
    }
}
