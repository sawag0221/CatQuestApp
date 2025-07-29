//package com.sawag.catquestapp.ui // ★パッケージ宣言をuiにする
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import com.sawag.catquestapp.data.user.UserRepository
//import com.sawag.catquestapp.ui.viewmodel.UserViewModel // UserViewModelのパスを確認
//
//class UserViewModelFactory(
//    private val userRepository: UserRepository
//) : ViewModelProvider.Factory {
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
//            @Suppress("UNCHECKED_CAST")
//            return UserViewModel(userRepository) as T
//        }
//        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
//    }
//}
//
