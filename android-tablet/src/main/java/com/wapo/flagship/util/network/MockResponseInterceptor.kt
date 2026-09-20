package com.wapo.flagship.util.network

import android.os.HandlerThread
import com.washingtonpost.android.BuildConfig
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

                    else -> null
                }
            if (response != null) return response
        }
        return chain.proceed(chain.request())
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
                "displayName": "Select a topic"
            },
            "items": [
                {
                    "id": "Business",
                    "displayName": "Business"
                },
                {
                    "id": "Politics",
                    "displayName": "Politics"
                },
                {
                    "id": "Sports",
                    "displayName": "Sports"
                },
                {
                    "id": "War and unrest",
                    "displayName": "War and unrest"
                },
                {
                    "id": "Elections",
                    "displayName": "Elections"
                },
                {
                    "id": "Education",
                    "displayName": "Education"
                },
                {
                    "id": "Arts and entertainment",
                    "displayName": "Arts and entertainment"
                },
                {
                    "id": "Health",
                    "displayName": "Health"
                },
                {
                    "id": "Media",
                    "displayName": "Media"
                }
            ],
            "selectionLimit": 3,
            "allowsCustomInput": false
        },
        {
            "title": {
                "id": "host_voice",
                "displayName": "Select a voice"
            },
            "items": [
                {
                    "id": "gs0tAILXbY5DNrJrsM6F",
                    "displayName": "Michael"
                },
                {
                    "id": "kdmDKE6EkgrWrrykO9Qt",
                    "displayName": "Michelle"
                },
                {
                    "id": "MFZUKuGQUsGJPQjTS4wC",
                    "displayName": "Jon"
                },
                {
                    "id": "jqcCZkN6Knx8BJ5TBdYR",
                    "displayName": "Zara"
                },
                {
                    "id": "UgBBYS2sOqTuMpoF3BR0",
                    "displayName": "Mark"
                },
                {
                    "id": "56AoDkrOh6qfVPDXZ7Pt",
                    "displayName": "Cassidy"
                },
                {
                    "id": "aMSt68OGf4xUZAnLpTU8",
                    "displayName": "Juniper"
                },
                {
                    "id": "vBKc2FfBKJfcZNyEt1n6",
                    "displayName": "Finn"
                }
            ],
            "selectionLimit": 1,
            "allowsCustomInput": false
        },
        {
            "title": {
                "id": "length",
                "displayName": "Select podcast length"
            },
            "items": [
                {
                    "id": "Short (5-10 min)",
                    "displayName": "Short (5-10 min)"
                },
                {
                    "id": "Medium (10-20 min)",
                    "displayName": "Medium (10-20 min)"
                },
                {
                    "id": "Long (20+ min)",
                    "displayName": "Long (20+ min)"
                }
            ],
            "selectionLimit": 1,
            "allowsCustomInput": false
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
    }
}
