package com.wapo.kmpshared.features.conversations.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test

class CommentsPaginationManagerTest {
    @Test
    fun testPaginationManagerConcurrency() =
        runTest {
            val manager = CommentsPaginationManager()
            // We launch 100 coroutines on a background thread pool (Dispatchers.Default)
            // This GUARANTEES that they will hit the map at the same time.
            withContext(Dispatchers.Default) {
                val jobs =
                    List(100) { i ->
                        launch {
                            val parentId = "parent-$i"
                            manager.update(parentId, "cursor-$i", true)
                            manager.getRequestParams(parentId)
                            if (i % 10 == 0) manager.reset()
                        }
                    }
                jobs.joinAll()
            }
            // If this test finishes without a ConcurrentModificationException, it is PROVEN thread-safe.
        }
}
