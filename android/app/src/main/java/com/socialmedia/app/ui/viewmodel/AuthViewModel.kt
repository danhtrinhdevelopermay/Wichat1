package com.socialmedia.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialmedia.app.data.api.RetrofitClient
import com.socialmedia.app.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val user: User? = null,
    val error: String? = null
)

class AuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState
    
    companion object {
        private const val TAG = "AuthViewModel"
    }
    
    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "Vui lòng nhập tên đăng nhập và mật khẩu"
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                Log.d(TAG, "Attempting login for user: $username")
                val response = RetrofitClient.apiService.login(
                    LoginRequest(username, password)
                )
                
                Log.d(TAG, "Login response code: ${response.code()}")
                
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    Log.d(TAG, "Login successful for user: ${authResponse.user.username}")
                    RetrofitClient.setAuthToken(authResponse.token)
                    _uiState.value = AuthUiState(
                        isAuthenticated = true,
                        user = authResponse.user
                    )
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Login failed: ${response.code()} - $errorBody")
                    
                    val errorMessage = when (response.code()) {
                        401 -> "Tên đăng nhập hoặc mật khẩu không đúng"
                        404 -> "Tài khoản không tồn tại"
                        500 -> "Lỗi server, vui lòng thử lại sau"
                        else -> "Đăng nhập thất bại (${response.code()})"
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = errorMessage
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Login error: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Lỗi kết nối: ${e.localizedMessage ?: "Không thể kết nối đến server"}"
                )
            }
        }
    }
    
    fun register(username: String, email: String, password: String, displayName: String) {
        if (username.isBlank() || email.isBlank() || password.isBlank() || displayName.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "Vui lòng điền đầy đủ thông tin"
            )
            return
        }
        
        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(
                error = "Mật khẩu phải có ít nhất 6 ký tự"
            )
            return
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = _uiState.value.copy(
                error = "Email không hợp lệ"
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                Log.d(TAG, "Attempting registration for user: $username")
                val response = RetrofitClient.apiService.register(
                    RegisterRequest(username, email, password, displayName)
                )
                
                Log.d(TAG, "Register response code: ${response.code()}")
                
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    Log.d(TAG, "Registration successful for user: ${authResponse.user.username}")
                    RetrofitClient.setAuthToken(authResponse.token)
                    _uiState.value = AuthUiState(
                        isAuthenticated = true,
                        user = authResponse.user
                    )
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Registration failed: ${response.code()} - $errorBody")
                    
                    val errorMessage = when (response.code()) {
                        400 -> "Thông tin đăng ký không hợp lệ"
                        409 -> "Tên đăng nhập hoặc email đã tồn tại"
                        500 -> "Lỗi server, vui lòng thử lại sau"
                        else -> "Đăng ký thất bại (${response.code()})"
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = errorMessage
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Registration error: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Lỗi kết nối: ${e.localizedMessage ?: "Không thể kết nối đến server"}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun logout() {
        RetrofitClient.clearAuthToken()
        _uiState.value = AuthUiState()
    }
}
