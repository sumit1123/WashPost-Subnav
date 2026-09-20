/* Copyright (c) 2024 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.grid.model

class SectionTopper(
    id: String?,
    style: SectionTopperStyle?,
    val title: String?,
    val tagline: String?
) : Item()

enum class SectionTopperStyle {
    COMMENTS
}