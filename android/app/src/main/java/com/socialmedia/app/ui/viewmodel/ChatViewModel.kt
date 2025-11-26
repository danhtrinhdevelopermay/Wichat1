package com.socialmedia.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialmedia.app.data.api.RetrofitClient
import com.socialmedia.app.data.model.*
import com.socialmedia.app.data.websocket.WebSocketManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users
    
    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages
    
    private val webSocketManager = WebSocketManager()
    private var searchJob: Job? = null
    
    fun loadUsers() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getUsers()
                if (response.isSuccessful && response.body() != null) {
                    _users.value = response.body()!!
                }
            } catch (e: Exception) {
                println("Error loading users: ${e.message}")
            }
        }
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        
        _isSearching.value = true
        searchJob = viewModelScope.launch {
            delay(300)
            searchUsers(query)
        }
    }
    
    private suspend fun searchUsers(query: String) {
        try {
            val response = RetrofitClient.apiService.searchUsers(query)
            if (response.isSuccessful && response.body() != null) {
                _searchResults.value = response.body()!!
            } else {
                val allUsers = _users.value
                _searchResults.value = allUsers.filter { user ->
                    user.username.contains(query, ignoreCase = true) ||
                    user.displayName.contains(query, ignoreCase = true) ||
                    user.email.contains(query, ignoreCase = true)
                }
            }
        } catch (e: Exception) {
            val allUsers = _users.value
            _searchResults.value = allUsers.filter { user ->
                user.username.contains(query, ignoreCase = true) ||
                user.displayName.contains(query, ignoreCase = true) ||
                user.email.contains(query, ignoreCase = true)
            }
        } finally {
            _isSearching.value = false
        }
    }
    
    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _isSearching.value = false
    }
    
    fun loadMessages(userId: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getMessages(userId)
                if (response.isSuccessful && response.body() != null) {
                    _messages.value = response.body()!!
                }
            } catch (e: Exception) {
                println("Error loading messages: ${e.message}")
            }
        }
    }
    
    fun sendMessage(recipientId: Int, content: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.sendMessage(
                    SendMessageRequest(recipientId, content)
                )
                if (response.isSuccessful && response.body() != null) {
                    val newMessage = response.body()!!
                    _messages.value = _messages.value + newMessage
                    
                    webSocketManager.sendMessage(
                        ChatMessage(
                            type = "message",
                            senderId = newMessage.senderId,
                            senderName = newMessage.senderName,
                            recipientId = recipientId,
                            content = content
                        )
                    )
                }
            } catch (e: Exception) {
                println("Error sending message: ${e.message}")
            }
        }
    }
    
    fun connectWebSocket(userId: Int) {
        webSocketManager.connect(userId, "user$userId") { message ->
            viewModelScope.launch {
                _messages.value = _messages.value + Message(
                    id = 0,
                    senderId = message.senderId,
                    senderName = message.senderName,
                    recipientId = userId,
                    content = message.content ?: "",
                    createdAt = System.currentTimeMillis().toString()
                )
            }
        }
    }
    
    fun disconnectWebSocket() {
        webSocketManager.disconnect()
    }
    
    override fun onCleared() {
        super.onCleared()
        webSocketManager.disconnect()
    }
}
