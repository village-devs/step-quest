package com.stepquest.domain

import com.stepquest.model.*
import kotlinx.coroutines.flow.StateFlow

sealed class GameEvent {
    data class StepsUpdated(val steps: Int) : GameEvent()
    data class ChoiceMade(val choiceId: String) : GameEvent()
    object GameStarted : GameEvent()
    object GameReset : GameEvent()
}

data class GameState(
    val currentNode: LocationNode? = null,
    val totalSteps: Int = 0,
    val stepsToNextChoice: Int = 0,
    val currentChoice: Choice? = null,
    val availableChoices: List<Choice> = emptyList(),
    val isLoading: Boolean = true,
    val isAtEnd: Boolean = false,
    val showChoiceModal: Boolean = false,
    val visitedNodes: List<String> = emptyList(),
    val choiceHistory: List<String> = emptyList()
)

interface GameViewModel {
    val state: StateFlow<GameState>
    
    fun start()
    fun onEvent(event: GameEvent)
    fun makeChoice(choiceId: String)
    fun dismissChoiceModal()
    fun resetGame()
}
