package com.wapo.flagship.features.utils

import android.os.HandlerThread
import com.wapo.flagship.features.audio.BuildConfig
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import kotlin.random.Random

class MockResponseInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (BuildConfig.DEBUG) {
            val request = chain.request()
            val response =
                when (request.header(MOCK_RESPONSE)) {
                    SUMMARY -> {
                        dispatchSummaryResponse(request)
                    }

                    SUMMARY_FEEDBACK -> {
                        dispatchSummaryFeedbackResponse(request)
                    }

                    FOR_YOU_AI_XP -> {
                        dispatchForYouAiXpResponse(request)
                    }

                    PERSO_CONFIG -> {
                        dispatchPersoConfigResponse(request)
                    }

                    GENERATED_PODCASTS -> {
                        dispatchGeneratedPodcastsResponse(request)
                    }

                    GENERATE_PODCAST -> {
                        dispatchGeneratePodcastResponse(request)
                    }

                    SINGLE_PODCAST -> {
                        dispatchSinglePersoPod(request)
                    }

                    else -> null
                }
            if (response != null) return response
        }
        return chain.proceed(chain.request())
    }

    private fun dispatchGeneratePodcastResponse(request: Request): Response {
        val responseString: String = """
        {
              "mp3_url": "https://wp-data-ai-bigdata-share-temp.s3.amazonaws.com/user/BF6438D072236C5FE0530100007FF468/2899deea-fa75-4a17-bedb-7f47d74ce6f4.mp3?AWSAccessKeyId=ASIATKPZH4EECQ33LK3J&Signature=o4%2FHvWqiZewyrR1fBCaMk10SJfA%3D&x-amz-security-token=IQoJb3JpZ2luX2VjEK7%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCXVzLWVhc3QtMSJGMEQCIF7eYwm6pYqvIYYsHfmAvEgU9R03H%2FOeILrKlmsmSVghAiBCP5F97jJdjROXTUoxMsWbmdEV9gi3kVwSBjT62DpMoSr9Awh2EAQaDDIyODY5Mjg0NDgwOCIMMW4LKN81K7FJ4RxOKtoDdgXNSdwdqJazH%2FK6RinGx4fWMKuTOFvTPHmyn9MrXXY%2B3GttiFZF65uZ%2F7NxGXGWYeTXNutDf0gs83XQEHBf%2FxyVO6IES6poKE4VFywCeTd%2BiehmGyp2rKUq9E54BVocRmfYyB7vbVygkRywItmIAuL1AwV5n%2B1P%2Fau25sSIv0mqr27rlX8JKqNwxAEQbmEHLB%2Bxl5cVbYpMgwhF3LE6j2pzs%2FbuSPWBdIGUC%2F0sPruGg%2BDyJVPXsXaUQJH6h0M26cj4ypFQoncya8lD0DeQyjaHGfeIpJ%2FSo81mL%2B8npmdrzotvafhYxQ0%2B3Bqi%2BBu8jDOa9B8mLc5YX76yKYE%2BiuvdwcvJO4m9tdszg5Y3ReucQYv%2FAWb1Q%2FIYdYnJqwfZ8J%2FCe30YE%2BvJ0WFRm%2Bq0EGMPRXWFMy0SrwrnuyPKRE5ZSJDzlc7d2rytz38pyedZmZxxrEQYS3GkUn8tugSkufd%2BHXhirjdUbRDKO%2F8YpaixQesOHg2npoijU9HRxiByaJoXSdTPa36O37199Cy6OOFUEPs4BuPm6gYY%2FK%2FMA36aKHpm9XHcc45Rqgj0U3iNjAU%2Bd7nhVfhqCOs0mzVJddZLugpYr8S1t1C%2Fhrczb0w1ynShCsINAHHaMM%2F0p8gGOqYBz%2B7aHa1dAgccQ%2BcCke97hSrhWnQYBbCYiGwmBLf4KPndvSiNYduA2TQyXW5e1jPJbWx6xDdRBdaIhImkfg3Ov7%2FopjwPT9qg8%2FGUlyHhpGq7c75XQbxdpsvftjzMXE%2FxNIdvhepYPKWC0hSSZkNBc3Zqr81xnvhB7coR9VLcuBMzLMFzSVk25FW4L6rqohU%2BRG9QV7Qv%2F%2BH7UYaQkGI3ohymcDnwTQ%3D%3D&Expires=1762278183",
              "kicker": "Personalized AI Podcast",
              "transcript": "https://wp-data-ai-bigdata-share-temp.s3.amazonaws.com/default/default.txt?AWSAccessKeyId=ASIATKPZH4EEK7YAQYLZ&Signature=0pen6aZptp%2BSzWYtUq0hk3jrMsk%3D&x-amz-security-token=IQoJb3JpZ2luX2VjEP3%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCXVzLWVhc3QtMSJIMEYCIQDJmBO%2BJX5OVOQGOIeUA4ElyHvbnpBRZDZVcH91CPd3mAIhAICW9SucX%2BDTM3QK80vddTJjOJmjUK6rO004xCQA88hXKoYECLX%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEQBBoMMjI4NjkyODQ0ODA4IgwqESv2pKEoDjqIRYcq2gPZzvhd0h6ZzfM5rYK4Nenyq5IhvwZG0eoZj9y%2FL4wsthb6IJKopdFRMIF9KHuTjHYhP1P8REaESxkidQCaKOrA03%2FtxkFRKbESEhCIhJkdRoLYrmd%2BWkkOepLIpYcBXoDi%2FZaZdutXpZb47uxWWzp7s3WSAcLJyhVQhl1F3NzDe5U7GYcar1uKZPJ0Q5WUBAsj8pgtSE0ECElkumaZn%2B%2FqNdyeL1rdjW38Ti8J5J9sOvJmcoFaMo%2BZ7Y6HK8z8VALoiKtOcMIvVNVn8SUAa93TDmRTgh5HYm3m0uQoXDr4U32SReeB1Bp29putbZuNzYgH%2FWuQye0I0V%2FzSY5%2BKJzzwhfOYGYdTSkRcINq1GCKhsjGh670ww7lY20g1m6fqFbe7W8VJIafzFIZNpLmFWbYveLtEHRtDufLoIciy6OFZxs42uBW5QivFeGGkUgl8PvypDdD5S68oAmVm0DOpRFCbyuYjb0HC1mHzF70F029UZDChPap4CVjHpTpd9F2R6a%2Fnx4gvcrg4SZQcR27IrLk6eCYEBKCZz16Cu3oRZhwaYDsDQYN3nHqybW2FZMLV1IQdgB6q%2BSyyYe%2BQv%2BjOvaZPpw%2FrOhVdaQDfxQWYTUmMS1WsPKv33qgteMw9YeByAY6pAE3IubiknWm%2F3iFezGDuyIshFsJP2GAY0SbaX%2BlnhdcF%2F33YsY25tSuOhYUTRny%2Bb1R6mRPjj7aeucIIfB7DywtSycw9Q%2Bran6rSLU2EFdw3USUsKddTz2rnJ6s%2Fe7X%2FEkz369vI%2BU23A8H5aigNQlKfxhHClg%2FVTR6FpaRLDNbd95h83kg3gmBD75yO%2Fz7bAAVtSCeuPupvdmqw5OZq926%2BAquXA%3D%3D&Expires=1761632348",
              "title": "November 4: The Ripple Effects of Shutdowns and Pandemics on Culture and Entertainment",
              "summary": "In this episode, we delve into the intricate relationships between justice, politics, and natural disasters. From the complexities of the US justice system to the challenges of responding to hurricanes, we explore how these elements intersect and impact our lives. With a focus on recent events and developments, we examine the role of key figures, the implications of their actions, and the potential consequences for the future. Join us as we navigate the nuances of these critical issues and their far-reaching effects.",
              "audio_file_path": "https://wp-data-ai-bigdata-share-temp.s3.amazonaws.com/user/BF6438D072236C5FE0530100007FF468/2899deea-fa75-4a17-bedb-7f47d74ce6f4.mp3?AWSAccessKeyId=ASIATKPZH4EECQ33LK3J&Signature=o4%2FHvWqiZewyrR1fBCaMk10SJfA%3D&x-amz-security-token=IQoJb3JpZ2luX2VjEK7%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCXVzLWVhc3QtMSJGMEQCIF7eYwm6pYqvIYYsHfmAvEgU9R03H%2FOeILrKlmsmSVghAiBCP5F97jJdjROXTUoxMsWbmdEV9gi3kVwSBjT62DpMoSr9Awh2EAQaDDIyODY5Mjg0NDgwOCIMMW4LKN81K7FJ4RxOKtoDdgXNSdwdqJazH%2FK6RinGx4fWMKuTOFvTPHmyn9MrXXY%2B3GttiFZF65uZ%2F7NxGXGWYeTXNutDf0gs83XQEHBf%2FxyVO6IES6poKE4VFywCeTd%2BiehmGyp2rKUq9E54BVocRmfYyB7vbVygkRywItmIAuL1AwV5n%2B1P%2Fau25sSIv0mqr27rlX8JKqNwxAEQbmEHLB%2Bxl5cVbYpMgwhF3LE6j2pzs%2FbuSPWBdIGUC%2F0sPruGg%2BDyJVPXsXaUQJH6h0M26cj4ypFQoncya8lD0DeQyjaHGfeIpJ%2FSo81mL%2B8npmdrzotvafhYxQ0%2B3Bqi%2BBu8jDOa9B8mLc5YX76yKYE%2BiuvdwcvJO4m9tdszg5Y3ReucQYv%2FAWb1Q%2FIYdYnJqwfZ8J%2FCe30YE%2BvJ0WFRm%2Bq0EGMPRXWFMy0SrwrnuyPKRE5ZSJDzlc7d2rytz38pyedZmZxxrEQYS3GkUn8tugSkufd%2BHXhirjdUbRDKO%2F8YpaixQesOHg2npoijU9HRxiByaJoXSdTPa36O37199Cy6OOFUEPs4BuPm6gYY%2FK%2FMA36aKHpm9XHcc45Rqgj0U3iNjAU%2Bd7nhVfhqCOs0mzVJddZLugpYr8S1t1C%2Fhrczb0w1ynShCsINAHHaMM%2F0p8gGOqYBz%2B7aHa1dAgccQ%2BcCke97hSrhWnQYBbCYiGwmBLf4KPndvSiNYduA2TQyXW5e1jPJbWx6xDdRBdaIhImkfg3Ov7%2FopjwPT9qg8%2FGUlyHhpGq7c75XQbxdpsvftjzMXE%2FxNIdvhepYPKWC0hSSZkNBc3Zqr81xnvhB7coR9VLcuBMzLMFzSVk25FW4L6rqohU%2BRG9QV7Qv%2F%2BH7UYaQkGI3ohymcDnwTQ%3D%3D&Expires=1762278183",
              "audio_duration": 268.146939,
              "total_characters": 4168,
              "item_type": "podcast",
              "image": "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/FTNQXJJ6XVBMNKFO3E6YDZBPGM.png",
              "articles_used": [
                {
                  "canonical_url": "/national-security/2025/10/27/comey-vindictive-prosecution-amicus/",
                  "headline": "Former DOJ officials say Comey case is vindictive, call for dismissal",
                  "display_date": "2025-10-27T22:02:14.422Z",
                  "image": "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/LSOZ2VR5CMHASUEFLOVX3FLLRY.jpg",
                  "source": "The Washington Post",
                  "byline": "Perry Stein"
                },
                {
                  "canonical_url": "/investigations/2025/10/27/injustice-jack-smith-trump-florida/",
                  "headline": "How Jack Smith’s strongest case against Donald Trump collapsed   ",
                  "display_date": "2025-10-27T20:39:05.122Z",
                  "image": "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/2A5F3HFL6YIZJAU2EQETUBRGSE.JPG",
                  "source": "The Washington Post",
                  "byline": "Aaron C. Davis"
                },
                {
                  "canonical_url": "/national-security/2025/10/27/hurricane-melissa-trump-venezuela-disaster-military/",
                  "headline": "Hurricane Melissa collides with U.S. military mission in Caribbean",
                  "display_date": "2025-10-27T23:11:54.109Z",
                  "image": "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/PGWN2IIMHEHWCKV3MWW3RALOKE.JPG",
                  "source": "The Washington Post",
                  "byline": "Dan Lamothe"
                }
              ]
            }
        """.trimIndent()
        return Response
            .Builder()
            .code(201)
            .message(responseString)
            .request(request)
            .protocol(Protocol.HTTP_1_0)
            .body(
                responseString
                    .toByteArray()
                    .toResponseBody("application/json".toMediaTypeOrNull()),
            ).addHeader("content-type", "application/json")
            .build()
    }

    private fun dispatchGeneratedPodcastsResponse(request: Request): Response {
        val responseString: String = """
        {
          "podcasts": [
            {
              "mp3_url": null,
              "kicker": "Personalized AI Podcast",
              "transcript": "https://wp-data-ai-bigdata-share-temp.s3.amazonaws.com/default/default.txt?AWSAccessKeyId=ASIATKPZH4EEK7YAQYLZ&Signature=0pen6aZptp%2BSzWYtUq0hk3jrMsk%3D&x-amz-security-token=IQoJb3JpZ2luX2VjEP3%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCXVzLWVhc3QtMSJIMEYCIQDJmBO%2BJX5OVOQGOIeUA4ElyHvbnpBRZDZVcH91CPd3mAIhAICW9SucX%2BDTM3QK80vddTJjOJmjUK6rO004xCQA88hXKoYECLX%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEQBBoMMjI4NjkyODQ0ODA4IgwqESv2pKEoDjqIRYcq2gPZzvhd0h6ZzfM5rYK4Nenyq5IhvwZG0eoZj9y%2FL4wsthb6IJKopdFRMIF9KHuTjHYhP1P8REaESxkidQCaKOrA03%2FtxkFRKbESEhCIhJkdRoLYrmd%2BWkkOepLIpYcBXoDi%2FZaZdutXpZb47uxWWzp7s3WSAcLJyhVQhl1F3NzDe5U7GYcar1uKZPJ0Q5WUBAsj8pgtSE0ECElkumaZn%2B%2FqNdyeL1rdjW38Ti8J5J9sOvJmcoFaMo%2BZ7Y6HK8z8VALoiKtOcMIvVNVn8SUAa93TDmRTgh5HYm3m0uQoXDr4U32SReeB1Bp29putbZuNzYgH%2FWuQye0I0V%2FzSY5%2BKJzzwhfOYGYdTSkRcINq1GCKhsjGh670ww7lY20g1m6fqFbe7W8VJIafzFIZNpLmFWbYveLtEHRtDufLoIciy6OFZxs42uBW5QivFeGGkUgl8PvypDdD5S68oAmVm0DOpRFCbyuYjb0HC1mHzF70F029UZDChPap4CVjHpTpd9F2R6a%2Fnx4gvcrg4SZQcR27IrLk6eCYEBKCZz16Cu3oRZhwaYDsDQYN3nHqybW2FZMLV1IQdgB6q%2BSyyYe%2BQv%2BjOvaZPpw%2FrOhVdaQDfxQWYTUmMS1WsPKv33qgteMw9YeByAY6pAE3IubiknWm%2F3iFezGDuyIshFsJP2GAY0SbaX%2BlnhdcF%2F33YsY25tSuOhYUTRny%2Bb1R6mRPjj7aeucIIfB7DywtSycw9Q%2Bran6rSLU2EFdw3USUsKddTz2rnJ6s%2Fe7X%2FEkz369vI%2BU23A8H5aigNQlKfxhHClg%2FVTR6FpaRLDNbd95h83kg3gmBD75yO%2Fz7bAAVtSCeuPupvdmqw5OZq926%2BAquXA%3D%3D&Expires=1761632348",
              "title": "Your personalized podcast for November 4, 2025",
              "summary": "In this episode, we delve into the intricate relationships between justice, politics, and natural disasters. From the complexities of the US justice system to the challenges of responding to hurricanes, we explore how these elements intersect and impact our lives. With a focus on recent events and developments, we examine the role of key figures, the implications of their actions, and the potential consequences for the future. Join us as we navigate the nuances of these critical issues and their far-reaching effects.",
              "audio_file_path": null,
              "audio_duration": null,
              "total_characters": null,
              "item_type": "placeholder",
              "image": "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/FTNQXJJ6XVBMNKFO3E6YDZBPGM.png",
              "articles_used": [
                {
                  "canonical_url": "/national-security/2025/10/27/comey-vindictive-prosecution-amicus/",
                  "headline": "Former DOJ officials say Comey case is vindictive, call for dismissal",
                  "display_date": "2025-10-27T22:02:14.422Z",
                  "image": "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/LSOZ2VR5CMHASUEFLOVX3FLLRY.jpg",
                  "source": "The Washington Post",
                  "byline": "Perry Stein"
                },
                {
                  "canonical_url": "/investigations/2025/10/27/injustice-jack-smith-trump-florida/",
                  "headline": "How Jack Smith’s strongest case against Donald Trump collapsed   ",
                  "display_date": "2025-10-27T20:39:05.122Z",
                  "image": "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/2A5F3HFL6YIZJAU2EQETUBRGSE.JPG",
                  "source": "The Washington Post",
                  "byline": "Aaron C. Davis"
                },
                {
                  "canonical_url": "/national-security/2025/10/27/hurricane-melissa-trump-venezuela-disaster-military/",
                  "headline": "Hurricane Melissa collides with U.S. military mission in Caribbean",
                  "display_date": "2025-10-27T23:11:54.109Z",
                  "image": "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/PGWN2IIMHEHWCKV3MWW3RALOKE.JPG",
                  "source": "The Washington Post",
                  "byline": "Dan Lamothe"
                }
              ]
            },
             {
              "mp3_url": "https://wapo-personalized-podcasts-staging.s3.amazonaws.com/users/user_3b02adf7-ab98-45d5-a8e7-8b40edce8724/audio/episode_2025-11-04.mp3?AWSAccessKeyId=ASIATKPZH4EEHR4QTDQD&Signature=AsYzuw9TiW4LJEKa25DY2pi3MJ0%3D&x-amz-security-token=IQoJb3JpZ2luX2VjEOL%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCXVzLWVhc3QtMSJHMEUCIQDjIvAlJPHtJWJ2CH4PVqz5w4ouWWoSuU33nwsFDRv18QIgDvxTWkiO1lFU92eZOWgkkl2nDwlRSVLgLpnx6Lp6j5sqhgQIq%2F%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FARAEGgwyMjg2OTI4NDQ4MDgiDL6AkD3XaqNiyYC9wSraA1IMwJHBxUxeri76ixjbW0mstekr85lSNbclu5105vDMrqDgIYd46A9oIb30Nfu1KQ%2FFZM1FHzHh%2F9GuCzoGHdkDtK6AJn%2BDg4YJmVb8xRmqZAbkAB0uaFP0IFyTVDzBvhepShbD22DhJt%2FDvp18O6%2BVqXYQGbxz4DNnp01PXnrzBsbPtNprGkPhCNTFbXhOkuq9pMp6tQQXxRBwAL93QYWEnM0%2F4o%2BA%2FBB9llLtVazvO4%2Foh3ltBZV%2FIShX0CY31a5nW%2BZd71NFbUPUXzIPhxmh8xVfr3%2FkNraloUZjh2nwhU4M2IclHiH85OGC7SjEoxoZxwqkmolhu5D0FLuqi7mbA62Kqv4YrI4gM14P9tzNJdEFtq07tClHMkxypUsyACkxayRyzR5OynI1%2Bt42D6XCk76f8DnNnVSsiLzAY%2Ff6BwLmZVVRtodYu%2FlMkCQ9XvBMohOi2FbVILy97y4ce1boZfDaWZiECIlv4xvpdv9hbhpPHtGmdRD6DpILeupgg83ClemT9ncnIer%2FAdUIVdecGOnxehLJoS00uh8U8dx3FoJfOt3it5qDu8XJnl8L4Mu5bzKNs6X2T1rdmG3PLUW0ALGSdvXg5C320pOzv0XZHsZ35mIwX5x63TCZtrPIBjqlAZSR9Ii19y%2F4wBbvMtNIKSr6dO63ESdHel0gvZxg5GmiN6CTEaZRtqxE0jMn2I6Qn878hEdmxLYKIsfg%2BZmWUESiJh4uOlizd4d1mbDz%2BSBhZ%2FIEXvl37g4YcNx3ykY%2FAhq8Qd6S%2FGUDb%2FrEPTmko2pXMShKA9%2FU%2BTbKO8Qxe3riHbmd2tXe5M86LPhktJYGipsd%2Bwm5ucaVbTG9Dv5h0CagiAwY9A%3D%3D&Expires=1762543359",
              "kicker": "Personalized AI Podcast",
              "transcript": "https://wp-data-ai-bigdata-share-temp.s3.amazonaws.com/default/default.txt?AWSAccessKeyId=ASIATKPZH4EEK7YAQYLZ&Signature=0pen6aZptp%2BSzWYtUq0hk3jrMsk%3D&x-amz-security-token=IQoJb3JpZ2luX2VjEP3%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCXVzLWVhc3QtMSJIMEYCIQDJmBO%2BJX5OVOQGOIeUA4ElyHvbnpBRZDZVcH91CPd3mAIhAICW9SucX%2BDTM3QK80vddTJjOJmjUK6rO004xCQA88hXKoYECLX%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEQBBoMMjI4NjkyODQ0ODA4IgwqESv2pKEoDjqIRYcq2gPZzvhd0h6ZzfM5rYK4Nenyq5IhvwZG0eoZj9y%2FL4wsthb6IJKopdFRMIF9KHuTjHYhP1P8REaESxkidQCaKOrA03%2FtxkFRKbESEhCIhJkdRoLYrmd%2BWkkOepLIpYcBXoDi%2FZaZdutXpZb47uxWWzp7s3WSAcLJyhVQhl1F3NzDe5U7GYcar1uKZPJ0Q5WUBAsj8pgtSE0ECElkumaZn%2B%2FqNdyeL1rdjW38Ti8J5J9sOvJmcoFaMo%2BZ7Y6HK8z8VALoiKtOcMIvVNVn8SUAa93TDmRTgh5HYm3m0uQoXDr4U32SReeB1Bp29putbZuNzYgH%2FWuQye0I0V%2FzSY5%2BKJzzwhfOYGYdTSkRcINq1GCKhsjGh670ww7lY20g1m6fqFbe7W8VJIafzFIZNpLmFWbYveLtEHRtDufLoIciy6OFZxs42uBW5QivFeGGkUgl8PvypDdD5S68oAmVm0DOpRFCbyuYjb0HC1mHzF70F029UZDChPap4CVjHpTpd9F2R6a%2Fnx4gvcrg4SZQcR27IrLk6eCYEBKCZz16Cu3oRZhwaYDsDQYN3nHqybW2FZMLV1IQdgB6q%2BSyyYe%2BQv%2BjOvaZPpw%2FrOhVdaQDfxQWYTUmMS1WsPKv33qgteMw9YeByAY6pAE3IubiknWm%2F3iFezGDuyIshFsJP2GAY0SbaX%2BlnhdcF%2F33YsY25tSuOhYUTRny%2Bb1R6mRPjj7aeucIIfB7DywtSycw9Q%2Bran6rSLU2EFdw3USUsKddTz2rnJ6s%2Fe7X%2FEkz369vI%2BU23A8H5aigNQlKfxhHClg%2FVTR6FpaRLDNbd95h83kg3gmBD75yO%2Fz7bAAVtSCeuPupvdmqw5OZq926%2BAquXA%3D%3D&Expires=1761632348",
              "title": "(Onboarding example) Introducing Personalized Podcasts",
              "summary": "In this episode, we delve into the intricate relationships between justice, politics, and natural disasters. From the complexities of the US justice system to the challenges of responding to hurricanes, we explore how these elements intersect and impact our lives. With a focus on recent events and developments, we examine the role of key figures, the implications of their actions, and the potential consequences for the future. Join us as we navigate the nuances of these critical issues and their far-reaching effects.",
              "audio_file_path": "https://wapo-personalized-podcasts-staging.s3.amazonaws.com/users/user_3b02adf7-ab98-45d5-a8e7-8b40edce8724/audio/episode_2025-11-04.mp3?AWSAccessKeyId=ASIATKPZH4EEHR4QTDQD&Signature=AsYzuw9TiW4LJEKa25DY2pi3MJ0%3D&x-amz-security-token=IQoJb3JpZ2luX2VjEOL%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCXVzLWVhc3QtMSJHMEUCIQDjIvAlJPHtJWJ2CH4PVqz5w4ouWWoSuU33nwsFDRv18QIgDvxTWkiO1lFU92eZOWgkkl2nDwlRSVLgLpnx6Lp6j5sqhgQIq%2F%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FARAEGgwyMjg2OTI4NDQ4MDgiDL6AkD3XaqNiyYC9wSraA1IMwJHBxUxeri76ixjbW0mstekr85lSNbclu5105vDMrqDgIYd46A9oIb30Nfu1KQ%2FFZM1FHzHh%2F9GuCzoGHdkDtK6AJn%2BDg4YJmVb8xRmqZAbkAB0uaFP0IFyTVDzBvhepShbD22DhJt%2FDvp18O6%2BVqXYQGbxz4DNnp01PXnrzBsbPtNprGkPhCNTFbXhOkuq9pMp6tQQXxRBwAL93QYWEnM0%2F4o%2BA%2FBB9llLtVazvO4%2Foh3ltBZV%2FIShX0CY31a5nW%2BZd71NFbUPUXzIPhxmh8xVfr3%2FkNraloUZjh2nwhU4M2IclHiH85OGC7SjEoxoZxwqkmolhu5D0FLuqi7mbA62Kqv4YrI4gM14P9tzNJdEFtq07tClHMkxypUsyACkxayRyzR5OynI1%2Bt42D6XCk76f8DnNnVSsiLzAY%2Ff6BwLmZVVRtodYu%2FlMkCQ9XvBMohOi2FbVILy97y4ce1boZfDaWZiECIlv4xvpdv9hbhpPHtGmdRD6DpILeupgg83ClemT9ncnIer%2FAdUIVdecGOnxehLJoS00uh8U8dx3FoJfOt3it5qDu8XJnl8L4Mu5bzKNs6X2T1rdmG3PLUW0ALGSdvXg5C320pOzv0XZHsZ35mIwX5x63TCZtrPIBjqlAZSR9Ii19y%2F4wBbvMtNIKSr6dO63ESdHel0gvZxg5GmiN6CTEaZRtqxE0jMn2I6Qn878hEdmxLYKIsfg%2BZmWUESiJh4uOlizd4d1mbDz%2BSBhZ%2FIEXvl37g4YcNx3ykY%2FAhq8Qd6S%2FGUDb%2FrEPTmko2pXMShKA9%2FU%2BTbKO8Qxe3riHbmd2tXe5M86LPhktJYGipsd%2Bwm5ucaVbTG9Dv5h0CagiAwY9A%3D%3D&Expires=1762543359",
              "audio_duration": 218.488163,
              "total_characters": 3414,
              "item_type": "onboarding",
              "image": "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/FTNQXJJ6XVBMNKFO3E6YDZBPGM.png",
              "articles_used": [
                {
                  "canonical_url": "/national-security/2025/10/27/comey-vindictive-prosecution-amicus/",
                  "headline": "Former DOJ officials say Comey case is vindictive, call for dismissal",
                  "display_date": "2025-10-27T22:02:14.422Z",
                  "image": "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/LSOZ2VR5CMHASUEFLOVX3FLLRY.jpg",
                  "source": "The Washington Post",
                  "byline": "Perry Stein"
                },
                {
                  "canonical_url": "/investigations/2025/10/27/injustice-jack-smith-trump-florida/",
                  "headline": "How Jack Smith’s strongest case against Donald Trump collapsed   ",
                  "display_date": "2025-10-27T20:39:05.122Z",
                  "image": "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/2A5F3HFL6YIZJAU2EQETUBRGSE.JPG",
                  "source": "The Washington Post",
                  "byline": "Aaron C. Davis"
                },
                {
                  "canonical_url": "/national-security/2025/10/27/hurricane-melissa-trump-venezuela-disaster-military/",
                  "headline": "Hurricane Melissa collides with U.S. military mission in Caribbean",
                  "display_date": "2025-10-27T23:11:54.109Z",
                  "image": "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/PGWN2IIMHEHWCKV3MWW3RALOKE.JPG",
                  "source": "The Washington Post",
                  "byline": "Dan Lamothe"
                }
              ]
            }
          ]
        }
        """.trimIndent()
        return Response
            .Builder()
            .code(201)
            .message(responseString)
            .request(request)
            .protocol(Protocol.HTTP_1_0)
            .body(
                responseString
                    .toByteArray()
                    .toResponseBody("application/json".toMediaTypeOrNull()),
            ).addHeader("content-type", "application/json")
            .build()
    }

    private fun dispatchSinglePersoPod(request: Request): Response {
        val responseString: String = """
            {
                "id": "user_123",
                "item_type": "podcast",
                "transcript_url": "null",
                "title": "South Korea corruption; Ukraine conflict; US immigration debate",
                "kicker": "Your Personal Podcast",
                "summary": "Personalized podcast covering 3 articles generated by IntroductionService",
                "audio_file_path": "null",
                "audio_duration": 408.372245,
                "total_characters": 423,
                "articles_used": [
                    {
                        "id": "OGSEZ7ERSFGADKY3HTOMLTESVI",
                        "canonical_url": "/world/2026/01/28/south-korea-bribery-first-lady-prosecutions/",
                        "headline": "South Korea’s former first lady jailed for bribery ahead of husband’s verdict",
                        "display_date": "2026-01-29T03:03:49.646Z",
                        "image": "https://cloudfront-us-east-1.images.arcpublishing.com/wapo/ESHOFL6YYBB4CWQJVRWFJKFR3A.jpg",
                        "publish_date": "2026-01-29",
                        "url": "/world/2026/01/28/south-korea-bribery-first-lady-prosecutions/",
                        "title": "South Korea’s former first lady jailed for bribery ahead of husband’s verdict",
                        "author": "Sammy Westfall",
                        "published_date": "2026-01-29T05:00:00Z",
                        "relevance_score": 0.30000000000000004,
                        "content_length": 535,
                        "byline": "Sammy Westfall",
                        "subtype": "default",
                        "label": {
                            "basic": {
                                "display": true,
                                "text": "Asia",
                                "url": "https://www.washingtonpost.com/world/asia-pacific/"
                            },
                            "transparency": {
                                "display": true,
                                "text": "News",
                                "url": ""
                            }
                        },
                        "section": "/world/asia-pacific"
                    },
                    {
                        "id": "TEHN3WLHHZFCTNXX3XHVQFHJVM",
                        "canonical_url": "/world/2026/01/29/ukraine-russia-trump-zelensky-negotiations/",
                        "headline": "Russia’s top diplomat rejects key part of deal to end war with Ukraine",
                        "display_date": "2026-01-29T14:52:33.503Z",
                        "image": "https://cloudfront-us-east-1.images.arcpublishing.com/wapo/IWVGNFNNO57L5GLIJU7JPPYKYY.JPG",
                        "publish_date": "2026-01-29",
                        "url": "/world/2026/01/29/ukraine-russia-trump-zelensky-negotiations/",
                        "title": "Russia’s top diplomat rejects key part of deal to end war with Ukraine",
                        "author": "David   Stern, Lizzie Johnson",
                        "published_date": "2026-01-29T05:00:00Z",
                        "relevance_score": 0.30000000000000004,
                        "content_length": 837,
                        "byline": "David   Stern, Lizzie Johnson",
                        "subtype": "default",
                        "label": {
                            "basic": {
                                "display": true,
                                "text": "Europe",
                                "url": "https://www.washingtonpost.com/world/europe/"
                            },
                            "transparency": {
                                "display": true,
                                "text": "News",
                                "url": ""
                            }
                        },
                        "section": "/world/europe"
                    },
                    {
                        "id": "QSNVBSDWYNHV5OT2PKULQKL6LM",
                        "canonical_url": "/politics/2026/01/29/trump-minnesota-immigration-maga-blowback/",
                        "headline": "Trump faces fresh MAGA blowback for efforts to ‘de-escalate’ in Minnesota",
                        "display_date": "2026-01-29T10:00:00.000Z",
                        "image": "https://cloudfront-us-east-1.images.arcpublishing.com/wapo/YTDCNWDHDXZQBC7EPQTCTOJDA4.jpg",
                        "publish_date": "2026-01-29",
                        "url": "/politics/2026/01/29/trump-minnesota-immigration-maga-blowback/",
                        "title": "Trump faces fresh MAGA blowback for efforts to ‘de-escalate’ in Minnesota",
                        "author": "Natalie Allison, Isaac Arnsdorf, Hannah Knowles",
                        "published_date": "2026-01-29T05:00:00Z",
                        "relevance_score": 0.30000000000000004,
                        "content_length": 1609,
                        "byline": "Natalie Allison, Isaac Arnsdorf, Hannah Knowles",
                        "subtype": "default",
                        "label": {
                            "basic": {
                                "display": true,
                                "text": "White House",
                                "url": "https://www.washingtonpost.com/politics/white-house/"
                            },
                            "story_length": {
                                "display": true,
                                "text": "3. Large (1000 words to 1500 words)",
                                "url": ""
                            },
                            "transparency": {
                                "display": true,
                                "text": "News",
                                "url": ""
                            },
                            "user_need": {
                                "display": true,
                                "text": "Educate me",
                                "url": ""
                            }
                        },
                        "section": "/politics/white-house"
                    }
                ],
                "image": "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/HFFPTEO6QZC6LEBOXFCQ5NVBAE.png",
                "generation_metadata": {
                    "is_fallback": false
                },
                "quality_metrics": null,
                "created_at": "2026-01-29T15:12:29.490060Z",
                "version": "2.0",
                "processing_time": null
            }
        """.trimIndent()
        return Response
            .Builder()
            .code(201)
            .message(responseString)
            .request(request)
            .protocol(Protocol.HTTP_1_0)
            .body(
                responseString
                    .toByteArray()
                    .toResponseBody("application/json".toMediaTypeOrNull()),
            ).addHeader("content-type", "application/json")
            .build()
    }


    private fun dispatchPersoConfigResponse(request: Request): Response {
        val responseString: String =
            """
            {
    "description": "Podcast selection configuration",
    "podcastConfigItems": [
        {
            "title": {
                "id": "topics",
                "displayName": "Podcast topics"
            },
            "items": [
                {
                    "id": "U.S.",
                    "displayName": "U.S."
                },
                {
                    "id": "World",
                    "displayName": "World"
                },
                {
                    "id": "Entertainment",
                    "displayName": "Entertainment"
                },
                {
                    "id": "Politics",
                    "displayName": "Politics"
                },
                {
                    "id": "Business",
                    "displayName": "Business"
                },
                {
                    "id": "Tech",
                    "displayName": "Tech"
                },
                {
                    "id": "Health",
                    "displayName": "Health"
                },
                {
                    "id": "Climate",
                    "displayName": "Climate"
                },
                {
                    "id": "Sports",
                    "displayName": "Sports"
                },
                {
                    "id": "D.C., Md. & Va.",
                    "displayName": "D.C., Md. & Va."
                }
            ],
            "selectionLimit": 3,
            "allowsCustomInput": false,
            "style": "chip"
        },
        {
            "title": {
                "id": "host_voice",
                "displayName": "Host voices"
            },
            "items": [
                {
                    "id": "gs0tAILXbY5DNrJrsM6F",
                    "displayName": "Batman and Robin"
                },
                {
                    "id": "MFZUKuGQUsGJPQjTS4wC",
                    "displayName": "Abbott and Costello"
                },
                {
                    "id": "jqcCZkN6Knx8BJ5TBdYR",
                    "displayName": "Lucy and Ethel"
                },
                {
                    "id": "UgBBYS2sOqTuMpoF3BR0",
                    "displayName": "Bert and Ernie"
                }
            ],
            "selectionLimit": 1,
            "allowsCustomInput": false,
            "style": "radio"
        },
        {
            "title": {
                "id": "length",
                "displayName": "Episode length"
            },
            "items": [
                {
                    "id": "Short (5-10 min)",
                    "displayName": "4 min"
                },
                {
                    "id": "Medium (10-20 min)",
                    "displayName": "6 min"
                },
                {
                    "id": "Long (20+ min)",
                    "displayName": "8 min"
                }
            ],
            "selectionLimit": 1,
            "allowsCustomInput": false,
            "style": "slider"
        }
    ],
    "message": null,
    "canGenerate": true
}
            """.trimIndent()
        return Response
            .Builder()
            .code(201)
            .message(responseString)
            .request(request)
            .protocol(Protocol.HTTP_1_0)
            .body(
                responseString
                    .toByteArray()
                    .toResponseBody("application/json".toMediaTypeOrNull()),
            ).addHeader("content-type", "application/json")
            .build()
    }

    private fun dispatchSummaryResponse(request: Request): Response {
        // Success
        val responseString: String =
            """
            {
                "id": "123456",
                "lmt": 3409825970,
                "url": "https://www.washingtonpost.com/elections/2024/03/19/election-2024-campaign-updates/",
                "headline": "Primary results, analysis in Ohio, Illinois and more",
                "summary": "- You can find the full results of each race <a href=\"https://www.washingtonpost.com/elections/results/2024/03/19/ohio-primary/\">here</a>.
                <br>
                - You can find the full results of each race <a href=\"https://www.washingtonpost.com/elections/results/2024/03/19/ohio-primary/\">here</a>.
                <br>
                - You can find the full results of each race <a href=\"https://www.washingtonpost.com/elections/results/2024/03/19/ohio-primary/\">here</a>.
                ",
                "disclaimer": "This content was generated by an AI and may not be 100% accurate.",
                "ratings": [
                { "value": 1, "description": "Good summary" },
                { "value": 2, "description": "Summary is good with minor edits" },
                { "value": 3, "description": "Poor summary, major edits needed" }
                ]
            }
            """.trimIndent()
        // Error
        val errorResponseString: String =
            """
            {
                "message": "Article summary not available.",
                "detail": "Article not included in current experiment."
            }
            """.trimIndent()
        HandlerThread.sleep(3000)
        if (Random.nextInt(0, 2) == 0) {
            return Response
                .Builder()
                .code(200)
                .message(responseString)
                .request(request)
                .protocol(Protocol.HTTP_1_0)
                .body(
                    responseString
                        .toByteArray()
                        .toResponseBody("application/json".toMediaTypeOrNull()),
                ).addHeader("content-type", "application/json")
                .build()
        } else {
            return Response
                .Builder()
                .code(404)
                .message(errorResponseString)
                .request(request)
                .protocol(Protocol.HTTP_1_0)
                .body(
                    errorResponseString
                        .toByteArray()
                        .toResponseBody("application/json".toMediaTypeOrNull()),
                ).addHeader("content-type", "application/json")
                .build()
        }
    }

    private fun dispatchSummaryFeedbackResponse(request: Request): Response {
        // Success
        val responseString: String = ""
        // Error
        val errorResponseString: String =
            """
            {
                "message": "status code based message.",
                "error": "appropriate error text"
            }
            """.trimIndent()
        HandlerThread.sleep(3000)
        if (Random.nextInt(0, 2) == 0) {
            return Response
                .Builder()
                .code(201)
                .message(responseString)
                .request(request)
                .protocol(Protocol.HTTP_1_0)
                .body(
                    responseString
                        .toByteArray()
                        .toResponseBody("application/json".toMediaTypeOrNull()),
                ).addHeader("content-type", "application/json")
                .build()
        } else {
            return Response
                .Builder()
                .code(404)
                .message(errorResponseString)
                .request(request)
                .protocol(Protocol.HTTP_1_0)
                .body(
                    errorResponseString
                        .toByteArray()
                        .toResponseBody("application/json".toMediaTypeOrNull()),
                ).addHeader("content-type", "application/json")
                .build()
        }
    }

    private fun dispatchForYouAiXpResponse(request: Request): Response {
        val responseString: String =
            """
            {
              "algorithm": "stack",
              "recommendations": [
                {
                  "_id": "KD5V7MJRBRBZTCIM2KT5JB37YU",
                  "article_id": "contentapi://KD5V7MJRBRBZTCIM2KT5JB37YU",
                  "url": "/style/media/2024/03/18/steve-doocy-fox-news-dissent/",
                  "canonical_url": "/style/media/2024/03/18/steve-doocy-fox-news-dissent/",
                  "normalized_url": "/style/media/2024/03/18/steve-doocy-fox-news-dissent/",
                  "authors": [
                    {
                      "name": "Jeremy Barr",
                      "url": "https://www.washingtonpost.com/people/jeremy-barr/"
                    }
                  ],
                  "category": "style",
                  "description": {
                    "basic": "Steve Doocy, the affable longtime “Fox & Friends” host, is challenging GOP orthodoxy to the dismay of Sean Hannity, Donald Trump and other Republicans leaders"
                  },
                  "display_date": "2024-03-18T10:00:46.988Z",
                  "first_publish_date": "2024-03-18T10:00:47.061Z",
                  "last_updated_date": "2024-03-16T04:15:01.198Z",
                  "headline": "On Fox News, Steve Doocy has become the unexpected voice of dissent",
                  "image_url": "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/VIHD2JWJOH35K5XCYLHIHCUPCE.jpg",
                  "publish_date": "2024-03-18T10:00:47.061Z",
                  "section": "/style/media",
                  "sections": [
                    "/style/media",
                    "/style"
                  ],
                  "tags": [
                    {
                      "additional_properties": {
                        "ellipsis_managed": true
                      },
                      "description": "The story-has-edit-branch tag specifies that this story has both an edit and a default branch",
                      "slug": "story-has-edit-branch",
                      "text": "story-has-edit-branch"
                    },
                    {
                      "description": "Send this to methode before it publishes.",
                      "slug": "methode-draft",
                      "text": "methode-draft"
                    },
                    {
                      "description": "collections-media",
                      "slug": "collections-media",
                      "text": "collections-media"
                    },
                    {
                      "additional_properties": {
                        "ellipsis_managed": true
                      },
                      "description": "The template that was used to create this story",
                      "slug": "created-from-template-id-CN4JK5DAE5GGJN442U2E2T4GUY",
                      "text": "Created from Template ID CN4JK5DAE5GGJN442U2E2T4GUY"
                    },
                    {
                      "additional_properties": {
                        "ellipsis_managed": true
                      },
                      "slug": "audio-article",
                      "text": "audio-article"
                    }
                  ],
                  "source": "washpost",
                  "source_type": "staff",
                  "type": "story",
                  "subtype": "default",
                  "days_since_publish": 0.010961248344907408,
                  "additional_properties": {
                    "audio_article": {
                      "enabled": true
                    },
                    "is_published": false
                  },
                  "headlines": {
                    "basic": "On Fox News, Steve Doocy has become the unexpected voice of dissent"
                  },
                  "label": {
                    "basic": {
                      "display": true,
                      "text": "The Media",
                      "url": "https://www.washingtonpost.com/style/media/"
                    },
                    "transparency": {
                      "display": true,
                      "text": "News",
                      "url": ""
                    }
                  },
                  "label_display": {
                    "basic": {
                      "text": "The Media",
                      "url": "https://www.washingtonpost.com/style/media/"
                    },
                    "transparency": {
                      "text": "",
                      "url": ""
                    }
                  },
                  "promo_items": {
                    "basic": {
                      "url": "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/VIHD2JWJOH35K5XCYLHIHCUPCE.jpg",
                      "additional_properties": {
                        "size_normalized_url": "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/VIHD2JWJOH35K5XCYLHIHCUPCE_size-normalized.jpg"
                      },
                      "width": 5477,
                      "height": 3651,
                      "aspect_ratio": null,
                      "credits_caption_display": "(Roy Rochlin/Getty Images)",
                      "credits_display": "(Roy Rochlin/Getty Images)",
                      "type": "image"
                    }
                  },
                  "credits": {
                    "by": [
                      {
                        "_id": "barrj",
                        "name": "Jeremy Barr",
                        "additional_properties": {
                          "original": {
                            "byline": "Jeremy Barr",
                            "bio_page": "https://www.washingtonpost.com/people/jeremy-barr/"
                          }
                        },
                        "image": {
                          "url": "https://s3.amazonaws.com/arc-authors/washpost/4ff7f94f-70a7-411d-ac23-3bf436fb06f0.jpg",
                          "version": "0.5.8"
                        }
                      }
                    ]
                  },
                  "score": 45255.0,
                  "rec_reason": "popularity",
                  "rank": 1,
                  "summary": {
                        "id": "123456",
                        "lmt": 1710874990763,
                        "url": "https://www.washingtonpost.com/style/media/2024/03/18/steve-doocy-fox-news-dissent/",
                        "headline": "Primary results, analysis in Ohio, Illinois and more",
                        "summary": "- You can find the full results of each race <a href=\"https://www.washingtonpost.com/elections/results/2024/03/19/ohio-primary/\">here</a>.
                        <br>
                        - You can find the full results of each race <a href=\"https://www.washingtonpost.com/elections/results/2024/03/19/ohio-primary/\">here</a>.
                        <br>
                        - You can find the full results of each race <a href=\"https://www.washingtonpost.com/elections/results/2024/03/19/ohio-primary/\">here</a>.
                        ",
                        "disclaimer": "This content was generated by an AI and may not be 100% accurate.",
                        "ratings": [
                        { "value": 1, "description": "Good summary" },
                        { "value": 2, "description": "Summary is good with minor edits" },
                        { "value": 3, "description": "Poor summary, major edits needed" }
                        ]
                    }
                }
              ]
            }
            """.trimIndent()

        val resp: String =
            """
            {
                "algorithm": "stack",
                "recommendations": [
                    {
                      "additional_properties": {
                        "apple_news": {
                          "exclude_apple_news": false,
                          "is_developing": false
                        },
                        "audio_article": {
                          "automated": {
                            "generate": true,
                            "manifest_url": "https://audio-articles.lionfish.media.aws.wapo.pub/IR4OUBEGX5GBZB7I3PQB3FL3LI/20240318-125675.754/manifest.json",
                            "voices": [
                              "AVA",
                              "MATTHEW"
                            ]
                          },
                          "enabled": true,
                          "type": "automated"
                        },
                        "audio_article_ads_url": "https://podcast.washpostpodcasts.com/washpost-audio-articles/IR4OUBEGX5GBZB7I3PQB3FL3LI/20240318-125675.754/current.mp3?awCollectionId=/lifestyle/food&awEpisodeId=IR4OUBEGX5GBZB7I3PQB3FL3LI&tags=chat-pack%7Cfood-chat-pack%7Cfoodhero%7Caudio-article",
                        "audio_article_enabled": true,
                        "audio_article_raw_url": "https://audio-articles.lionfish.media.aws.wapo.pub/IR4OUBEGX5GBZB7I3PQB3FL3LI/20240318-125675.754/current.mp3",
                        "clipboard": {},
                        "first_display_date": "2024-03-12T18:00:54.404Z",
                        "has_published_copy": true,
                        "is_published": true,
                        "lead_art": {
                          "_id": "KQ3R5PLMQVGV3DOGSDCJTHJIY4",
                          "additional_properties": {
                            "_id": "X64RQKGGHRA5BIG5HEFC7LNEJE",
                            "categoryFilter": "staff",
                            "comments": [],
                            "editors_pick": false,
                            "file_size": 936806,
                            "fullSizeResizeUrl": "/photo/resize/bJaobwHBj_nv_Or7hX-67ALBSOk=/arc-anglerfish-washpost-prod-washpost/public/KQ3R5PLMQVGV3DOGSDCJTHJIY4.jpg",
                            "galleries": [],
                            "image_orientation": "horizontal",
                            "ingestionMethod": "manual",
                            "iptc": {
                              "populated": true
                            },
                            "largeResizeUrl": "/photo/resize/cIgymEf4YuyYQrZWR8UW9vmtswk=/1440x0/arc-anglerfish-washpost-prod-washpost/public/KQ3R5PLMQVGV3DOGSDCJTHJIY4.jpg",
                            "last_published_date": "2024-03-12T19:05:49Z",
                            "manuallySelectedTime": 0,
                            "mark_for_print": {
                              "eligible": true,
                              "failed": false,
                              "receivedOn": "2024-03-15T19:42:34Z"
                            },
                            "mime_type": "image/jpeg",
                            "originalName": "food-chat-pack-tom-kids2.jpg",
                            "originalUrl": "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/KQ3R5PLMQVGV3DOGSDCJTHJIY4.jpg",
                            "owner": "cece.pascual@washpost.com",
                            "proxyUrl": "/photo/resize/bJaobwHBj_nv_Or7hX-67ALBSOk=/arc-anglerfish-washpost-prod-washpost/public/KQ3R5PLMQVGV3DOGSDCJTHJIY4.jpg",
                            "published": true,
                            "resizeUrl": "https://www.washingtonpost.com/resizer/bJaobwHBj_nv_Or7hX-67ALBSOk=/arc-anglerfish-washpost-prod-washpost/public/KQ3R5PLMQVGV3DOGSDCJTHJIY4.jpg",
                            "resize_urls": {
                              "full": "/photo/api/v2/photos/KQ3R5PLMQVGV3DOGSDCJTHJIY4/resize/full",
                              "jpeg-full": "/photo/api/v2/photos/KQ3R5PLMQVGV3DOGSDCJTHJIY4/resize/jpeg-full",
                              "large": "/photo/api/v2/photos/KQ3R5PLMQVGV3DOGSDCJTHJIY4/resize/large",
                              "medium": "/photo/api/v2/photos/KQ3R5PLMQVGV3DOGSDCJTHJIY4/resize/medium",
                              "small": "/photo/api/v2/photos/KQ3R5PLMQVGV3DOGSDCJTHJIY4/resize/small"
                            },
                            "restricted": false,
                            "syndication": {
                              "getty": {
                                "disabled": false,
                                "eligible": false,
                                "failed": false
                              }
                            },
                            "takenOn": "2024-03-12T17:12:26Z",
                            "template_id": 56,
                            "thumbnailResizeUrl": "https://www.washingtonpost.com/resizer/dzMCVJlW109JKl0Wti9Do-4OguI=/300x0/arc-anglerfish-washpost-prod-washpost/public/KQ3R5PLMQVGV3DOGSDCJTHJIY4.jpg",
                            "usage_instructions": "**ONE TIME USE ONLY. MANDATORY CREDIT. NO SALES. NO TRADES. MUST LICENSE FOR REUSE. NO NEWS SERVICE.** Reach out to designeds@washpost.com to inquire about recommissioning an illustration for additional use – which may take time and will incur a cost.",
                            "version": 4,
                            "workflow_privileges": {
                              "crop": {
                                "allowed": true,
                                "reason": null
                              },
                              "delete_photo": {
                                "allowed": true,
                                "reason": null
                              },
                              "initial_publish": {
                                "allowed": true,
                                "reason": null
                              },
                              "mark_editors_pick": {
                                "allowed": true,
                                "reason": null
                              },
                              "mark_on_hold": {
                                "allowed": false,
                                "reason": null
                              },
                              "modify_binary_metadata": {
                                "allowed": false,
                                "reason": null
                              },
                              "modify_caption": {
                                "allowed": true,
                                "reason": null
                              },
                              "publish": {
                                "allowed": true,
                                "reason": null
                              },
                              "role_name": null,
                              "secondary_publish": {
                                "allowed": true,
                                "reason": null
                              },
                              "set_restricted": {
                                "allowed": true,
                                "reason": null
                              },
                              "unpublish": {
                                "allowed": true,
                                "reason": null
                              },
                              "workflow_enabled": false
                            }
                          },
                          "address": {},
                          "caption": "",
                          "caption_display": "",
                          "copyright": "The Washington Post",
                          "created_date": "2024-03-12T17:12:26Z",
                          "credits": {
                            "affiliation": [
                              {
                                "name": "The Washington Post",
                                "type": "author"
                              }
                            ],
                            "by": [
                              {
                                "byline": "Washington Post illustration; The Washington Post; iStock",
                                "name": "Washington Post illustration; The Washington Post; iStock",
                                "type": "author"
                              }
                            ]
                          },
                          "credits_caption_display": "(Washington Post illustration; The Washington Post; iStock)",
                          "credits_display": "(Washington Post illustration; The Washington Post; iStock)",
                          "distributor": {
                            "category": "staff",
                            "mode": "custom",
                            "name": "The Washington Post"
                          },
                          "height": 1360,
                          "image_type": "illustration",
                          "last_updated_date": "2024-03-15T19:42:35Z",
                          "licensable": false,
                          "owner": {
                            "id": "washpost",
                            "sponsored": false
                          },
                          "slug": "food-chat-pack-tom-kids",
                          "source": {
                            "edit_url": "https://washpost.arcpublishing.com/photo/KQ3R5PLMQVGV3DOGSDCJTHJIY4",
                            "name": "The Washington Post",
                            "source_type": "other",
                            "system": "Anglerfish"
                          },
                          "status": "Approved",
                          "subtitle": "Chat Pack: How do I prepare my child for restaurants?",
                          "syndication": {
                            "search": true
                          },
                          "type": "image",
                          "url": "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/KQ3R5PLMQVGV3DOGSDCJTHJIY4.jpg",
                          "version": "0.10.4",
                          "width": 2040
                        },
                        "news_service": {
                          "attention_line": "",
                          "categories": {
                            "BookWorld": [],
                            "Budget_Advisory": [],
                            "Business": [],
                            "Entertainment": [],
                            "Features": [],
                            "Health": [],
                            "National": [],
                            "Opinion": [],
                            "Spanish": [],
                            "Sports": [],
                            "Technology": [],
                            "World": []
                          },
                          "category": "",
                          "clone_to_news_service": false,
                          "correction": false,
                          "nss_site_correction": false,
                          "print_only": false,
                          "release_date": "",
                          "writers_group": false
                        },
                        "nlp": {
                          "key_takeaways": []
                        },
                        "page_title": "How to teach kids to dine at restaurants - The Washington Post",
                        "parse_errors": [],
                        "picklistOverride": [
                          {
                            "path": "copyright",
                            "reset": false
                          }
                        ],
                        "present_display_date": "2024-03-12T18:00:54.404Z",
                        "present_publish_date": "2024-03-12T19:05:51.466Z",
                        "pubble_app_id": "",
                        "publish_date": "2024-03-12T19:05:51.466Z",
                        "publish_embargo_indefinitely": false,
                        "seo_canonical_url": "",
                        "time_to_listen": "144 seconds",
                        "time_to_read": "3 minutes",
                        "topic_description": "",
                        "topic_publication_date": "2024-03-20T04:00:00.000Z",
                        "tracking": {
                          "commercial_node": ""
                        },
                        "truncate_posts": true
                      },
                      "article_id": null,
                      "authors": null,
                      "canonical_url": "/food/2024/03/12/children-at-restaurants-dining-kids/",
                      "category": null,
                      "credits": {
                        "audio_narrators": [],
                        "audio_producers": [],
                        "by": [
                          {
                            "_id": "sietsematw",
                            "additional_properties": {
                              "original": {
                                "_id": "sietsematw",
                                "awards": [],
                                "bio": "Tom Sietsema has been The Washington Post's food critic since 2000. He previously worked for the Microsoft Corp., where he launched sidewalk.com; the Seattle Post-Intelligencer; the San Francisco Chronicle; and the Milwaukee Journal. He has also written for Food & Wine.",
                                "bio_page": "https://www.washingtonpost.com/people/tom-sietsema/",
                                "books": [],
                                "byline": "Tom Sietsema",
                                "custom_washpost_desk_name_1": "Features",
                                "custom_washpost_desk_name_2": "Features - Food",
                                "desk": "Magazine",
                                "education": [
                                  {
                                    "name": "Georgetown University, School of Foreign Service"
                                  }
                                ],
                                "email": "tom.sietsema@washpost.com",
                                "employeeID": "000009042",
                                "expertise": "Food critic",
                                "facebook": "https://www.facebook.com/tom.sietsema",
                                "firstName": "Tom",
                                "follow-author": true,
                                "follow-newsletter-id": "post_newsletter493",
                                "image": "https://s3.amazonaws.com/arc-authors/washpost/4c30ad52-6982-436f-b195-fa9db8c36b5b.png",
                                "isWorkdayLoad": true,
                                "jobProfile": "Columnist",
                                "lastName": "Sietsema",
                                "last_updated": "2018-04-04T20:24:15.747Z",
                                "last_updated_date": "2024-03-15T09:38:04.482Z",
                                "location": "Washington, D.C.",
                                "longBio": "Tom Sietsema has been The Washington Post's food critic since 2000. In leaner years, he worked for the Microsoft Corp., where he launched sidewalk.com; the Seattle Post-Intelligencer; the San Francisco Chronicle; and the Milwaukee Journal. A graduate of the School of Foreign Service at Georgetown University, he has also written for Food & Wine, Gourmet, GQ, Travel & Leisure and other national publications. In 2016, he received an award from the James Beard Foundation for his series identifying and rating the \"10 Best Food Cities in America\" the previous year.",
                                "networkID": "SIETSEMATW",
                                "newsDesk": "Features",
                                "newsJobCategory": "Reporter",
                                "podcasts": [],
                                "role": "Columnist",
                                "rss": "http://feeds.washingtonpost.com/rss/linksets/lifestyle/dining",
                                "slug": "tom-sietsema",
                                "status": "Active",
                                "subDesk": "Features - Food",
                                "subDeskHead": "Joe Yonan",
                                "twitter": "@tomsietsema",
                                "workerType": "Employee"
                              }
                            },
                            "description": "Tom Sietsema has been The Washington Post's food critic since 2000. He previously worked for the Microsoft Corp., where he launched sidewalk.com; the Seattle Post-Intelligencer; the San Francisco Chronicle; and the Milwaukee Journal. He has also written for Food & Wine.",
                            "image": {
                              "url": "https://s3.amazonaws.com/arc-authors/washpost/4c30ad52-6982-436f-b195-fa9db8c36b5b.png",
                              "version": "0.5.8"
                            },
                            "name": "Tom Sietsema",
                            "org": "Washington, D.C.",
                            "slug": "tom-sietsema",
                            "socialLinks": [
                              {
                                "deprecated": true,
                                "deprecation_msg": "Please use social_links.",
                                "site": "email",
                                "url": "tom.sietsema@washpost.com"
                              },
                              {
                                "deprecated": true,
                                "deprecation_msg": "Please use social_links.",
                                "site": "facebook",
                                "url": "https://www.facebook.com/tom.sietsema"
                              },
                              {
                                "deprecated": true,
                                "deprecation_msg": "Please use social_links.",
                                "site": "twitter",
                                "url": "@tomsietsema"
                              },
                              {
                                "deprecated": true,
                                "deprecation_msg": "Please use social_links.",
                                "site": "rss",
                                "url": "http://feeds.washingtonpost.com/rss/linksets/lifestyle/dining"
                              }
                            ],
                            "social_links": [
                              {
                                "site": "email",
                                "url": "tom.sietsema@washpost.com"
                              },
                              {
                                "site": "facebook",
                                "url": "https://www.facebook.com/tom.sietsema"
                              },
                              {
                                "site": "twitter",
                                "url": "@tomsietsema"
                              },
                              {
                                "site": "rss",
                                "url": "http://feeds.washingtonpost.com/rss/linksets/lifestyle/dining"
                              }
                            ],
                            "type": "author",
                            "url": "https://www.washingtonpost.com/people/tom-sietsema/",
                            "version": "0.5.8"
                          }
                        ]
                      },
                      "days_since_publish": null,
                      "description": {
                        "basic": "Taking a young child to a restaurant for the first time takes a little prep. Post Food critic Tom Sietsema and readers offer advice."
                      },
                      "display_date": "2024-03-12T18:00:54.404Z",
                      "first_publish_date": "2024-03-12T18:00:54.471Z",
                      "headline": "Ask a food critic: How can I teach my child to eat at restaurants?",
                      "headlines": {
                        "apple_news": "",
                        "basic": "Ask a food critic: How can I teach my child to eat at restaurants?",
                        "meta_title": "How to teach kids to dine at restaurants",
                        "mobile": "",
                        "native": "",
                        "print": "",
                        "tablet": "",
                        "url": "children-at-restaurants-dining-kids",
                        "web": ""
                      },
                      "image_url": null,
                      "label": {
                        "basic": {
                          "display": true,
                          "text": "Food",
                          "url": "https://www.washingtonpost.com/food/"
                        },
                        "transparency": {
                          "display": true,
                          "text": "Advice",
                          "url": ""
                        }
                      },
                      "label_display": {
                        "basic": {
                          "text": "Food",
                          "url": "https://www.washingtonpost.com/food/"
                        },
                        "transparency": {
                          "text": "Advice",
                          "url": ""
                        }
                      },
                      "last_updated_date": "2024-03-18T16:56:35.634Z",
                      "normalized_url": null,
                      "promo_items": {
                        "basic": {
                          "_id": "KQ3R5PLMQVGV3DOGSDCJTHJIY4",
                          "additional_properties": {
                            "_id": "X64RQKGGHRA5BIG5HEFC7LNEJE",
                            "categoryFilter": "staff",
                            "comments": [],
                            "editors_pick": false,
                            "file_size": 936806,
                            "fullSizeResizeUrl": "/photo/resize/bJaobwHBj_nv_Or7hX-67ALBSOk=/arc-anglerfish-washpost-prod-washpost/public/KQ3R5PLMQVGV3DOGSDCJTHJIY4.jpg",
                            "galleries": [],
                            "image_orientation": "horizontal",
                            "ingestionMethod": "manual",
                            "iptc": {
                              "populated": true
                            },
                            "largeResizeUrl": "/photo/resize/cIgymEf4YuyYQrZWR8UW9vmtswk=/1440x0/arc-anglerfish-washpost-prod-washpost/public/KQ3R5PLMQVGV3DOGSDCJTHJIY4.jpg",
                            "last_published_date": "2024-03-12T19:05:49Z",
                            "manuallySelectedTime": 0,
                            "mark_for_print": {
                              "eligible": true,
                              "failed": false,
                              "receivedOn": "2024-03-15T19:42:34Z"
                            },
                            "mime_type": "image/jpeg",
                            "originalName": "food-chat-pack-tom-kids2.jpg",
                            "originalUrl": "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/KQ3R5PLMQVGV3DOGSDCJTHJIY4.jpg",
                            "owner": "cece.pascual@washpost.com",
                            "proxyUrl": "/photo/resize/bJaobwHBj_nv_Or7hX-67ALBSOk=/arc-anglerfish-washpost-prod-washpost/public/KQ3R5PLMQVGV3DOGSDCJTHJIY4.jpg",
                            "published": true,
                            "resizeUrl": "https://www.washingtonpost.com/resizer/bJaobwHBj_nv_Or7hX-67ALBSOk=/arc-anglerfish-washpost-prod-washpost/public/KQ3R5PLMQVGV3DOGSDCJTHJIY4.jpg",
                            "resize_urls": {
                              "full": "/photo/api/v2/photos/KQ3R5PLMQVGV3DOGSDCJTHJIY4/resize/full",
                              "jpeg-full": "/photo/api/v2/photos/KQ3R5PLMQVGV3DOGSDCJTHJIY4/resize/jpeg-full",
                              "large": "/photo/api/v2/photos/KQ3R5PLMQVGV3DOGSDCJTHJIY4/resize/large",
                              "medium": "/photo/api/v2/photos/KQ3R5PLMQVGV3DOGSDCJTHJIY4/resize/medium",
                              "small": "/photo/api/v2/photos/KQ3R5PLMQVGV3DOGSDCJTHJIY4/resize/small"
                            },
                            "restricted": false,
                            "syndication": {
                              "getty": {
                                "disabled": false,
                                "eligible": false,
                                "failed": false
                              }
                            },
                            "takenOn": "2024-03-12T17:12:26Z",
                            "template_id": 56,
                            "thumbnailResizeUrl": "https://www.washingtonpost.com/resizer/dzMCVJlW109JKl0Wti9Do-4OguI=/300x0/arc-anglerfish-washpost-prod-washpost/public/KQ3R5PLMQVGV3DOGSDCJTHJIY4.jpg",
                            "usage_instructions": "**ONE TIME USE ONLY. MANDATORY CREDIT. NO SALES. NO TRADES. MUST LICENSE FOR REUSE. NO NEWS SERVICE.** Reach out to designeds@washpost.com to inquire about recommissioning an illustration for additional use – which may take time and will incur a cost.",
                            "version": 4,
                            "workflow_privileges": {
                              "crop": {
                                "allowed": true,
                                "reason": null
                              },
                              "delete_photo": {
                                "allowed": true,
                                "reason": null
                              },
                              "initial_publish": {
                                "allowed": true,
                                "reason": null
                              },
                              "mark_editors_pick": {
                                "allowed": true,
                                "reason": null
                              },
                              "mark_on_hold": {
                                "allowed": false,
                                "reason": null
                              },
                              "modify_binary_metadata": {
                                "allowed": false,
                                "reason": null
                              },
                              "modify_caption": {
                                "allowed": true,
                                "reason": null
                              },
                              "publish": {
                                "allowed": true,
                                "reason": null
                              },
                              "role_name": null,
                              "secondary_publish": {
                                "allowed": true,
                                "reason": null
                              },
                              "set_restricted": {
                                "allowed": true,
                                "reason": null
                              },
                              "unpublish": {
                                "allowed": true,
                                "reason": null
                              },
                              "workflow_enabled": false
                            }
                          },
                          "address": {},
                          "caption": "",
                          "caption_display": "",
                          "copyright": "The Washington Post",
                          "created_date": "2024-03-12T17:12:26Z",
                          "credits": {
                            "affiliation": [
                              {
                                "name": "The Washington Post",
                                "type": "author"
                              }
                            ],
                            "by": [
                              {
                                "byline": "Washington Post illustration; The Washington Post; iStock",
                                "name": "Washington Post illustration; The Washington Post; iStock",
                                "type": "author"
                              }
                            ]
                          },
                          "credits_caption_display": "(Washington Post illustration; The Washington Post; iStock)",
                          "credits_display": "(Washington Post illustration; The Washington Post; iStock)",
                          "distributor": {
                            "category": "staff",
                            "mode": "custom",
                            "name": "The Washington Post"
                          },
                          "height": 1360,
                          "image_type": "illustration",
                          "last_updated_date": "2024-03-15T19:42:35Z",
                          "licensable": false,
                          "owner": {
                            "id": "washpost",
                            "sponsored": false
                          },
                          "slug": "food-chat-pack-tom-kids",
                          "source": {
                            "edit_url": "https://washpost.arcpublishing.com/photo/KQ3R5PLMQVGV3DOGSDCJTHJIY4",
                            "name": "The Washington Post",
                            "source_type": "other",
                            "system": "Anglerfish"
                          },
                          "status": "Approved",
                          "subtitle": "Chat Pack: How do I prepare my child for restaurants?",
                          "syndication": {
                            "search": true
                          },
                          "type": "image",
                          "url": "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/KQ3R5PLMQVGV3DOGSDCJTHJIY4.jpg",
                          "version": "0.10.4",
                          "width": 2040
                        }
                      },
                      "publish_date": "2024-03-18T16:56:35.670Z",
                      "rank": null,
                      "rec_reason": null,
                      "score": null,
                      "section": null,
                      "sections": null,
                      "source": "washpost",
                      "source_type": null,
                      "subtype": "default",
                      "summary": {
                        "id": "8e3be53_IR4OUBEGX5GBZB7I3PQB3FL3LI",
                        "lmt": 1710780995670,
                        "url": "/food/2024/03/12/children-at-restaurants-dining-kids/",
                        "headline": "Ask a food critic: How can I teach my child to eat at restaurants?",
                        "summary": "The article provides tips for parents taking their young children to restaurants for the first time, emphasizing good behavior, interaction with others, and avoiding noisy electronic devices. It also suggests using coloring books or small games for distraction, and knowing when to leave if the child becomes fussy.",
                        "disclaimer": "This content was generated by an AI and may not be 100% accurate.",
                        "ratings": [
                          {
                            "value": 0,
                            "description": "Good summary"
                          },
                          {
                            "value": 1,
                            "description": "Summary is good with minor edits"
                          },
                          {
                            "value": 2,
                            "description": "Poor summary, major edits needed"
                          }
                        ]
                      },
                      "tags": null,
                      "type": "story",
                      "url": "/food/2024/03/12/children-at-restaurants-dining-kids/"
                    }
                ]
            }
            """.trimIndent()
        return Response
            .Builder()
            .code(201)
            .message(resp)
            .request(request)
            .protocol(Protocol.HTTP_1_0)
            .body(
                resp
                    .toByteArray()
                    .toResponseBody("application/json".toMediaTypeOrNull()),
            ).addHeader("content-type", "application/json")
            .build()
    }

    companion object {
        /**
         * Custom header
         */
        const val MOCK_RESPONSE = "MOCK_RESPONSE"
        const val SUMMARY = "SUMMARY"
        const val SUMMARY_FEEDBACK = "SUMMARY_FEEDBACK"
        const val FOR_YOU_AI_XP = "FOR_YOU_AI_XP"
        const val PERSO_CONFIG = "PERSO_CONFIG"
        const val GENERATED_PODCASTS = "GENERATED_PODCASTS"
        const val GENERATE_PODCAST = "GENERATE_PODCAST"
        const val SINGLE_PODCAST = "SINGLE_PODCAST"
    }
}