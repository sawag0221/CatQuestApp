package com.sawag.catquestapp.ui.viewmodel // あなたのViewModelのパッケージ

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sawag.catquestapp.data.user.UserEntity
import com.sawag.catquestapp.data.user.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// UserUiState の定義 (まだなければ)
sealed interface UserUiState {
    data class Success(val user: UserEntity) : UserUiState
    data class Error(val message: String) : UserUiState
    object Loading : UserUiState
}

class UserViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Loading)
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    // currentUser を外部から参照できるようにする場合 (読み取り専用)
    val currentUser: StateFlow<UserEntity?> = uiState
        .map { (it as? UserUiState.Success)?.user }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        loadCurrentUser() // ViewModel 初期化時にユーザーを読み込む
    }

    // ★★★ このメソッドを実装 ★★★
    fun loadCurrentUser() {
        viewModelScope.launch {
            _uiState.value = UserUiState.Loading
            try {
                // ID 1 のユーザーをデフォルトとして読み込む
                // userRepository.getUserById(1) は Flow<UserEntity?> を返す
                val user = userRepository.getUserById(1).firstOrNull() // Flow から最初の値を取得 (または null)

                if (user != null) {
                    _uiState.value = UserUiState.Success(user)
                } else {
                    // ユーザーが見つからない場合、デフォルトの "プレイヤー" という名前でユーザーを作成
                    Log.d("UserViewModel", "No user found with ID 1, creating default user.")
                    // ★★★ UserEntity の実際のプロパティ名に合わせて修正 ★★★
                    val defaultUser = UserEntity(
                        id = 1,                      // UserEntity のプロパティ名は 'id'
                        name = "プレイヤー",             // UserEntity のプロパティ名は 'name'
                        level = 1,                   // UserEntity のプロパティ名は 'level'
                        breed = "null",              // UserEntity のプロパティ名は 'breed', 初期値は "null" 文字列
                        experiencePoints = 0,        // UserEntity のプロパティ名は 'experiencePoints'
                        catCoins = 0                 // UserEntity のプロパティ名は 'catCoins'
                    )
                    userRepository.insertUser(defaultUser) // UserRepository.insertUser を使用
                    _uiState.value = UserUiState.Success(defaultUser)
                }
            } catch (e: Exception) {
                val errorMessage = "ユーザー情報の読み込みに失敗: ${e.message}"
                _uiState.value = UserUiState.Error(errorMessage)
                Log.e("UserViewModel", errorMessage, e)
            }
        }
    }

    fun updateUserNameAndSignalCompletion(newName: String, onResult: (success: Boolean) -> Unit) {
        viewModelScope.launch {
            // uiState から最新のユーザー情報を取得するのが望ましい
            val currentUserFromState = (_uiState.value as? UserUiState.Success)?.user

            // もし uiState にまだユーザー情報がない場合 (例: loadCurrentUser が完了する前など)、
            // または何らかの理由で取得できなかった場合、DBから直接取得を試みる (フォールバック)
            // ★★★ 呼び出しを UserRepository のメソッドに合わせる ★★★
            val userToUpdate = currentUserFromState ?: userRepository.getUserById(1).firstOrNull()


            if (userToUpdate != null) {
                // ★ UserEntity の name フィールドを更新
                val updatedUser = userToUpdate.copy(name = newName)
                try {
                    // ★ UserRepository の updateUser メソッドを使用
                    userRepository.updateUser(updatedUser)
                    // 更新成功後、最新のユーザー情報を再読み込みして uiState を更新
                    loadCurrentUser() // これにより他の画面のユーザー情報も最新になる
                    onResult(true)
                } catch (e: Exception) {
                    val errorMessage = "名前の更新に失敗: ${e.message}"
                    Log.e("UserViewModel", errorMessage, e)
                    _uiState.value = UserUiState.Error(errorMessage)
                    onResult(false)
                }
            } else {
                Log.e("UserViewModel", "User not found (ID: 1), cannot update name.")
                onResult(false)
            }
        }
    }

    // 猫種を選択し、ユーザー情報を更新するメソッドの例
    fun selectBreedAndUpdateUser(breedName: String, onResult: (success: Boolean) -> Unit) {
        viewModelScope.launch {
            val currentUserFromState = (_uiState.value as? UserUiState.Success)?.user
            val userToUpdate = currentUserFromState ?: userRepository.getUserById(1).firstOrNull()

            if (userToUpdate != null) {
                try {
                    // ★ UserRepository の updateUserBreed メソッドを使用
                    // userRepository.updateUserBreed(userId = 1, newBreed = breedName)
                    // または、UserEntity全体を更新するなら
                    val updatedUserWithBreed = userToUpdate.copy(breed = breedName)
                    userRepository.updateUser(updatedUserWithBreed)

                    loadCurrentUser() // UI状態を更新
                    onResult(true)
                } catch (e: Exception) {
                    val errorMessage = "猫種の更新に失敗: ${e.message}"
                    Log.e("UserViewModel", errorMessage, e)
                    _uiState.value = UserUiState.Error(errorMessage)
                    onResult(false)
                }
            } else {
                Log.e("UserViewModel", "User not found (ID: 1), cannot update breed.")
                onResult(false)
            }
        }
    }


    companion object {
        fun provideFactory(
            userRepository: UserRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
                    return UserViewModel(userRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
