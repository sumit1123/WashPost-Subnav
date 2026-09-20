package com.wapo.flagship.features.grid.model

import com.wapo.flagship.features.grid.Rating
import com.wapo.flagship.features.grid.RecipeInfo

data class CarouselRecipe(
    val items: List<CarouselRecipeItem>,
    val label: CompoundLabel?,
    val cta: CompoundLabel?,
    val cardify: Boolean?
) : Item()

data class CarouselRecipeItem(
    val headline: ImmersionHeadline,
    val media: Media?,
    val link: Link,
    var recipeInfo: RecipeInfo?,
    var rating: Rating?,
)