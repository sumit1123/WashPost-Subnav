package com.wapo.flagship.data.repository

import com.wapo.flagship.data.repository.TestData.Tile.TILE_ONE
import com.wapo.flagship.data.repository.TestData.Tile.TILE_TWO
import com.wapo.flagship.data.repository.TestData.Tile.fakeTestData
import com.wapo.flagship.features.grid.domain.model.SectionHabitTile
import com.wapo.flagship.features.grid.domain.repository.SectionHabitTilesRepository
import com.washingtonpost.foryou.data.HabitTilesResponse
import com.washingtonpost.foryou.data.Tile
import com.washingtonpost.foryou.domain.HabitTilesRepository
import com.washingtonpost.foryou.network.APIResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SectionHabitTilesRepositoryTest {

    private lateinit var sectionHabitTilesRepository: SectionHabitTilesRepository
    private lateinit var habitTilesRepository: HabitTilesRepository

    @Before
    fun setup() {
        habitTilesRepository = mockk<HabitTilesRepository>(relaxed = true)
        sectionHabitTilesRepository = SectionHabitTilesRepositoryImpl(habitTilesRepository)
    }

    @Test
    fun `Given a successful response, when getHabitTilesFeed is called, then a SectionHabitTilesData is returned`() = runTest {
        coEvery { habitTilesRepository.getHabitTilesFeed(any()) } returns APIResult.Success(fakeTestData)

        val result = sectionHabitTilesRepository.getHabitTilesFeed(false)

        assertEquals(fakeTestData.tiles!!.size, result!!.tiles!!.size)
        compareSectionHabitTileVSTile(result.tiles!![0], TILE_ONE)
        compareSectionHabitTileVSTile(result.tiles!![1], TILE_TWO)
    }

    @Test
    fun `Given an error response, when getHabitTilesFeed is called, then null is returned`() = runTest {
        coEvery { habitTilesRepository.getHabitTilesFeed(any()) } returns APIResult.Failure(0, null)

        val result = sectionHabitTilesRepository.getHabitTilesFeed(false)

        assertNull(result)
    }

    @Test
    fun `When canRequestPersonalizedData is called, then the correct value is returned`() = runTest {
        every { habitTilesRepository.canRequestPersonalizedData() } returns true

        val result = sectionHabitTilesRepository.canRequestPersonalizedData()

        assertTrue(result)
    }

    private fun compareSectionHabitTileVSTile(sectionHabitTile: SectionHabitTile, tile: Tile) {
        assertEquals(sectionHabitTile.score, tile.score)
        assertEquals(sectionHabitTile.tileCategory, tile.tileCategory)
        assertEquals(sectionHabitTile.imageUrl, tile.imageUrl)
        assertEquals(sectionHabitTile.contextLabel, tile.contextLabel)
        assertEquals(sectionHabitTile.contextIndicator, tile.contextIndicator)
        assertEquals(sectionHabitTile.tileLabel, tile.tileLabel)
        assertEquals(sectionHabitTile.tileLink, tile.tileLink)
        assertEquals(sectionHabitTile.tileLabelBehavior, tile.tileLabelBehavior)
        assertEquals(sectionHabitTile.tileCategoryDetail, tile.tileCategoryDetail)
        assertEquals(sectionHabitTile.position, tile.position)
        assertEquals(sectionHabitTile.persoPodcastMetadata, tile.persoPodcastMetadata)
    }
}
