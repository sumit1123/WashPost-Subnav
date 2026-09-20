package com.wapo.flagship.features.audio.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.LiveEvent
import com.wapo.flagship.features.audio.R
import com.wapo.flagship.features.audio.playlist.Playlist
import com.wapo.flagship.features.audio.playlist.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistActivityViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _playlistFlow = MutableStateFlow<List<Playlist>>(emptyList())
    val playlist: LiveData<List<Playlist>> = _playlistFlow.flatMapLatest {
        playlistRepository.getPlaylist()
    }.asLiveData()

    private val _playListVisibilityState = MutableLiveData(false)
    val playListVisibilityState: LiveData<Boolean>  = _playListVisibilityState

    private val _addedToPlaylistEvent = LiveEvent<Any>()
    val addedToPlaylistEvent: LiveData<Any> = _addedToPlaylistEvent

    val _playlistClickTrackEvent = LiveEvent<Boolean>()
    val playlistClickTrackEvent: LiveData<Boolean> = _playlistClickTrackEvent

    val _playlistCtaClickEvent = LiveEvent<Boolean>()
    val playlistCtaClickEvent: LiveData<Boolean> = _playlistCtaClickEvent

    private val _sharedLockedHeight = MutableLiveData<Float?>(null)
    val sharedLockedHeight: LiveData<Float?> = _sharedLockedHeight

    fun setSharedLockedHeight(height: Float?) {
        if (_sharedLockedHeight.value != height) {
            _sharedLockedHeight.value = height
        }
    }

    fun addToPlaylist(playlistAudio: Playlist?) {
        playlistAudio ?: return
        viewModelScope.launch {
            if (!playlistRepository.addPlaylistAudio(playlistAudio)) {
                AppContextUtils.toastError(R.string.unable_to_add)
                return@launch
            }
            _addedToPlaylistEvent.postValue(Any())
        }
    }

    fun removeFromPlaylist(playlistAudio: Playlist?) {
        playlistAudio ?: return
        viewModelScope.launch {
            playlistRepository.removePlaylistAudio(playlistAudio)
        }
    }

    fun removePlaylistByIdAudio(id: String?) {
        id ?: return
        viewModelScope.launch {
            playlistRepository.removePlaylistByIdAudio(id)
        }
    }

    suspend fun getPlaylistFromDatabase(id: String) : Boolean {
        return playlistRepository.getPlaylistArticleExists(id)
    }

    fun clearPlaylist() {
        viewModelScope.launch {
            playlistRepository.clearPlaylist()
        }
    }

    fun setPlayListVisibility(isVisible: Boolean) {
        _playListVisibilityState.value = isVisible
    }

    fun togglePlayListVisibility() {
        val isVisible = _playListVisibilityState.value ?: return
        _playListVisibilityState.value = !isVisible
    }

}
