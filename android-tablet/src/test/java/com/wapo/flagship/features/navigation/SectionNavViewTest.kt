package com.wapo.flagship.features.navigation

import com.wapo.android.commons.domain.AppContextUtilsRepo
import com.wapo.flagship.IntentHelper
import com.wapo.flagship.data.CacheManager
import com.wapo.flagship.domain.repository.FindRepository
import com.wapo.flagship.domain.repository.MenuSectionRepo
import com.wapo.flagship.features.BaseViewModelTest
import com.wapo.flagship.features.find.model.HighlightBoxType
import com.wapo.flagship.json.MenuSection
import com.wapo.flagship.navigation.ui.TopBarState
import com.wapo.flagship.navigation.viewmodel.sectionnav.SectionNavEvent
import com.wapo.flagship.navigation.viewmodel.sectionnav.SectionNavUiState
import com.wapo.flagship.navigation.viewmodel.sectionnav.SectionNavViewModel
import com.wapo.flagship.navigation.viewmodel.sectionnav.SectionNavViewModel.Companion.POLITICS_PATH
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.util.tracking.states.NavigationBehavior
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SectionNavViewTest : BaseViewModelTest<SectionNavUiState, SectionNavEvent?>() {

    private lateinit var menuSectionOne: MenuSection
    private lateinit var menuSectionTwo: MenuSection
    private lateinit var listMenuSections: List<MenuSection>
    private lateinit var menuSectionThree: MenuSection
    private lateinit var findRepository: FindRepository
    private lateinit var intentHelper: IntentHelper
    private lateinit var menuSectionRepo: MenuSectionRepo
    private lateinit var appContextUtilsRepo: AppContextUtilsRepo
    private lateinit var cacheManager: CacheManager

    private lateinit var viewModel: SectionNavViewModel

    @Before
    fun setup() {
        menuSectionTwo = mockk<MenuSection>(relaxed = true)
        menuSectionThree = mockk<MenuSection>(relaxed = true)

        menuSectionOne = mockk<MenuSection>(relaxed = true)
        every { menuSectionOne.databaseId } returns FAKE_DATABASE_ID_ONE
        every { menuSectionOne.title } returns SECTION_TITLE_TEST
        every { menuSectionOne.sectionInfo } returns arrayOf(
            menuSectionTwo,
            menuSectionThree
        )

        findRepository = mockk<FindRepository>(relaxed = true)
        every { findRepository.getMenuSection(FAKE_DATABASE_ID_ONE) } returns menuSectionOne
        every { findRepository.getMenuSection(FAKE_DATABASE_ID_TWO) } returns menuSectionTwo
        every { findRepository.getMenuSection(HighlightBoxType.COMICS.link) } returns menuSectionTwo
        every { findRepository.getMenuSection(POLITICS_PATH) } returns menuSectionTwo

        intentHelper = mockk<IntentHelper>(relaxed = true)

        listMenuSections = listOf(
            menuSectionOne,
            menuSectionTwo
        )
        menuSectionRepo = mockk<MenuSectionRepo>(relaxed = true)
        every { menuSectionRepo.getMenuSections() } returns listMenuSections
        every {
            menuSectionRepo.getMenuSectionFromUrl(
                FAKE_ONE_SECTION_URL,
                intentHelper,
                listMenuSections
            )
        } returns menuSectionOne
        every {
            menuSectionRepo.getMenuSectionFromUrl(
                FAKE_NULL_SECTION_URL,
                intentHelper,
                listMenuSections
            )
        } returns null

        appContextUtilsRepo = mockk<AppContextUtilsRepo>(relaxed = true)

        cacheManager = mockk<CacheManager>(relaxed = true)

        mockkStatic(Measurement::class)
        every { Measurement.trackComicsSection(any()) } just Runs
        every { Measurement.trackHoroscopesSection(any()) } just Runs

        viewModel = SectionNavViewModel(
            findRepository,
            intentHelper,
            menuSectionRepo,
            appContextUtilsRepo,
            cacheManager
        )
    }

    override fun collectUIStates(): StateFlow<SectionNavUiState> {
        return viewModel.uiState
    }

    override fun collectEvents(): SharedFlow<SectionNavEvent?> {
        return viewModel.sectionNavEvent
    }

    @Test
    fun sectionNavViewModel_getSectionTitle_Init_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                advanceUntilIdle()

                assertEquals(EMPTY_STRING, viewModel.getSectionTitle())
            }
        }

    @Test
    fun sectionNavViewModel_getSectionTitle_setSectionTitle_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                viewModel.setSectionTitle(SECTION_TITLE_TEST)
                advanceUntilIdle()

                assertEquals(SECTION_TITLE_TEST, viewModel.getSectionTitle())
            }
        }

    @Test
    fun sectionNavViewModel_setSectionTitle_With_Title_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                viewModel.setSectionTitle(SECTION_TITLE_TEST)
                advanceUntilIdle()

                assertEquals(SECTION_TITLE_TEST, uiStates.last().sectionTitle)
            }
        }

    @Test
    fun sectionNavViewModel_setTopBarState_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                viewModel.setTopBarState(TopBarState.TITLE_COLLAPSED)
                advanceUntilIdle()

                assertEquals(TopBarState.TITLE_COLLAPSED, uiStates.last().topBarState)
            }
        }

    @Test
    fun sectionNavViewModel_reloadTitle_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                viewModel.reloadTitle()
                advanceUntilIdle()

                assert(events.contains(SectionNavEvent.ReloadTitle))
            }
        }

    @Test
    fun sectionNavViewModel_setSectionTitle_section_with_info_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                viewModel.openSection(FAKE_DATABASE_ID_ONE, null)
                viewModel.setSectionTitle()
                advanceUntilIdle()

                assertEquals(SECTION_TITLE_TEST, uiStates.last().sectionTitle)
            }
        }

    @Test
    fun sectionNavViewModel_setSectionTitle_section_with_no_info_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                viewModel.openSection(FAKE_DATABASE_ID_TWO, null)
                viewModel.setSectionTitle()
                advanceUntilIdle()

                assertEquals(null, uiStates.last().sectionTitle)
            }
        }

    @Test
    fun sectionNavViewModel_openSection_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                viewModel.openSection(FAKE_DATABASE_ID_ONE, null)
                advanceUntilIdle()

                assert(events.contains(SectionNavEvent.OpenSection(menuSection = menuSectionOne)))
                assertEquals(TopBarState.EXPANDED, uiStates.last().topBarState)
            }
        }

    @Test
    fun sectionNavViewModel_openSection_with_nav_value_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                viewModel.openSection(FAKE_DATABASE_ID_ONE, NavigationBehavior.SETTINGS)
                advanceUntilIdle()

                assert(
                    events.contains(
                        SectionNavEvent.OpenSection(
                            menuSection = menuSectionOne,
                            navType = NavigationBehavior.SETTINGS.value
                        )
                    )
                )
                assertEquals(TopBarState.EXPANDED, uiStates.last().topBarState)
            }
        }

    @Test
    fun sectionNavViewModel_resetOpenSection_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                viewModel.openSection(FAKE_DATABASE_ID_ONE, NavigationBehavior.SETTINGS)
                viewModel.setSectionTitle()
                advanceUntilIdle()

                assertEquals(SECTION_TITLE_TEST, uiStates.last().sectionTitle)

                viewModel.resetOpenSection()
                viewModel.setSectionTitle()
                advanceUntilIdle()

                assertEquals(null, uiStates.last().sectionTitle)
            }
        }

    @Test
    fun sectionNavViewModel_openSectionByUrl_with_menu_section_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                viewModel.openSectionByUrl(FAKE_ONE_SECTION_URL)
                advanceUntilIdle()

                assert(
                    events.contains(
                        SectionNavEvent.OpenSection(
                            menuSection = menuSectionOne
                        )
                    )
                )
                assertEquals(TopBarState.EXPANDED, uiStates.last().topBarState)
            }
        }

    @Test
    fun sectionNavViewModel_openSectionByUrl_no_menu_section_no_debuggable_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                every { appContextUtilsRepo.isDebuggableBuild() } returns false

                viewModel.openSectionByUrl(FAKE_NULL_SECTION_URL)
                advanceUntilIdle()

                assert(
                    events.contains(
                        SectionNavEvent.OpenInWebView(
                            url = FAKE_NULL_SECTION_URL
                        )
                    )
                )
            }
        }

    @Test
    fun sectionNavViewModel_openSectionByUrl_no_menu_section_debuggable_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                every { appContextUtilsRepo.isDebuggableBuild() } returns true

                viewModel.openSectionByUrl(FAKE_NULL_SECTION_URL)
                advanceUntilIdle()

                assert(
                    events.contains(
                        SectionNavEvent.OpenInWebView(
                            url = FAKE_NULL_SECTION_URL
                        )
                    )
                )
            }
        }

    @Test
    fun sectionNavViewModel_openComics_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                viewModel.openComics()
                advanceUntilIdle()

                assert(
                    events.contains(
                        SectionNavEvent.OpenSection(
                            menuSection = menuSectionTwo,
                            navType = NavigationBehavior.FIND_TAB_HIGHLIGHT.value
                        )
                    )
                )
                assertEquals(TopBarState.EXPANDED, uiStates.last().topBarState)
            }
        }

    @Test
    fun sectionNavViewModel_openPrint_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                viewModel.openPrint()
                advanceUntilIdle()

                assert(
                    events.contains(
                        SectionNavEvent.OpenPrint
                    )
                )
            }
        }

    @Test
    fun sectionNavViewModel_openPoliticsFromShortcut_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                viewModel.openPoliticsFromShortcut()
                advanceUntilIdle()

                assert(
                    events.contains(
                        SectionNavEvent.OpenSection(
                            menuSection = menuSectionTwo
                        )
                    )
                )
                assertEquals(TopBarState.EXPANDED, uiStates.last().topBarState)
            }
        }

    @Test
    fun sectionNavViewModel_openHoroscope_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                viewModel.openHoroscope()
                advanceUntilIdle()

                assert(
                    events.contains(
                        SectionNavEvent.OpenInWebView(
                            url = HighlightBoxType.HOROSCOPES.link
                        )
                    )
                )
            }
        }

    @Test
    fun sectionNavViewModel_openRipple_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                viewModel.openRipple()
                advanceUntilIdle()

                assert(
                    events.contains(
                        SectionNavEvent.OpenInWebView(
                            url = HighlightBoxType.RIPPLE.link
                        )
                    )
                )
            }
        }

    @Test
    fun sectionNavViewModel_determineTopBarState_MINUS_TOGGLE_THRESHOLD_PIXELS_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                viewModel.determineTopBarState(-21f, true)
                advanceUntilIdle()

                assertEquals(TopBarState.COLLAPSED, uiStates.last().topBarState)
            }
        }

    @Test
    fun sectionNavViewModel_determineTopBarState_MORE_TOGGLE_THRESHOLD_PIXELS_TITLE_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                viewModel.determineTopBarState(21f, true)
                advanceUntilIdle()

                assertEquals(TopBarState.EXPANDED, uiStates.last().topBarState)
            }
        }

    @Test
    fun sectionNavViewModel_determineTopBarState_MORE_TOGGLE_THRESHOLD_PIXELS_NO_TITLE_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                viewModel.determineTopBarState(21f, false)
                advanceUntilIdle()

                assertEquals(TopBarState.EXPANDED, uiStates.last().topBarState)
            }
        }

    @Test
    fun sectionNavViewModel_determineTopBarState_0_Y_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                viewModel.determineTopBarState(0f, false)
                advanceUntilIdle()

                assertEquals(TopBarState.EXPANDED, uiStates.last().topBarState)
            }
        }

    @Test
    fun sectionNavViewModel_finishSectionOnBack_Test() =
        runTest(coroutinesTestRule.dispatcher) {
            viewModelTest_runTest {
                advanceUntilIdle()
                var result = viewModel.getFinishSectionOnBack()

                assertEquals(false, result)

                viewModel.shouldFinishSectionOnBack(true)
                result = viewModel.getFinishSectionOnBack()
                assertEquals(true, result)
            }
        }

    @After
    fun tearDown() {
        unmockkStatic(Measurement::class)
    }

    companion object {
        const val SECTION_TITLE_TEST = "Section Title Test"
        const val EMPTY_STRING = ""
        const val FAKE_DATABASE_ID_ONE = "1234"
        const val FAKE_DATABASE_ID_TWO = "4321"
        const val FAKE_ONE_SECTION_URL = "one_section.url"
        const val FAKE_NULL_SECTION_URL = "null_section.url"
    }
}
