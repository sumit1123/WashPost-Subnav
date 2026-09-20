package com.wapo.flagship.features

import com.wapo.flagship.util.CoroutinesTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
abstract class BaseViewModelTest<U, E> {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    val uiStates = mutableListOf<U>()
    val events = mutableListOf<E?>()

    abstract fun collectUIStates(): StateFlow<U>

    abstract fun collectEvents(): SharedFlow<E>

    suspend fun viewModelTest_runTest(block: () -> Unit) {
        coroutineScope {
            val jobUiStates = launch {
                collectUIStates().collect {
                    uiStates.add(it)
                }
            }

            val jobEvents = launch {
                collectEvents().collect {
                    events.add(it)
                }
            }

            block()
            println("BaseViewModelTest -> uiStates ?=> $uiStates")
            println("BaseViewModelTest -> events ?=> $events")

            jobUiStates.cancel()
            jobEvents.cancel()
        }
    }
}