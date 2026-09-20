import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SharedScrollViewModel : ViewModel() {
    private val _shouldScrollToPlaylist = MutableStateFlow(false)
    val shouldScrollToPlaylist: StateFlow<Boolean> = _shouldScrollToPlaylist

    fun triggerScrollToPlaylist() {
        _shouldScrollToPlaylist.value = true
    }

    fun clearScrollRequest() {
        _shouldScrollToPlaylist.value = false
    }
}