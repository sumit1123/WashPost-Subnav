package com.wapo.flagship.content

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.wapo.flagship.di.core.modules.DefaultOnDataMismatchAdapter
import com.wapo.flagship.features.amazonunification.models.RainbowArticle
import com.wapo.flagship.features.articles2.models.Item
import com.wapo.flagship.features.articles2.models.deserialized.ByLine
import com.wapo.flagship.features.articles2.models.deserialized.Date
import org.junit.Test
import java.io.File

class UnifiedSavedStoriesTest {
    @Test
    fun test1() {
        val article =
            getMoshiBuilder()
                .build()
                .adapter(RainbowArticle::class.java)
                .fromJson(getArticleFeedAsJson())

        article?.let {
            println(
                "\nTest1 Result:\nTitle=${article.title},\nUrl=${article.contentUrl},\nsourceUrl=${article.sourceUrl}}\n",
            )
        }

        assert(article != null)
    }

    private fun getArticleFeedAsJson(): String {
        val classLoader = this.javaClass.classLoader
        val resource = classLoader.getResource("classic_native_article.json")
        return File(resource.path).readText()
    }

    private fun getMoshiBuilder(): Moshi.Builder =
        Moshi
            .Builder()
            .add(DefaultOnDataMismatchAdapter.newFactory(Item::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(ByLine::class.java, null))
            .add(DefaultOnDataMismatchAdapter.newFactory(Date::class.java, null))
            .add(
                PolymorphicJsonAdapterFactory
                    .of(Item::class.java, "type")
                    .withSubtype(ByLine::class.java, "byline")
                    .withSubtype(Date::class.java, "date")
                    .withDefaultValue(Item("default")),
            )
}
