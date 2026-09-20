package com.wapo.kmpshared.features.conversations.domain.usecase

import com.wapo.kmpshared.core.KMPResult
import com.wapo.kmpshared.features.conversations.data.CommentAuthRepository
import org.koin.core.annotation.Factory

@Factory
class UpdateDisplayNameUseCase(
    private val authRepo: CommentAuthRepository,
) {
    suspend operator fun invoke(newName: String): KMPResult<Unit> {
        // Handles both the Profile Update AND the ctoken refresh
        return authRepo.updateDisplayName(newName)
    }
}
