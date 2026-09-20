package com.wapo.flagship.content

import com.wapo.flagship.common.SECTION_URL_TO_SECTION_PATH_REGEX
import org.junit.Test
import java.util.regex.Pattern

class SectionUrlsText {
    private val sectionUrls =
        listOf(
            "",
            "https://www.washingtonpost.com",
            "https://www.washingtonpost.com/",
            "https://www.washingtonpost.com/path1", // valid
            "https://www.washingtonpost.com/path1/", // valid
            "https://www.washingtonpost.com/path1//",
            "https://www.washingtonpost.com/path1/path2", // valid
            "https://www.washingtonpost.com/path1/path2/", // valid
            "https://www.washingtonpost.com/path1/path2/path3",
        )

    @Test
    fun test() {
        sectionUrls.forEach {
            if (Pattern.matches(SECTION_URL_TO_SECTION_PATH_REGEX, it)) {
                println("✓ $it")
            } else {
                println("x $it")
            }
        }
    }
}
