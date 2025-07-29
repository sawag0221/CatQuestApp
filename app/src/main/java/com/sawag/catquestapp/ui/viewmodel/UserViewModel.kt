
////package com.sawag.catquestapp.ui.viewmodel
////
////import android.util.Log
////import androidx.lifecycle.ViewModel
////import androidx.lifecycle.viewModelScope
////import com.sawag.catquestapp.data.user.UserEntity
////import com.sawag.catquestapp.data.user.UserRepository
////import kotlinx.coroutines.flow.MutableStateFlow
////import kotlinx.coroutines.flow.StateFlow
////import kotlinx.coroutines.flow.asStateFlow
////import kotlinx.coroutines.flow.firstOrNull
////import kotlinx.coroutines.launch
////
////class UserViewModel(private val userRepository: UserRepository) : ViewModel() {
////
////    private val _user = MutableStateFlow<UserEntity?>(null)
////    val user: StateFlow<UserEntity?> = _user.asStateFlow()
////
////    init {
////        loadUser()
////    }
////
////    // UserViewModel.kt
////    private fun loadUser() {
////        viewModelScope.launch {
////            val existingUser = userRepository.getUser().firstOrNull() // Flowから最初の値を取得
////            if (existingUser == null) {
////                Log.d("UserViewModel", "初期ユーザーが存在しないため、新規作成します。")
////                val newUser = UserEntity() // デフォルトコンストラクタで初期ユーザー作成
////                try {
////                    userRepository.upsertUser(newUser)
////                    _user.value = newUser // 作成したユーザーをStateFlowに設定
////                    Log.i("UserViewModel", "初期ユーザー作成成功: ID=${newUser.id}")
////                } catch (e: Exception) {
////                    Log.e("UserViewModel", "初期ユーザー作成失敗", e)
////                }
////            } else {
////                Log.d("UserViewModel", "既存ユーザーをロードしました: ID=${existingUser.id}")
////                _user.value = existingUser
////            }
////
////            // (オプション) 既存ユーザーがロードされた後も、DBの変更を監視し続けたい場合
////            // ただし、上記で一度ロードしているので、重複してcollectすると複雑になる可能性も。
////            // 目的によっては、userRepository.getUser() を直接UI側でcollectする方が良い場合もある。
////            // 今回は初期ロードと初期作成にフォーカスする。
////            // userRepository.getUser().collect { updatedUser ->
////            //     _user.value = updatedUser
////            // }
////        }
////    }
////
////    // 経験値を加算するメソッド (戦闘後などに呼び出す)
////    // UserViewModel.kt - addXpメソッド内
////    fun addXp(amount: Int) {
////        viewModelScope.launch {
////            _user.value?.let { currentUser -> // (A) _user.value を currentUser として取得
////                val currentXp = currentUser.xp
////                val newXpLong = currentXp + amount.toLong()
////
////                Log.d("UserViewModel", "経験値獲得処理開始: 現在XP=$currentXp, 獲得XP=$amount, 新XP計算値=$newXpLong, ユーザーID=${currentUser.id}")
////
////                if (newXpLong >= currentUser.nextLevelXp) {
////                    Log.d("UserViewModel", "レベルアップ条件を満たしました。checkAndProcessLevelUpを呼び出します。")
////                    // ★ levelUp() の呼び出しを checkAndProcessLevelUp に変更
////                    // ★ また、checkAndProcessLevelUp に渡す UserEntity は、
////                    //    新しい経験値が反映されたものであるべき
////                    val userWithNewXp = currentUser.copy(xp = newXpLong)
////                    _user.value = userWithNewXp // UIにも先に新しい経験値を反映
////                    checkAndProcessLevelUp(userWithNewXp) // (B) 新しい経験値を持つユーザーでレベルアップチェック
////
////                    // 注意: checkAndProcessLevelUp が userRepository.levelUpUser を呼び出すと、
////                    // その結果として _user.value は getUser() の collect によって再度更新されるはず。
////                    // もし checkAndProcessLevelUp の中で _user.value を直接更新しているなら、
////                    // ここでの _user.value = userWithNewXp は不要かもしれないが、
////                    // レベルアップ前に経験値が増えた状態を一度UIに反映する意味はある。
////
////                } else {
////                    // UI反映用のStateFlowを更新
////                    _user.value = currentUser.copy(xp = newXpLong)
////                    try {
////                        userRepository.updateXp(newXp = newXpLong, userId = currentUser.id)
////                        Log.i("UserViewModel", "XPアップデート成功: ユーザーID=${currentUser.id}, 新XP=$newXpLong")
////                    } catch (e: Exception) {
////                        Log.e("UserViewModel", "XPアップデート失敗: ユーザーID=${currentUser.id}, 新XP=$newXpLong", e)
////                        _user.value = currentUser
////                    }
////                }
////            } ?: run {
////                Log.w("UserViewModel", "addXp呼び出し時、userデータがnullです。")
////            }
////        }
////    }
////
////    // レベルアップ条件をチェックし、必要ならレベルアップ処理を行うメソッド
////    // 引数として現在のユーザー状態を受け取るように変更
////    private fun checkAndProcessLevelUp(currentUser: UserEntity) {
////        viewModelScope.launch {
////            if (currentUser.xp >= currentUser.nextLevelXp) {
////                val newLevel = currentUser.level + 1
////                // 余剰経験値を計算
////                val surplusXp = currentUser.xp - currentUser.nextLevelXp
////
////                // --- ここから新しいステータスの計算 (具体的な計算式はゲームバランスによる) ---
////                val newMaxHp = currentUser.maxHp + (10..20).random() // 例: 10から20の間でランダムに上昇
////                val newHp = newMaxHp // HPは全回復
////
////                val newMaxMp = currentUser.maxMp + (5..15).random() // 例: 5から15の間でランダムに上昇
////                val newMp = newMaxMp // MPは全回復
////
////                val newAttack = currentUser.attackPower + (2..5).random() // 例: 2から5の間でランダムに上昇
////                val newDefense = currentUser.defensePower + (1..3).random() // 例: 1から3の間でランダムに上昇
////
////                // 次のレベルアップに必要な経験値を設定 (例: 現在の1.5倍)
////                val newNextLevelXp = (currentUser.nextLevelXp * 1.5).toLong()
////                // --- ここまで新しいステータスの計算 ---
////
////                userRepository.levelUpUser(
////                    newLevel = newLevel,
////                    newXp = surplusXp, // 余剰経験値を次のレベルの経験値として設定
////                    newNextLevelXp = newNextLevelXp,
////                    newHp = newHp,
////                    newMaxHp = newMaxHp,
////                    newMp = newMp,
////                    newMaxMp = newMaxMp,
////                    newAtk = newAttack,
////                    newDef = newDefense,
////                    userId = currentUser.id
////                )
////                // TODO: レベルアップしたことをユーザーに通知するUI処理 (例: Toast, Snackbar, ダイアログなど)
////                // 例: _levelUpEvent.value = true (LiveDataやSharedFlowでイベントを通知)
////                println("レベルアップ！ Lv $newLevel になった！") // ログで確認
////            }
////        }
////    }
////
////    // ダメージを受ける処理の例 (HP更新)
////
////    fun takeDamage(damage: Int) {
////        viewModelScope.launch {
////            _user.value?.let { currentUser ->
////                val newHp = (currentUser.hp - damage).coerceAtLeast(0) // HPが0未満にならないように
////
////                Log.d("UserViewModel", "ダメージ処理開始: 現在HP=${currentUser.hp}, ユーザーID=${currentUser.id}, ダメージ量=$damage, 新HP計算値=$newHp")
////
////                if (currentUser.hp == newHp) {
////                    Log.d("UserViewModel", "HPに変化なし。アップデートはスキップします。")
////                    return@launch
////                }
////
////                // UI反映用のStateFlowを先に更新 (任意だが、UIの即時反応のため)
////                // ただし、DB更新が失敗した場合のロールバックも考慮する必要が出てくる場合がある
////                // シンプルにするならDB更新後にUIを更新しても良い
////                _user.value = currentUser.copy(hp = newHp)
////
////                try {
////                    // ★ UserRepository の updateHp メソッドを呼び出す
////                    userRepository.updateHp(newHp = newHp, userId = currentUser.id)
////                    Log.i("UserViewModel", "HPアップデート成功: ユーザーID=${currentUser.id}, 新HP=$newHp")
////
////                    if (newHp == 0) {
////                        Log.w("UserViewModel", "プレイヤーHPが0になりました。ゲームオーバー処理を検討してください。")
////                        // TODO: HPが0になった場合のゲームオーバー処理などをここに追加
////                    }
////
////                } catch (e: Exception) {
////                    Log.e("UserViewModel", "HPアップデート失敗: ユーザーID=${currentUser.id}, 新HP=$newHp", e)
////                    // エラーハンドリング: UI上のHPを元に戻すなどの処理も検討
////                    _user.value = currentUser // 例: アップデート失敗時はUIを元の状態に戻す
////                }
////            } ?: run {
////                Log.w("UserViewModel", "takeDamage呼び出し時、userデータがnullです。")
////            }
////        }
////    }
////
////
////            // MPを消費する処理の例
////    fun consumeMp(cost: Int): Boolean { // 消費できたかどうかの結果を返す
////        var success = false
////        viewModelScope.launch {
////            val currentUser = userRepository.getUser().firstOrNull()
////            currentUser?.let { user ->
////                if (user.mp >= cost) {
////                    val newMp = user.mp - cost
////                    userRepository.updateMp(newMp, user.id)
////                    success = true
////                } else {
////                    // TODO: MPが足りない場合の処理
////                    println("MPが足りない！")
////                    success = false
////                }
////            }
////        }.invokeOnCompletion {
////            // このブロックはコルーチン完了後に実行される (必要に応じて)
////        }
////        // 注意: この関数の戻り値はコルーチン完了前に評価されるため、
////        // 即座に消費結果を知りたい場合はコールバックやFlowを使う方が良い
////        return success // この return はコルーチン内の success の値を直接は返さない
////    }
////
////
////    // ユーザーデータを初期化または特定の値に設定する (デバッグ用など)
////    fun resetOrSetUserData(userEntity: UserEntity) {
////        viewModelScope.launch {
////            userRepository.upsertUser(userEntity)
////        }
////    }
////}
//
//package com.sawag.catquestapp.ui.viewmodel
//
//import android.util.Log
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.sawag.catquestapp.data.user.UserEntity // シンプル版 (id, name のみ)
//import com.sawag.catquestapp.data.user.UserRepository
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.flow.firstOrNull // getUser() の戻り値が Flow<UserEntity?> の場合
//import kotlinx.coroutines.launch
//
//class UserViewModel(private val userRepository: UserRepository) : ViewModel() {
//
//    private val _user = MutableStateFlow<UserEntity?>(null)
//    val user: StateFlow<UserEntity?> = _user.asStateFlow()
//
//    private val userIdToLoad = 1 // 仮にデフォルトユーザーのIDを1とする
//
//    init {
//        Log.d("UserViewModel", "ViewModel initialized. Attempting to load user with ID: $userIdToLoad")
//        loadOrCreateUser(userIdToLoad)
//    }
//
//    private fun loadOrCreateUser(userId: Int) {
//        viewModelScope.launch {
//            // userRepository.getUser(userId) は Flow<UserEntity?> を返すと仮定
//            val existingUser = userRepository.getUser(userId).firstOrNull() // Flowから最初の値を取得
//
//            if (existingUser == null) {
//                Log.d("UserViewModel", "User with ID $userId not found. Creating a new default user.")
//                // ★ シンプルなUserEntityを生成 (idは自動生成されるので指定しないことが多いが、
//                //    特定のIDで作りたい場合は upsertUser の実装による)
//                //    ここではupsertUserがID=0で新規作成し、DBが自動採番すると仮定。
//                //    もし特定のID (例: userIdToLoad) で固定したい場合は、
//                //    UserEntity(id = userId, name = "Hero") のようにするが、
//                //    PrimaryKey(autoGenerate = true) の場合は id=0 で渡すのが一般的。
//                val newUser = UserEntity(name = "Hero") // デフォルト名
//                try {
//                    userRepository.upsertUser(newUser) // 新規ユーザーを挿入
//                    // 挿入後、実際にDBから再取得してIDが確定したユーザーをFlowに流すのがより確実
//                    // ここでは簡略化のため、upsertUserがIDを確定してくれると期待
//                    // ただし、autoGenerate = true の場合、newUserのIDは0のまま。
//                    // DBから再取得するか、upsertUserが挿入したIDを返す設計にする必要がある。
//                    // 最も簡単なのは、再度 getUser(userId) を呼び出すこと。
//                    // しかし、初期ユーザーのIDが固定でない場合は、別の方法で初期ユーザーを特定する必要がある。
//
//                    // 【改善案】初期ユーザーが存在しない場合、ID=1として作成し、それをロードする
//                    val defaultUserToCreate = UserEntity(id = userIdToLoad, name = "Hero") // IDを指定して作成
//                    userRepository.upsertUser(defaultUserToCreate)
//                    _user.value = defaultUserToCreate // 作成したユーザーをStateFlowに設定
//                    Log.i("UserViewModel", "Default user created and loaded: ${defaultUserToCreate.name} (ID: ${defaultUserToCreate.id})")
//
//                } catch (e: Exception) {
//                    Log.e("UserViewModel", "Failed to create default user.", e)
//                }
//            } else {
//                _user.value = existingUser
//                Log.i("UserViewModel", "Existing user loaded: ${existingUser.name} (ID: ${existingUser.id})")
//            }
//
//            // (オプション) 既存ユーザーがロードされた後も、DBの変更を監視し続けたい場合
//            // この場合、loadOrCreateUser の最初で collect を開始し、
//            // existingUser のチェックはその collect の中で行う
//            // 例:
//            // userRepository.getUser(userId).collect { userFromDb ->
//            //     if (userFromDb == null && !_user.value?.id == userId) { // まだ初期ユーザー作成前の場合
//            //         // 初期ユーザー作成ロジック
//            //     } else {
//            //         _user.value = userFromDb
//            //     }
//            // }
//            // 今回は初期ロード/作成のみにフォーカス
//        }
//    }
//
//    // ★★★ UserEntityがシンプルになったため、以下のメソッドは大幅な変更または削除が必要 ★★★
//
//    // // 経験値を加算するメソッド (戦闘後などに呼び出す)
//    // fun addXp(amount: Int) { ... } // xp, nextLevelXp がないので削除または大幅変更
//
//    // // レベルアップ条件をチェックし、必要ならレベルアップ処理を行うメソッド
//    // private fun checkAndProcessLevelUp(currentUser: UserEntity) { ... } // level, xp, nextLevelXp などがないので削除
//
//    // // ダメージを受ける処理の例 (HP更新)
//    // fun takeDamage(damage: Int) { ... } // hp がないので削除
//
//    // // MPを消費する処理の例
//    // fun consumeMp(cost: Int): Boolean { ... } // mp がないので削除
//
//
//    // ユーザー名を変更する例 (シンプルなUserEntityに合わせて)
//    fun updateUserName(newName: String) {
//        viewModelScope.launch {
//            _user.value?.let { currentUser ->
//                if (currentUser.name == newName) {
//                    Log.d("UserViewModel", "New name is the same as the current name. No update needed.")
//                    return@launch
//                }
//                val updatedUser = currentUser.copy(name = newName)
//                try {
//                    userRepository.upsertUser(updatedUser) // upsertUserで更新
//                    _user.value = updatedUser // UIにも反映
//                    Log.i("UserViewModel", "User name updated successfully to: $newName for ID: ${currentUser.id}")
//                } catch (e: Exception) {
//                    Log.e("UserViewModel", "Failed to update user name for ID: ${currentUser.id}", e)
//                    // エラーハンドリング: 必要ならUIを元の状態に戻す
//                    // _user.value = currentUser
//                }
//            } ?: run {
//                Log.w("UserViewModel", "Cannot update user name, current user is null.")
//            }
//        }
//    }
//
//
//    // ユーザーデータを初期化または特定の値に設定する (デバッグ用など)
//    // シンプルなUserEntityをupsertする形に
//    fun resetOrSetUserData(userEntity: UserEntity) {
//        viewModelScope.launch {
//            try {
//                userRepository.upsertUser(userEntity)
//                // 必要であれば、upsert後に _user.value も更新
//                if (_user.value?.id == userEntity.id || userEntity.id == userIdToLoad) { // 更新対象が現在のユーザー、またはデフォルトユーザーの場合
//                    _user.value = userEntity
//                }
//                Log.i("UserViewModel", "User data reset/set successfully for ID: ${userEntity.id}")
//            } catch (e: Exception) {
//                Log.e("UserViewModel", "Failed to reset/set user data for ID: ${userEntity.id}", e)
//            }
//        }
//    }
//}
//

// UserViewModel.kt
package com.sawag.catquestapp.ui.viewmodel // パッケージ名は適宜変更してください

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sawag.catquestapp.data.user.UserEntity
import com.sawag.catquestapp.data.user.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull // ★ firstOrNull を使うためにインポート
import kotlinx.coroutines.launch

// ViewModelに渡すUIの状態を表すクラス
sealed interface UserUiState {
    object Loading : UserUiState
    data class Success(val user: UserEntity) : UserUiState
    data class Error(val message: String) : UserUiState
}

class UserViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Loading)
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    // ユーザーエンティティそのものを公開したい場合はこちらも用意 (任意)
    private val _user = MutableStateFlow<UserEntity?>(null)
    val user: StateFlow<UserEntity?> = _user.asStateFlow()

    companion object {
        private const val TAG = "UserViewModel"
    }

    init {
        Log.d(TAG, "init: Initializing UserViewModel")
        loadOrCreateUser()
    }

    private fun loadOrCreateUser() {
        viewModelScope.launch {
            _uiState.value = UserUiState.Loading // 処理開始時にLoading状態にする
            Log.d(TAG, "loadOrCreateUser: Attempting to load or create user...")
            try {
                // 1. DBから最初のユーザーを取得試行 (コールバックで作成されたはず)
                var currentUser = userRepository.getAllUsers().firstOrNull()?.firstOrNull() // Flow<List<User>> -> List<User>? -> User?

                if (currentUser != null) {
                    Log.d(TAG, "loadOrCreateUser: Found existing user from DB: ${currentUser.name} (ID: ${currentUser.id})")
                } else {
                    Log.w(TAG, "loadOrCreateUser: No user found in DB. Likely callback didn't run or DB was empty. Creating a fallback user 'Hero'.")
                    // 2. 存在しない場合は新しいフォールバックユーザー "Hero" を作成して保存
                    val fallbackUserName = "Hero"
                    val fallbackUserEntity = UserEntity(name = fallbackUserName) // idは自動生成
                    userRepository.addUser(fallbackUserEntity) // DBに保存

                    // 保存後、再度取得を試みる (IDが振られ、確実に存在するか確認)
                    // 名前で検索する機能がDaoにあればそれを使うのが確実だが、なければ再度getAllUsersから探す
                    currentUser = userRepository.getAllUsers().firstOrNull()?.find { it.name == fallbackUserName }

                    if (currentUser != null) {
                        Log.d(TAG, "loadOrCreateUser: Successfully created and retrieved fallback user: ${currentUser.name} (ID: ${currentUser.id})")
                    } else {
                        Log.e(TAG, "loadOrCreateUser: CRITICAL - Failed to retrieve fallback user even after attempting to create it!")
                        _uiState.value = UserUiState.Error("Failed to initialize user data. Could not create fallback.")
                        _user.value = null
                        return@launch // これ以上処理を進めない
                    }
                }

                // ユーザーが見つかった場合 (コールバックから or フォールバック作成)
                _user.value = currentUser
                _uiState.value = UserUiState.Success(currentUser) // currentUserはここでnullでないはず
                Log.d(TAG, "loadOrCreateUser: User set in ViewModel: ${currentUser.name}")

            } catch (e: Exception) {
                Log.e(TAG, "loadOrCreateUser: Error loading or creating user", e)
                _uiState.value = UserUiState.Error(e.message ?: "Unknown error during user initialization")
                _user.value = null
            }
        }
    }

    // 必要に応じてユーザー情報を更新するメソッドなどを追加
    //例: fun updateUserName(newName: String) { ... }
}

// ViewModelFactory (UserViewModelにUserRepositoryを渡すため)
class UserViewModelFactory(private val userRepository: UserRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserViewModel(userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

