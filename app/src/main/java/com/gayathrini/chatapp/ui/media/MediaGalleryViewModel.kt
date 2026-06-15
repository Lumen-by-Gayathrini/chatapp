package com.gayathrini.chatapp.ui.media

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gayathrini.chatapp.core.common.AppResult
import com.gayathrini.chatapp.core.navigation.Screen
import com.gayathrini.chatapp.data.messages.MessageRepository
import com.gayathrini.chatapp.domain.model.Message
import com.gayathrini.chatapp.ui.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MediaGalleryUiState(
    val isLoading: Boolean = true,
    val images: List<Message> = emptyList(),
    val files: List<Message> = emptyList(),
    val error: String? = null,
)

/** Conversation media gallery (TDD §6.16): images grid + files list. */
@HiltViewModel
class MediaGalleryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val messageRepository: MessageRepository,
) : ViewModel() {

    private val conversationId: String = checkNotNull(savedStateHandle[Screen.MediaGallery.ARG])

    private val _state = MutableStateFlow(MediaGalleryUiState())
    val state: StateFlow<MediaGalleryUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val images = messageRepository.conversationMedia(conversationId, "IMAGE")
            val files = messageRepository.conversationMedia(conversationId, "FILE")
            _state.update {
                it.copy(
                    isLoading = false,
                    images = (images as? AppResult.Success)?.data ?: it.images,
                    files = (files as? AppResult.Success)?.data ?: it.files,
                    error = if (images is AppResult.Failure && files is AppResult.Failure) {
                        images.error.toUserMessage()
                    } else {
                        null
                    },
                )
            }
        }
    }
}
