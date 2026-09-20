package com.wapo.flagship.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.*

/**
 * Created by curacamalitod on 12/14/16.
 */

data class PrintManifestResponse(
    @SerializedName("issue") val issue: Issue,
) : Serializable {
    override fun equals(other: Any?): Boolean = super.equals(other)
}

data class Issue(
    val pubdate: Long,
    val sections: List<PrintSection>,
) : Serializable {
    fun getFrontPageImageName(): String? {
        if (this.sections.isNotEmpty()) {
            return this.sections[0].getCoverImageName()
        }

        return null
    }
}

class PrintSection(
    @SerializedName("section_letter") val sectionLetter: String,
    @SerializedName("name") val sectionName: String,
    @SerializedName("lmt") val lmt: String?,
    @SerializedName("pages") val pages: ArrayList<PrintSectionPage>?,
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is PrintSection) {
            return false
        }

        return (sectionLetter == other.sectionLetter) && (sectionName == other.sectionName)
    }

    fun getCoverImageName(): String? {
        if (this.pages != null && this.pages.size > 0) {
            return this.pages.get(0).hiResImagePath
        }

        return null
    }

    fun getCoverImageHeight(): Int {
        if (this.pages != null && this.pages.size > 0) {
            return this.pages.get(0).pageHeight
        }
        return -1
    }

    fun getCoverImageWidth(): Int {
        if (this.pages != null && this.pages.size > 0) {
            return this.pages.get(0).pageWidth
        }
        return -1
    }
}

class PrintSectionPage(
    @SerializedName("page_name") val pageName: String,
    @SerializedName("page_height") val pageHeight: Int,
    @SerializedName("page_width") val pageWidth: Int,
    @SerializedName("hires_2048") val hiResImagePath: String?,
    @SerializedName("thumb_300") val thumbnailPath: String,
    @SerializedName("hires_pdf") val hiResPdfPath: String,
    @SerializedName("pn") val pageNumber: Int,
    @SerializedName("article_uuids") val articleUuids: List<PrintArticleUuid>?,
    var sectionLetter: String?,
    var sectionLmt: Long?,
) : Serializable

class PrintArticleUuid(
    val lmt: String,
    val uuid: String,
) : Serializable
