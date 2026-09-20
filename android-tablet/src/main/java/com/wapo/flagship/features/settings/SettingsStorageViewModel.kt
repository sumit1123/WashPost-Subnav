package com.wapo.flagship.features.settings

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.flagship.data.ArchiveManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsStorageViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {
    enum class DeleteState {
        START,
        DONE,
    }

    private val _deleteState = MutableLiveData<DeleteState>()
    val deleteState: LiveData<DeleteState> = _deleteState

    fun deletePrintFiles() {
        viewModelScope.launch {
            _deleteState.value = DeleteState.START
            withContext(Dispatchers.IO) {
                ArchiveManager.deleteArchiveFiles(context)
            }
            _deleteState.value = DeleteState.DONE
        }
    }
}
