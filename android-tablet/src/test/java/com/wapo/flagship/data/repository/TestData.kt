/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.data.repository

import com.wapo.flagship.features.personalizedpodcasts.model.PersonalizedPodcast
import com.wapo.flagship.features.sections.model.Section
import com.wapo.flagship.features.sections.model.SectionType
import com.washingtonpost.foryou.data.HabitTilesResponse
import com.washingtonpost.foryou.data.Tile
import com.washingtonpost.userhistory.models.PushNotificationViewItem
import com.washingtonpost.userhistory.models.ScrollDepthItem
import com.washingtonpost.userhistory.remote.UserHistoryEventType
import io.mockk.mockk

object TestData {

    object Tile {
        const val STATUS = "status"
        const val REQUEST_ID = "requestId"
        const val TILE_COUNT = 1

        const val TEST_GROUP = "test_group"

        val mockPersoPodcastMetadata = mockk<PersonalizedPodcast>(relaxed = true)

        val TILE_ONE = Tile(
            score = 2.0,
            tileCategory = "tile_category",
            imageUrl = "image_url",
            contextLabel = "context_label",
            contextIndicator = "context_indicator",
            tileLabel = "tile_label",
            tileLink = "tile_link",
            tileLabelBehavior = "tile_category_detail",
            tileCategoryDetail = "tile_category_detail",
            position = 1,
            persoPodcastMetadata = mockPersoPodcastMetadata
        )

        val TILE_TWO = TILE_ONE.copy(
            score = 3.0
        )

        val fakeTestData = HabitTilesResponse(
            status = STATUS,
            requestId = REQUEST_ID,
            tileCount = TILE_COUNT,
            tiles = listOf(TILE_ONE, TILE_TWO),
            testGroup = TEST_GROUP
        )
    }

    object UserHistory {
        fun createDummyScrollDepthItem(
            articleId: String = "article1",
            pageViewId: String = "pv1",
            deepestScrollIndex: Int = 1,
            deepestScrollId: String = "id1"
        ): ScrollDepthItem {
            return ScrollDepthItem(
                articleId = articleId,
                wapoLoginId = "login1",
                jucId = "juc1",
                jtId = "jt1",
                pageViewId = pageViewId,
                totalElements = 10,
                deepestScrollIndex = deepestScrollIndex,
                deepestScrollId = deepestScrollId,
                mostRecentScrollIndex = 1,
                mostRecentScrollId = "id1",
                clientEventTime = "time"
            )
        }

        val pushNotificationViewItem = PushNotificationViewItem(
            eventType = UserHistoryEventType.PUSH_ORIGINATED.eventName,
            articleId = "push_event",
            eventSubType = "push_notification",
            canonicalUrl = "/some-article-url",
            loginId = "user-login-id",
            jucId = "user-juc-id",
            clientEventTime = "2024-01-01T12:00:00Z",
            interfase = "android",
            surface = "push_notification",
            surfaceVariant = "phone",
            pushId = "push-id-123",
            pushCategory = "breaking_news",
            testGroup = "A",
            clicked = true,
            shared = false,
            deviceId = "device-id-456",
            appVersion = "10.0.0",
            devicePlatform = "android"
        )
    }

    object Section {
        val testSections = listOf(
            Section("1", "/.", "Top Stories", "Top Stories", sectionType = SectionType.SECTION),
            Section("2", "/politics", "Politics", "Politics", sectionType = SectionType.SECTION),
            Section("3", "/opinions", "Opinions", "Opinions", sectionType = SectionType.SECTION),
            Section("for-you", "/for-you", "For You", "For You", sectionType = SectionType.SECTION),
        )
    }
}
