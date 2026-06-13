package com.example.bowlingmaster200.ui.games

import android.app.Application
import com.example.bowlingmaster200.data.repository.BowlingRepository
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GamesViewModelTest {

    private lateinit var fakeDao: FakeBowlingDao
    private lateinit var viewModel: GamesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        fakeDao = FakeBowlingDao()
        val repository = BowlingRepository(fakeDao)
        viewModel = GamesViewModel(mockk<Application>(relaxed = true), repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun resetGame_clearsInputAndRestoresInitialState() {
        viewModel.onKeyInput("9")
        viewModel.onKeyInput("-")
        viewModel.selectCell(frameIndex = 5, rollIndex = 1)

        viewModel.resetGame()

        val state = viewModel.uiState.value
        val initial = GamesUiState.initial()
        assertEquals(initial.frames.map { it.firstRollText }, state.frames.map { it.firstRollText })
        assertEquals(initial.frames.map { it.secondRollText }, state.frames.map { it.secondRollText })
        assertNull(state.totalScore)
        assertEquals(initial.selectedFrameIndex, state.selectedFrameIndex)
        assertEquals(initial.selectedRollIndex, state.selectedRollIndex)
    }

    @Test
    fun isComplete_falseWhenGameIncomplete() {
        enterOpenFrame()

        val state = viewModel.uiState.value
        assertFalse(state.isComplete)
        assertNull(state.totalScore)
    }

    @Test
    fun isComplete_trueWhenGameComplete() {
        enterCompleteOpenGame()

        val state = viewModel.uiState.value
        assertTrue(state.isComplete)
        assertEquals(90, state.totalScore)
    }

    @Test
    fun onKeyInput_updatesCumulativeAndTotalScore() {
        enterOpenFrame()

        val state = viewModel.uiState.value
        assertEquals(9, state.frames[0].cumulativeScore)
        assertNull(state.totalScore)
        assertFalse(state.isComplete)

        repeat(9) { enterOpenFrame() }

        val completeState = viewModel.uiState.value
        assertEquals(90, completeState.frames[9].cumulativeScore)
        assertEquals(90, completeState.totalScore)
        assertTrue(completeState.isComplete)
    }

    @Test
    fun saveGame_savesToRepositoryAndResetsState() {
        enterCompleteOpenGame()

        viewModel.saveGame()

        assertEquals(1, fakeDao.insertGameWithFramesCallCount)
        assertEquals(GamesUiState.initial(), viewModel.uiState.value)
    }

    private fun enterOpenFrame() {
        viewModel.onKeyInput("9")
        viewModel.onKeyInput("-")
    }

    private fun enterCompleteOpenGame() {
        repeat(10) { enterOpenFrame() }
    }
}
