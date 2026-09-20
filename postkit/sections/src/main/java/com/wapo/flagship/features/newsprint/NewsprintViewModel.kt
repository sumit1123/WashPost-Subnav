package com.wapo.flagship.features.newsprint

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.wapo.flagship.features.grid.model.HomepageStory
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NewsprintViewModel @Inject constructor() : ViewModel() {

    private val _engagedStatus = MutableLiveData("")
    val engagedStatus: LiveData<String> get() = _engagedStatus

    private val _hasCompletedNewsprint = MutableLiveData(false)
    val hasCompletedNewsprint: LiveData<Boolean> get() = _hasCompletedNewsprint

    private val _readerType = MutableLiveData<NewsprintReaderType?>(null)
    val readerType: LiveData<NewsprintReaderType?> get() = _readerType

    private val _topCardState = MutableLiveData(NewsprintState.LOW_ENGAGED)
    val topCardState: LiveData<NewsprintState> get() = _topCardState

    private val _imageUrl = MutableLiveData(IMAGE_2024)
    val imageUrl: LiveData<String> get() = _imageUrl

    private val _videoUrl = MutableLiveData(VIDEO_2024)
    val videoUrl: LiveData<String> get() = _videoUrl

    fun updateData(engagedStatus: String, hasCompletedNewsprint: Boolean, readerTypeId: String) {
        _engagedStatus.value = engagedStatus
        _hasCompletedNewsprint.value = hasCompletedNewsprint
        _readerType.value = NewsprintReaderType.findById(readerTypeId)
        determineTopCardState()
        determineImageUrl()
        determineVideoUrl()
    }

    private fun determineTopCardState() {
        _topCardState.value = when {
            hasCompletedNewsprint.value == true && readerType.value != null -> NewsprintState.COMPLETED_NEWSPRINT
            engagedStatus.value == HIGH_ENGAGED && readerType.value != null -> NewsprintState.HIGH_ENGAGED
            else -> NewsprintState.LOW_ENGAGED
        }
    }

    private fun determineImageUrl() {
        _imageUrl.value = if (topCardState.value == NewsprintState.COMPLETED_NEWSPRINT) {
            readerType.value?.imageUrl ?: IMAGE_2024
        } else {
            IMAGE_2024
        }
    }

    private fun determineVideoUrl() {
        _videoUrl.value = if (topCardState.value == NewsprintState.COMPLETED_NEWSPRINT) {
            readerType.value?.videoUrl ?: VIDEO_2024
        } else {
            VIDEO_2024
        }
    }

    fun shouldShowNewsprintBadge(homepageStory: HomepageStory): Boolean {
        return homepageStory.isNewsprint
                && hasCompletedNewsprint.value == true
                && homepageStory.headline?.text != null
                && homepageStory.headline.text == readerType.value?.displayName
    }

    companion object {
        const val HIGH_ENGAGED = "high"

        private const val BASE_IMAGE_URL = "https://d1i4t8bqe7zgj6.cloudfront.net/07-26-2024/"
        private const val BASE_VIDEO_URL = "https://d21rhj7n383afu.cloudfront.net/washpost-production/Ray_Dak_Lam_The_Washington_Post/20240725/"
        private const val VIDEO_SIZE = "file_854x480-1200-v3_1.mp4"

        const val VIDEO_2024 = "${BASE_VIDEO_URL}66a29ad3368cb00bdd9a7e52/66a3c604d9dfd01cdcfb5d22/$VIDEO_SIZE"
        const val VIDEO_OWL = "${BASE_VIDEO_URL}66a29a1f368cb00bdd9a7de2/66a3e400d9dfd01cdcfb5d67/$VIDEO_SIZE"
        const val VIDEO_DOG = "${BASE_VIDEO_URL}66a29a61509c0d7bb257b8ff/66a3c71cd9dfd01cdcfb5d2c/$VIDEO_SIZE"
        const val VIDEO_OCTOPUS = "${BASE_VIDEO_URL}66a29a02509c0d7bb257b7b8/66a3e48eb711eb5ff5a15e90/$VIDEO_SIZE"
        const val VIDEO_CHEETAH = "${BASE_VIDEO_URL}66a29a45509c0d7bb257b876/66a3e41f5007d0611153bb5c/$VIDEO_SIZE"
        const val VIDEO_MONKEY = "${BASE_VIDEO_URL}66a29a85fdae0105876d3311/66a3e42d5007d0611153bb5d/$VIDEO_SIZE"
        const val VIDEO_SQUIRREL = "${BASE_VIDEO_URL}66a298bafdae0105876d3159/66a3e4095007d0611153bb53/$VIDEO_SIZE"

        const val IMAGE_2024 = "${BASE_IMAGE_URL}t_79ebc55da1934e86af31c958e80aee68_name_LOGO_ANIMATION_FINAL__0_00_03_08_.jpg"
        const val IMAGE_OWL = "${BASE_IMAGE_URL}t_75119a2cdb3949a699a1e170ef7f5134_name_ReaderType_Intellectual__0_00_00_00_.jpg"
        const val IMAGE_DOG = "${BASE_IMAGE_URL}t_3a772c1d9d0241168fd2ed7b8d7e6695_name_ReaderType_PosterChild__0_00_00_00_.jpg"
        const val IMAGE_OCTOPUS = "${BASE_IMAGE_URL}t_25a92d21dcb04dcbb9d2d3d2105da3f2_name_ReaderType_DeepDiver__0_00_00_00_.jpg"
        const val IMAGE_CHEETAH = "${BASE_IMAGE_URL}t_6df53edcec074bcbb835b47134f982cd_name_ReaderType_Optimizer__0_00_00_00_.jpg"
        const val IMAGE_MONKEY = "${BASE_IMAGE_URL}t_ca41496280e44711bbd18dc797540bc4_name_ReaderType_Trailblazer__0_00_00_00_.jpg"
        const val IMAGE_SQUIRREL = "${BASE_IMAGE_URL}t_a0772fd0ce22451ebfe88d237544aeff_name_ReaderType_Curator__0_00_00_00_.jpg"
    }
}

enum class NewsprintState {
    LOW_ENGAGED,
    HIGH_ENGAGED,
    COMPLETED_NEWSPRINT
}