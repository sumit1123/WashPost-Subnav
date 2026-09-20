package com.wapo.flagship.features.articles2.repo

import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.FlagshipApplication
import junit.framework.TestCase
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito.*

class Articles2RepositoryTest : TestCase() {
    override fun setUp() {
        super.setUp()
        mockStatic(RemoteLog::class.java)
        mockStatic(FlagshipApplication::class.java)
    }

    @Test
    fun testJsonAdaptor() {
        val articleJson =
            "{\"id\": \"c1cda8bc0a27597c335633234b2bdb32\", \"type\": \"blog\", \"renderer\": \"native\", \"arcId\": \"YBY5TBWPHJC67I3BSSMUW6SRMY\", \"title\": \"CDC presents data aimed at keeping schools open\", \"blurb\": \"The country had made \\u201ctremendous progress\\u201d using key strategies for reducing risk, including vaccination, masking, increasing ventilation and testing, CDC director says.\", \"contenturl\": \"https://www.washingtonpost.com/nation/2022/01/07/covid-omicron-variant-live-updates/\", \"shareurl\": \"https://www.washingtonpost.com/nation/2022/01/07/covid-omicron-variant-live-updates/\", \"source\": \"the-washington-post\", \"section\": \"National\", \"sourcesection\": \"national\", \"sourcesubsection\": \"national\", \"sourcecategory\": \"National\", \"lmt\": 1641650876099, \"published\": 1641650876099, \"first_published\": 1641546170527, \"socialImage\": \"https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/JSKVNFDPYEI6ZMPCAU45VD2EKE.jpg\", \"sourceslug\": \"live-virusglobal0107\", \"commercialnode\": \"/health/coronavirus\", \"omniture\": {\"title\": \"Coronavirus live updates and omicron variant news - The Washington Post\", \"pageName\": \"covid-omicron-variant-live-updates\", \"channel\": \"national\", \"contentId\": \"nullYBY5TBWPHJC67I3BSSMUW6SRMY\", \"contentType\": \"blog\", \"contentSource\": \"the-washington-post\", \"contentTopics\": \"Coronavirus;National;Health\", \"newsroomDesk\": \"foreign;foreign;national;audience engagement\", \"newsroomSubdesk\": \"seoul hub;london hub;health and science;ga and live news\", \"contentAuthor\": \"andrew jeong;adela suliman;frances stead sellers;mar\\u00eda luisa pa\\u00fal\", \"authorId\": \"jeongh;sulimana;sellersfs;paulm\", \"authorType\": \"(not set);staff writer;(not set);(not set)\", \"subSection\": \"national\", \"arcId\": \"YBY5TBWPHJC67I3BSSMUW6SRMY\", \"trackingTags\": \"magnet-coronavirus;coronavirus-free;story-has-edit-branch;created from template id bhnue6ku4neizpcnd5ybg5y46y;linkbox-coronavirus;collections-omicron\"}, \"tags\": \"magnet-coronavirus,coronavirus-free,story-has-edit-branch,created from template id bhnue6ku4neizpcnd5ybg5y46y,linkbox-coronavirus,collections-omicron\", \"items\": [{\"content\": \"National\", \"mime\": \"text/plain\", \"type\": \"kicker\", \"coverageActive\": false, \"displayLabel\": \"National\", \"path\": \"/national\", \"liveText\": \"Covid-19 live updates\"}, {\"content\": \"Covid-19 live updates: CDC presents data aimed at keeping schools open\", \"mime\": \"text/plain\", \"type\": \"title\", \"subtype\": \"h1\", \"liveContent\": \"CDC presents data aimed at keeping schools open\"}, [{\"type\": \"image\", \"mime\": \"image/jpeg\", \"imageURL\": \"https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/JSKVNFDPYEI6ZMPCAU45VD2EKE.jpg\", \"imageWidth\": 3500, \"imageHeight\": 2334, \"image_type\": \"photograph\", \"widthFactor\": \"full-bleed\", \"fullcaption\": \"Centers for Disease Control and Prevention Director Rochelle Walensky gives her opening statement during a Senate hearing on Nov. 4 on Capitol Hill. (Elizabeth Frantz/Reuters)\", \"blurb\": \"Centers for Disease Control and Prevention Director Rochelle Walensky gives her opening statement during a Senate hearing on Nov. 4 on Capitol Hill.\"}]]}"

        try {
            //val article2 = Article2JsonAdapter(getMoshiBuilder().build()).fromJson(articleJson)
            //Assert.assertTrue(article2 != null)
        } catch (ex: Exception) {
            Assert.fail()
        }
    }
}
