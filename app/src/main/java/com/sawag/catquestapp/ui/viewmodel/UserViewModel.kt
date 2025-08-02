package com.sawag.catquestapp.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sawag.catquestapp.data.user.UserEntity
import com.sawag.catquestapp.data.user.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
// import kotlinx.coroutines.flow.firstOrNull // 現状のコードでは未使用
import kotlinx.coroutines.launch

// ViewModelに渡すUIの状態を表すクラス
sealed class UserUiState { // sealed class に変更 (interfaceでも可)
    object Loading : UserUiState()
    data class Success(val user: UserEntity) : UserUiState()
    data class Error(val message: String) : UserUiState()
}

class UserViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Loading)
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    // ユーザーエンティティそのものを公開したい場合はこちらも用意
    // currentUserとして公開し、uiStateがSuccessの時に値を持ち、それ以外はnullになるようにする
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow() // ★ 名前を currentUser に変更

    companion object {
        private const val TAG = "UserViewModel"
        private const val DEFAULT_USER_ID: Int = 1 // ★ DEFAULT_USER_ID を定義 (Int型)

        // ViewModelProvider.Factory を提供するメソッド
        fun provideFactory(
            userRepository: UserRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
                        return UserViewModel(userRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }

    init {
        Log.d(TAG, "init: Initializing UserViewModel with user ID: $DEFAULT_USER_ID")
        loadUser(DEFAULT_USER_ID) // ★ loadOrCreateUser から loadUser に変更 (名前をより明確に)
    }

    // IDを指定してユーザーをロードする関数
    private fun loadUser(userId: Int) {
        viewModelScope.launch {
            _uiState.value = UserUiState.Loading
            _currentUser.value = null // ローディング開始時に currentUser もクリア
            Log.d(TAG, "loadUser: Attempting to load user with ID $userId...")
            try {
                // userRepository.getUserById は Flow<UserEntity?> を返す想定
                userRepository.getUserById(userId).collect { userFromDb ->
                    if (userFromDb != null) {
                        Log.d(TAG, "loadUser: User found: ID=${userFromDb.id}, Breed=${userFromDb.breed}, Lvl=${userFromDb.level}")
                        _uiState.value = UserUiState.Success(userFromDb)
                        _currentUser.value = userFromDb // ★ currentUser にもセット
                    } else {
                        Log.w(TAG, "loadUser: User with ID $userId NOT found. This might indicate an issue with initial data population or wrong ID.")
                        // AppDatabaseCallback で初期ユーザーが作られるはずなので、
                        // ここでユーザーが存在しない場合は、明確なエラーとして扱うか、
                        // または特定のシナリオ (例: 初回起動でまだ何も選択していない状態) を示す別のStateを定義するか検討
                        _uiState.value = UserUiState.Error("User with ID $userId not found. Database might be empty or ID is incorrect.")
                        _currentUser.value = null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadUser: Error loading user with ID $userId", e)
                _uiState.value = UserUiState.Error(e.message ?: "Unknown error during user loading")
                _currentUser.value = null
            }
        }
    }

    // 血統を選択・更新する関数
    fun selectBreedAndUpdateUser(breedName: String) {
        viewModelScope.launch {
            val userToUpdate = _currentUser.value // 現在ロードされているユーザー情報を取得
            if (userToUpdate != null) {
                // すでに同じ血統が設定されているか、または初期状態("null")でなければ更新
                if (userToUpdate.breed == "null" || userToUpdate.breed != breedName) {
                    Log.d(TAG, "selectBreedAndUpdateUser: Updating breed for user ${userToUpdate.id} from '${userToUpdate.breed}' to '$breedName'")
                    try {
                        userRepository.updateUserBreed(userToUpdate.id, breedName)
                        // DB更新後、UserDaoのgetUserByIdがFlowなので、
                        // loadUser 内の collect が自動的に最新のユーザー情報を検知し、_uiState と _currentUser が更新されるはず。
                        Log.i(TAG, "selectBreedAndUpdateUser: Breed update request sent for user ${userToUpdate.id}. UI should refresh via Flow.")
                        // 必要であれば、ここで明示的に再ロードをトリガーすることも可能だが、Flowの動作に任せるのが理想。
                        // loadUser(userToUpdate.id) // もしFlowで自動更新されない場合のフォールバック
                    } catch (e: Exception) {
                        Log.e(TAG, "selectBreedAndUpdateUser: Error updating breed for user ${userToUpdate.id}", e)
                        // エラーが発生した場合、UIにエラーメッセージを表示する
                        // (現在の_uiStateがSuccessのままなので、エラー用のStateを発行するか検討)
                        // 例: _uiState.value = UserUiState.Error("Failed to update breed: ${e.message}")
                        // ただし、この操作が失敗してもユーザーデータ自体はまだ読み込めている状態なので、
                        // UI全体をエラー状態にするかは設計次第。一時的なトースト表示なども考えられる。
                    }
                } else {
                    Log.d(TAG, "selectBreedAndUpdateUser: Breed '$breedName' is already set for user ${userToUpdate.id}. No update needed.")
                }
            } else {
                Log.e(TAG, "selectBreedAndUpdateUser: Cannot update breed, current user is null. User might not be loaded yet.")
                // ユーザーがロードされていない状態でこの関数が呼ばれるのは通常予期しない
                // _uiState を Error にすることも検討
                // _uiState.value = UserUiState.Error("Cannot select breed: User data not loaded.")
            }
        }
    }

    // 他のユーザー情報更新メソッドのプレースホルダー
    fun gainExperience(xp: Int) {
        viewModelScope.launch {
            _currentUser.value?.let { user ->
                val updatedUser = user.copy(experiencePoints = user.experiencePoints + xp)
                // userRepository.updateUser(updatedUser) // UserRepositoryに更新メソッドが必要
                // レベルアップロジックなどもここに追加
                Log.d(TAG, "User ${user.id} gained $xp XP. New XP: ${updatedUser.experiencePoints}")
            }
        }
    }

    fun spendCatCoins(amount: Int) {
        viewModelScope.launch {
            _currentUser.value?.let { user ->
                if (user.catCoins >= amount) {
                    val updatedUser = user.copy(catCoins = user.catCoins - amount)
                    // userRepository.updateUser(updatedUser)
                    Log.d(TAG, "User ${user.id} spent $amount cat coins. Remaining: ${updatedUser.catCoins}")
                } else {
                    Log.w(TAG, "User ${user.id} does not have enough cat coins to spend $amount.")
                    // UIに通知する処理など
                }
            }
        }
    }
}

// ViewModelFactory はViewModelファイルの外 (例: ApplicationクラスやDIモジュール) に置くか、
// 上記のように companion object 内の provideFactory メソッドで提供するのが一般的です。
// ここでは、provideFactory を使っているので、独立した UserViewModelFactory クラスは削除しても良いでしょう。
// もし残す場合は、このファイル内ではなく、別のファイルに配置することを検討してください。

class UserViewModelFactory(private val userRepository: UserRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserViewModel(userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


