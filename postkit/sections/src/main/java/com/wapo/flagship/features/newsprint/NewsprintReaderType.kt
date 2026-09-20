package com.wapo.flagship.features.newsprint

enum class NewsprintReaderType(
    val id: String,
    val displayName: String,
    val imageUrl: String,
    val videoUrl: String
) {
    THE_INTELLECTUAL(
        id = "Intellectual",
        displayName = "The Intellectual",
        imageUrl = NewsprintViewModel.IMAGE_OWL,
        videoUrl = NewsprintViewModel.VIDEO_OWL
    ),

    THE_POST_ER_CHILD(
        id = "Post-er Child",
        displayName = "The Post-er Child",
        imageUrl = NewsprintViewModel.IMAGE_DOG,
        videoUrl = NewsprintViewModel.VIDEO_DOG
    ),

    THE_OPTIMIZER(
        id = "Optimizer",
        displayName = "The Optimizer",
        imageUrl = NewsprintViewModel.IMAGE_CHEETAH,
        videoUrl = NewsprintViewModel.VIDEO_CHEETAH
    ),

    THE_TRAILBLAZER(
        id = "Trailblazer",
        displayName = "The Trailblazer",
        imageUrl = NewsprintViewModel.IMAGE_MONKEY,
        videoUrl = NewsprintViewModel.VIDEO_MONKEY
    ),

    THE_DEEP_DIVER(
        id = "Deep Diver",
        displayName = "The Deep Diver",
        imageUrl = NewsprintViewModel.IMAGE_OCTOPUS,
        videoUrl = NewsprintViewModel.VIDEO_OCTOPUS
    ),

    THE_CURATOR(
        id = "Curator",
        displayName = "The Curator",
        imageUrl = NewsprintViewModel.IMAGE_SQUIRREL,
        videoUrl = NewsprintViewModel.VIDEO_SQUIRREL
    );

    companion object {
        fun findById(id: String): NewsprintReaderType? {
            return entries.firstOrNull { it.id == id }
        }
    }
}