// Copyright (c) 2023 The Washington Post. All rights reserved.

package com.wapo.flagship.features.preferencesapi

import com.wapo.flagship.features.preferencesapi.repo.TopicNotificationsRepo
import org.junit.Assert.assertEquals
import org.junit.Test

class TopicNotificationsRepoTest {

    @Test
    fun testTopicListsMerge() {
        val list1 = listOf("topic0", "topic1", "topic2")
        val list2 = listOf("topic3", "topic4", "topic5", "topic6")
        val list3 = listOf("topic2", "topic1", "topic0")

        val repo = TopicNotificationsRepo()

        var mergedTopics = repo.mergeTopics(list1, list2)
        assertEquals(
            setOf(mergedTopics),
            setOf(listOf("topic0", "topic1", "topic2", "topic3", "topic4", "topic5", "topic6")),
        )

        mergedTopics = repo.mergeTopics(list1, list3)
        assertEquals(
            setOf(list1),
            setOf(mergedTopics),
        )

        mergedTopics = repo.mergeTopics(list1, listOf())
        assertEquals(
            setOf(list1),
            setOf(mergedTopics),
        )

        mergedTopics = repo.mergeTopics(list1, null)
        assertEquals(
            setOf(list1),
            setOf(mergedTopics),
        )

        mergedTopics = repo.mergeTopics(listOf(), listOf())
        assertEquals(
            setOf(listOf<String>()),
            setOf(mergedTopics),
        )

        mergedTopics = repo.mergeTopics(listOf(), null)
        assertEquals(
            setOf(listOf<String>()),
            setOf(mergedTopics),
        )
    }
}
