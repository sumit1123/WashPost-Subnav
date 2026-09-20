package com.wapo.zendesk.viewmodel

import android.app.Application
import android.content.Context
import com.wapo.android.commons.util.Logger
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.config.sec.helper.WapoSecDataProvider
import com.wapo.zendesk.BuildConfig
import com.wapo.zendesk.ZendeskApplication
import com.wapo.zendesk.model.Status
import com.wapo.zendesk.model.ZendeskImage
import com.wapo.zendesk.repository.Result
import com.wapo.zendesk.repository.ZendeskRepository
import com.wapo.zendesk.util.LiveDataValidator
import com.wapo.zendesk.util.LiveDataValidatorResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import zendesk.core.AnonymousIdentity
import zendesk.core.Zendesk
import zendesk.support.CreateRequest
import zendesk.support.CustomField
import zendesk.support.Support
import javax.inject.Inject

private const val IMAGE_LIST = "ZendeskViewModel.IMAGE_LIST"
private const val IMAGE_COUNTER = "ZendeskViewModel.IMAGE_COUNTER"
private const val TICKET_FORMS = "ZendeskViewModel.TICKET_FROMS"
private const val LOADING_STATE = "ZendeskViewModel.LOADING"
private const val RESULT = "ZendeskViewModel.RESULT"

@HiltViewModel
class ZendeskViewModel @Inject constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    internal val zendeskProvider = (application as ZendeskApplication).zendeskProvider
    private val zendeskRepository = ZendeskRepository(zendeskProvider)
    val imageLiveData: MutableLiveData<MutableSet<ZendeskImage>?> =
        savedStateHandle.getLiveData(IMAGE_LIST)
    val imageCounterLiveData: LiveData<Int> = savedStateHandle.getLiveData(IMAGE_COUNTER, 0)
    val ticketFormsLiveData: LiveData<List<Pair<Long, String>>?> =
        savedStateHandle.getLiveData(TICKET_FORMS)
    val loadingLiveData: LiveData<Boolean> = savedStateHandle.getLiveData(LOADING_STATE, true)
    val resultLiveData: LiveData<Result?> = savedStateHandle.getLiveData(RESULT, null)
    val inputForm = MutableLiveData("")
    val inputEmail = MutableLiveData("")
    val inputSubject = MutableLiveData(getDefaultSubject())
    val inputDescription = MutableLiveData("")
    val isFormValid = MediatorLiveData<Boolean>()
    val inputFormValidator =
        LiveDataValidator(inputForm).apply {
            // Whenever the condition of the predicate is true, the error message should be emitted
            addRule("Please select a topic and try again.") { it?.isBlank() == true }
        }
    val inputEmailValidator =
        LiveDataValidator(inputEmail).apply {
            addRule("Please enter a valid email address and try again.") {
                !Patterns.EMAIL_ADDRESS
                    .matcher(
                        it.toString(),
                    ).matches()
            }
            if (zendeskProvider.isBeta) {
                addRule("You must use a @washpost.com email to submit beta feedback") {
                    it == null || !it.endsWith("@washpost.com")
                }
            }
        }
    val inputSubjectValidator =
        LiveDataValidator(inputSubject).apply {
            addRule("Please enter a subject and try again.") { it?.isBlank() == true }
        }
    val inputDescriptionValidator =
        LiveDataValidator(inputDescription).apply {
            addRule("Please enter at least 15 characters and try again.") { it?.length ?: 0 < 15 }
        }

    init {
        // init form validation to false, add sources
        isFormValid.value = false
        isFormValid.addSource(inputForm) { validateForm() }
        isFormValid.addSource(inputEmail) { validateForm() }
        isFormValid.addSource(inputSubject) { validateForm() }
        isFormValid.addSource(inputDescription) { validateForm() }

        inputEmail.value = zendeskProvider.email
    }

    fun init(context: Context) {
        Zendesk.INSTANCE.init(
            context,
            zendeskProvider.config.url,
            if (BuildConfig.DEBUG) WapoSecDataProvider.zendeskDevAppId else WapoSecDataProvider.zendeskProdAppId,
            if (BuildConfig.DEBUG) WapoSecDataProvider.zendeskDevClientId else WapoSecDataProvider.zendeskProdClientId,
        )
        Support.INSTANCE.init(Zendesk.INSTANCE)
    }

    private fun getDefaultSubject(): String {
        if (zendeskProvider.isBeta) {
            return "Android Beta Feedback"
        }
        return ""
    }

    private fun setIdentity(emailAddress: String) {
        val identity =
            AnonymousIdentity
                .Builder()
                .withNameIdentifier(zendeskProvider.name)
                .withEmailIdentifier(checkEmailForBeta(emailAddress))
                .build()
        Zendesk.INSTANCE.setIdentity(identity)
    }

    private fun checkEmailForBeta(emailAddress: String): String {
        if (zendeskProvider.isBeta) {
            if (emailAddress.endsWith("@washpost.com")) {
                // first.last@washpost.com -> first.last+android-beta@washpost.com
                return emailAddress.replace("@washpost.com", "+android-beta@washpost.com")
            }
        }
        return emailAddress
    }

    fun addImage(zendeskImage: ZendeskImage) {
        savedStateHandle.set(IMAGE_LIST, getUpdatedList(zendeskImage, Operation.ADD))
    }

    fun removeImage(zendeskImage: ZendeskImage) {
        savedStateHandle.set(IMAGE_LIST, getUpdatedList(zendeskImage, Operation.REMOVE))
    }

    fun getTicketForms() {
        viewModelScope.launch {
            val ticketForms: List<Pair<Long?, String?>>? =
                try {
                    when {
                        zendeskProvider.isBeta -> zendeskProvider.config.ticketForms.map { Pair(it.id, it.name) }
                        else -> zendeskRepository.getTicketForms()?.map { Pair(it.id, it.name) }
                    }
                } catch (e: Exception) {
                    Logger.e(TAG, "Error", e)
                    zendeskProvider.config.ticketForms.map { Pair(it.id, it.name) }
                }
            savedStateHandle.set(TICKET_FORMS, ticketForms)
            setIsLoading(false)
        }
    }

    fun createRequest(
        ticketFrom: String,
        emailAddress: String,
        subject: String,
        description: String,
    ) {
        viewModelScope.launch {
            try {
                setIsLoading(true)
                setIdentity(emailAddress)
                val request =
                    CreateRequest().apply {
                        this.subject = subject
                        this.description = description
                        this.ticketFormId =
                            ticketFormsLiveData.value?.find { it.second == ticketFrom }?.first
                        this.customFields =
                            listOf(
                                CustomField(
                                    zendeskProvider.config.customFields.metadataId,
                                    zendeskProvider.metadataCustomField,
                                ),
                                CustomField(
                                    zendeskProvider.config.customFields.appTypeId,
                                    zendeskProvider.config.customFields.appType,
                                ),
                                CustomField(
                                    zendeskProvider.config.customFields.mobileAppId,
                                    zendeskProvider.config.customFields.mobileApp,
                                ),
                            )
                        this.attachments = uploadImages()
                    }
                savedStateHandle.set(RESULT, zendeskRepository.createRequest(request))
            } finally {
                setIsLoading(false)
            }
        }
    }

    private suspend fun uploadImages(): List<String> {
        val attachmentTokens = mutableListOf<String>()
        val inputSet = imageLiveData.value
        inputSet ?: return attachmentTokens
        var outputSet =
            inputSet
                .map { image -> image.copy().also { it.status = Status.PENDING } }
                .toMutableSet()
        imageLiveData.postValue(outputSet)

        inputSet.forEach { zendeskImage ->
            try {
                outputSet = outputSet.replace(zendeskImage.uri, Status.PROGRESS)
                imageLiveData.postValue(outputSet)

                val attachmentToken =
                    zendeskRepository.uploadImage(
                        zendeskImage.uri,
                        zendeskImage.fileName,
                        zendeskImage.mimeType,
                    )

                outputSet = outputSet.replace(zendeskImage.uri, Status.FINISHED)
                imageLiveData.postValue(outputSet)

                if (attachmentToken != null) {
                    attachmentTokens.add(attachmentToken)
                } else {
                    throw NullPointerException("Missing attachment token")
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Error uploading image", e)
            }
        }
        return attachmentTokens
    }

    private fun getUpdatedList(
        zendeskImage: ZendeskImage,
        operation: Operation,
    ): MutableSet<ZendeskImage> {
        val set = savedStateHandle.get<MutableSet<ZendeskImage>?>(IMAGE_LIST) ?: linkedSetOf()
        return set.apply {
            when (operation) {
                Operation.ADD -> add(zendeskImage)
                Operation.REMOVE -> remove(zendeskImage)
            }
            savedStateHandle.set(IMAGE_COUNTER, set.size)
        }
    }

    private fun setIsLoading(isLoading: Boolean) {
        savedStateHandle.set(LOADING_STATE, isLoading)
    }

    // Resolves validators to determine if all form fields are valid, then sets isFormValid to true.
    private fun validateForm() {
        val validators =
            listOf(
                inputFormValidator,
                inputEmailValidator,
                inputSubjectValidator,
                inputDescriptionValidator,
            )
        val validatorResolver = LiveDataValidatorResolver(validators)
        isFormValid.value = validatorResolver.isValid()
    }

    private enum class Operation {
        ADD,
        REMOVE,
    }

    companion object {
        private val TAG = ZendeskViewModel::class.java.simpleName
    }
}

private fun MutableSet<ZendeskImage>.replace(
    uri: String,
    status: Status,
): MutableSet<ZendeskImage> =
    this
        .map {
            val item = it.copy()
            if (item.uri == uri) {
                item.status = status
            } else {
                item.status = it.status
            }
            item
        }.toMutableSet()
