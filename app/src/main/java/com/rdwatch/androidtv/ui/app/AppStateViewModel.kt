package com.rdwatch.androidtv.ui.app

import androidx.lifecycle.viewModelScope
import com.rdwatch.androidtv.presentation.viewmodel.BaseViewModel
import com.rdwatch.androidtv.di.IoDispatcher
import com.rdwatch.androidtv.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Global app state management ViewModel for coordinating state across screens
 * Handles cross-screen concerns like:
 * - User authentication state
 * - App-wide preferences and settings
 * - Navigation state coordination
 * - Global loading and error states
 * - Background task coordination
 */
@HiltViewModel
class AppStateViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BaseViewModel<AppUiState>() {
    
    // Navigation state tracking
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()
    
    private val _navigationHistory = MutableStateFlow<List<Screen>>(listOf(Screen.Home))
    val navigationHistory: StateFlow<List<Screen>> = _navigationHistory.asStateFlow()
    
    // Authentication state coordination
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    
    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()
    
    // Global app preferences
    private val _appPreferences = MutableStateFlow(AppPreferences())
    val appPreferences: StateFlow<AppPreferences> = _appPreferences.asStateFlow()
    
    // Background tasks and global loading states
    private val _backgroundTasks = MutableStateFlow<Set<BackgroundTask>>(emptySet())
    val backgroundTasks: StateFlow<Set<BackgroundTask>> = _backgroundTasks.asStateFlow()
    
    // Network connectivity state
    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()
    
    // Global notifications/messages
    private val _globalMessages = MutableStateFlow<List<AppMessage>>(emptyList())
    val globalMessages: StateFlow<List<AppMessage>> = _globalMessages.asStateFlow()
    
    override fun createInitialState(): AppUiState {
        return AppUiState()
    }
    
    init {
        // Initialize app state
        initializeAppState()
        observeStateChanges()
    }
    
    // Navigation state management
    fun onNavigateTo(screen: Screen) {
        val currentHistory = _navigationHistory.value.toMutableList()
        currentHistory.add(screen)
        
        // Limit navigation history to prevent memory issues
        if (currentHistory.size > 20) {
            currentHistory.removeAt(0)
        }
        
        _currentScreen.value = screen
        _navigationHistory.value = currentHistory
        
        updateState { 
            copy(
                currentScreen = screen,
                navigationHistory = currentHistory
            )
        }
    }
    
    fun onNavigateBack() {
        val currentHistory = _navigationHistory.value.toMutableList()
        if (currentHistory.isNotEmpty()) {
            currentHistory.removeLastOrNull()
            val previousScreen = currentHistory.lastOrNull() ?: Screen.Home
            
            _currentScreen.value = previousScreen
            _navigationHistory.value = currentHistory
            
            updateState { 
                copy(
                    currentScreen = previousScreen,
                    navigationHistory = currentHistory
                )
            }
        }
    }
    
    fun clearNavigationHistory() {
        _navigationHistory.value = listOf(Screen.Home)
        _currentScreen.value = Screen.Home
        
        updateState { 
            copy(
                currentScreen = Screen.Home,
                navigationHistory = listOf(Screen.Home)
            )
        }
    }
    
    // Authentication state management
    fun onUserAuthenticated(userId: String) {
        _isAuthenticated.value = true
        _userId.value = userId
        
        updateState { 
            copy(
                isAuthenticated = true,
                userId = userId
            )
        }
        
        // Load user-specific preferences
        loadUserPreferences(userId)
    }
    
    fun onUserSignedOut() {
        _isAuthenticated.value = false
        _userId.value = null
        
        updateState { 
            copy(
                isAuthenticated = false,
                userId = null,
                userPreferences = UserPreferences()
            )
        }
        
        // Clear user-specific data
        clearUserData()
        clearNavigationHistory()
    }
    
    // App preferences management
    fun updateAppPreferences(updater: (AppPreferences) -> AppPreferences) {
        val newPreferences = updater(_appPreferences.value)
        _appPreferences.value = newPreferences
        
        updateState { copy(appPreferences = newPreferences) }
        
        // Persist preferences
        saveAppPreferences(newPreferences)
    }
    
    fun updateUserPreferences(updater: (UserPreferences) -> UserPreferences) {
        val currentUserId = _userId.value ?: return
        val currentUserPrefs = uiState.value.userPreferences
        val newPreferences = updater(currentUserPrefs)
        
        updateState { copy(userPreferences = newPreferences) }
        
        // Persist user preferences
        saveUserPreferences(currentUserId, newPreferences)
    }
    
    // Background task management
    fun startBackgroundTask(task: BackgroundTask) {
        val currentTasks = _backgroundTasks.value.toMutableSet()
        currentTasks.add(task)
        _backgroundTasks.value = currentTasks
        
        updateState { 
            copy(
                backgroundTasks = currentTasks,
                hasBackgroundActivity = currentTasks.isNotEmpty()
            )
        }
    }
    
    fun completeBackgroundTask(taskId: String) {
        val currentTasks = _backgroundTasks.value.toMutableSet()
        currentTasks.removeAll { it.id == taskId }
        _backgroundTasks.value = currentTasks
        
        updateState { 
            copy(
                backgroundTasks = currentTasks,
                hasBackgroundActivity = currentTasks.isNotEmpty()
            )
        }
    }
    
    // Network state management
    fun onNetworkStateChanged(isAvailable: Boolean) {
        _isNetworkAvailable.value = isAvailable
        
        updateState { copy(isNetworkAvailable = isAvailable) }
        
        if (!isAvailable) {
            showGlobalMessage(
                AppMessage(
                    id = "network_disconnected",
                    message = "Network connection lost",
                    type = MessageType.WARNING,
                    duration = MessageDuration.INDEFINITE
                )
            )
        } else {
            dismissGlobalMessage("network_disconnected")
        }
    }
    
    // Global message management
    fun showGlobalMessage(message: AppMessage) {
        val currentMessages = _globalMessages.value.toMutableList()
        
        // Remove existing message with same ID if it exists
        currentMessages.removeAll { it.id == message.id }
        currentMessages.add(message)
        
        _globalMessages.value = currentMessages
        updateState { copy(globalMessages = currentMessages) }
        
        // Auto-dismiss if duration is specified
        if (message.duration != MessageDuration.INDEFINITE) {
            viewModelScope.launch {
                kotlinx.coroutines.delay(message.duration.millis)
                dismissGlobalMessage(message.id)
            }
        }
    }
    
    fun dismissGlobalMessage(messageId: String) {
        val currentMessages = _globalMessages.value.toMutableList()
        currentMessages.removeAll { it.id == messageId }
        
        _globalMessages.value = currentMessages
        updateState { copy(globalMessages = currentMessages) }
    }
    
    // Private helper methods
    private fun initializeAppState() {
        viewModelScope.launch(ioDispatcher) {
            // Load saved app preferences
            loadAppPreferences()
            
            // Check authentication state
            checkAuthenticationState()
            
            // Initialize network monitoring
            initializeNetworkMonitoring()
        }
    }
    
    private fun observeStateChanges() {
        // Combine all relevant state flows for comprehensive state updates
        viewModelScope.launch {
            combine(
                _currentScreen,
                _isAuthenticated,
                _appPreferences,
                _backgroundTasks,
                _isNetworkAvailable,
                _globalMessages
            ) { values ->
                val currentScreen = values[0] as Screen
                val isAuth = values[1] as Boolean
                val prefs = values[2] as AppPreferences
                val tasks = values[3] as Set<BackgroundTask>
                val network = values[4] as Boolean
                val messages = values[5] as List<AppMessage>
                
                updateState { 
                    copy(
                        currentScreen = currentScreen,
                        isAuthenticated = isAuth,
                        appPreferences = prefs,
                        backgroundTasks = tasks,
                        hasBackgroundActivity = tasks.isNotEmpty(),
                        isNetworkAvailable = network,
                        globalMessages = messages
                    )
                }
            }.collect()
        }
    }
    
    private fun loadAppPreferences() {
        // TODO: Load from DataStore or SharedPreferences
        // For now, use defaults
    }
    
    private fun saveAppPreferences(preferences: AppPreferences) {
        viewModelScope.launch(ioDispatcher) {
            // TODO: Save to DataStore or SharedPreferences
        }
    }
    
    private fun loadUserPreferences(userId: String) {
        viewModelScope.launch(ioDispatcher) {
            // TODO: Load user-specific preferences from database
        }
    }
    
    private fun saveUserPreferences(userId: String, preferences: UserPreferences) {
        viewModelScope.launch(ioDispatcher) {
            // TODO: Save user preferences to database
        }
    }
    
    private fun checkAuthenticationState() {
        // TODO: Check stored authentication state
        // For now, assume not authenticated
        _isAuthenticated.value = false
    }
    
    private fun clearUserData() {
        // Clear any user-specific cached data
        updateState { copy(userPreferences = UserPreferences()) }
    }
    
    private fun initializeNetworkMonitoring() {
        // TODO: Initialize network connectivity monitoring
        // For now, assume network is available
        _isNetworkAvailable.value = true
    }
}

// Data classes for app state
data class AppUiState(
    val currentScreen: Screen = Screen.Home,
    val navigationHistory: List<Screen> = listOf(Screen.Home),
    val isAuthenticated: Boolean = false,
    val userId: String? = null,
    val appPreferences: AppPreferences = AppPreferences(),
    val userPreferences: UserPreferences = UserPreferences(),
    val backgroundTasks: Set<BackgroundTask> = emptySet(),
    val hasBackgroundActivity: Boolean = false,
    val isNetworkAvailable: Boolean = true,
    val globalMessages: List<AppMessage> = emptyList()
)

data class AppPreferences(
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: String = "en",
    val autoPlay: Boolean = true,
    val dataUsageMode: DataUsageMode = DataUsageMode.NORMAL,
    val analyticsEnabled: Boolean = true,
    val crashReportingEnabled: Boolean = true
)

data class UserPreferences(
    val playbackSpeed: Float = 1.0f,
    val subtitlesEnabled: Boolean = false,
    val subtitleLanguage: String = "en",
    val videoQuality: VideoQuality = VideoQuality.AUTO,
    val watchlistSyncEnabled: Boolean = true,
    val continueWatchingEnabled: Boolean = true
)

data class BackgroundTask(
    val id: String,
    val name: String,
    val progress: Float = 0f,
    val status: TaskStatus = TaskStatus.RUNNING
)

data class AppMessage(
    val id: String,
    val message: String,
    val type: MessageType,
    val duration: MessageDuration = MessageDuration.MEDIUM,
    val action: MessageAction? = null
)

data class MessageAction(
    val label: String,
    val action: () -> Unit
)

enum class AppTheme {
    LIGHT, DARK, SYSTEM
}

enum class DataUsageMode {
    LOW, NORMAL, HIGH
}

enum class VideoQuality {
    LOW, MEDIUM, HIGH, AUTO
}

enum class TaskStatus {
    RUNNING, COMPLETED, FAILED, CANCELLED
}

enum class MessageType {
    INFO, SUCCESS, WARNING, ERROR
}

enum class MessageDuration(val millis: Long) {
    SHORT(3000),
    MEDIUM(5000),
    LONG(8000),
    INDEFINITE(-1)
}