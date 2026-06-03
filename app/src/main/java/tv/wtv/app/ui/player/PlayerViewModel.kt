package tv.wtv.app.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tv.wtv.app.data.chat.ChatWebSocket
import tv.wtv.app.data.model.ChatMessage
import tv.wtv.app.data.model.StreamInfo
import tv.wtv.app.data.repository.StreamRepository

sealed interface PlayerUiState {
    data object Loading : PlayerUiState
    data class Playing(
        val streamInfo: StreamInfo,
        val availableQualities: List<String>,
        val selectedQuality: String,
        val isChatVisible: Boolean,
        val isQualityPickerVisible: Boolean,
        val isMetadataVisible: Boolean,
        val chatMessages: List<ChatMessage>,
    ) : PlayerUiState
    data class Error(val message: String) : PlayerUiState
}

class PlayerViewModel(
    private val repository: StreamRepository = StreamRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val chatMessages = mutableListOf<ChatMessage>()
    private var chatWebSocket: ChatWebSocket? = null
    private var metadataHideJob: Job? = null

    fun load(slug: String) {
        viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading
            try {
                val info = repository.loadStreamInfo(slug)
                chatMessages.clear()
                _uiState.value = PlayerUiState.Playing(
                    streamInfo = info,
                    availableQualities = listOf("Auto"),
                    selectedQuality = "Auto",
                    isChatVisible = true,
                    isQualityPickerVisible = false,
                    isMetadataVisible = true,
                    chatMessages = emptyList(),
                )
                scheduleMetadataHide()
                launch { repository.joinStream(info.streamId) }
                launch { connectChat(info.channelId) }
            } catch (e: Exception) {
                _uiState.value = PlayerUiState.Error(e.message ?: "Failed to load stream")
            }
        }
    }

    private suspend fun connectChat(channelId: String) {
        try {
            val backlog = repository.getChatBacklog(channelId)
            chatMessages.addAll(backlog)
            emitChatMessages()

            chatWebSocket?.close()
            val token = repository.getChatToken(channelId)
            chatWebSocket = ChatWebSocket(
                scope = viewModelScope,
                token = token,
                onMessage = { msg ->
                    chatMessages.add(0, msg)
                    if (chatMessages.size > 200) chatMessages.removeAt(chatMessages.size - 1)
                    emitChatMessages()
                },
                onError = {},
            )
            chatWebSocket?.connect()
        } catch (_: Exception) {
            // chat failure is non-fatal; stream continues playing
        }
    }

    private fun emitChatMessages() {
        val current = _uiState.value as? PlayerUiState.Playing ?: return
        _uiState.value = current.copy(chatMessages = chatMessages.toList())
    }

    fun setAvailableQualities(qualities: List<String>) {
        val current = _uiState.value as? PlayerUiState.Playing ?: return
        val selected = if (current.selectedQuality in qualities) current.selectedQuality
                       else qualities.firstOrNull() ?: "Auto"
        _uiState.value = current.copy(availableQualities = qualities, selectedQuality = selected)
    }

    fun selectQuality(quality: String) {
        val current = _uiState.value as? PlayerUiState.Playing ?: return
        _uiState.value = current.copy(selectedQuality = quality, isQualityPickerVisible = false)
    }

    fun toggleChat() {
        val current = _uiState.value as? PlayerUiState.Playing ?: return
        _uiState.value = current.copy(isChatVisible = !current.isChatVisible)
    }

    fun toggleQualityPicker() {
        val current = _uiState.value as? PlayerUiState.Playing ?: return
        _uiState.value = current.copy(isQualityPickerVisible = !current.isQualityPickerVisible)
    }

    fun showMetadata() {
        val current = _uiState.value as? PlayerUiState.Playing ?: return
        _uiState.value = current.copy(isMetadataVisible = true)
        scheduleMetadataHide()
    }

    private fun scheduleMetadataHide() {
        metadataHideJob?.cancel()
        metadataHideJob = viewModelScope.launch {
            delay(4_000)
            (_uiState.value as? PlayerUiState.Playing)?.let {
                _uiState.value = it.copy(isMetadataVisible = false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatWebSocket?.close()
    }
}
