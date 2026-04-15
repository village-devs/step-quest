package com.stepquest.presentation

import com.stepquest.data.GameRepositoryImpl
import com.stepquest.data.MapServiceImpl
import com.stepquest.domain.*
import com.stepquest.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModelImpl(
    private val gameRepository: GameRepository,
    private val mapService: MapService,
    private val stepCounter: StepCounter,
    private val appScope: CoroutineScope = CoroutineScope(SupervisorJob())
) : GameViewModel {
    
    private val _state = MutableStateFlow(GameState())
    override val state: StateFlow<GameState> = _state.asStateFlow()
    
    private var gameConfig: GameConfig? = null
    private var currentProgress: PlayerProgress? = null
    
    init {
        appScope.launch {
            initializeGame()
        }
    }
    
    private suspend fun initializeGame() {
        _state.value = _state.value.copy(isLoading = true)
        
        try {
            gameConfig = gameRepository.loadConfig()
            mapService = MapServiceImpl(gameConfig!!)
            
            val savedProgress = gameRepository.getProgress()
            
            if (savedProgress != null) {
                currentProgress = savedProgress
                val node = mapService.getNode(savedProgress.currentNodeId)
                
                _state.value = _state.value.copy(
                    isLoading = false,
                    currentNode = node,
                    totalSteps = savedProgress.totalSteps,
                    stepsToNextChoice = calculateStepsToNextChoice(node),
                    availableChoices = node?.choices ?: emptyList(),
                    visitedNodes = savedProgress.visitedNodes,
                    choiceHistory = savedProgress.choiceHistory,
                    isAtEnd = node?.isEnd == true
                )
                
                if (!node.isEnd && node.choices.isNullOrEmpty()) {
                    _state.value = _state.value.copy(showChoiceModal = true)
                }
            } else {
                startNewGame()
            }
            
            startStepListening()
        } catch (e: Exception) {
            _state.value = _state.value.copy(isLoading = false)
        }
    }
    
    private suspend fun startNewGame() {
        val config = gameConfig ?: return
        val startNode = mapService.getNode(config.startNodeId)
        
        currentProgress = PlayerProgress(
            currentNodeId = config.startNodeId,
            totalSteps = 0,
            stepsSinceLastChoice = 0,
            visitedNodes = listOf(config.startNodeId),
            choiceHistory = emptyList()
        )
        
        gameRepository.saveProgress(currentProgress!!)
        
        _state.value = _state.value.copy(
            isLoading = false,
            currentNode = startNode,
            totalSteps = 0,
            stepsToNextChoice = startNode?.choices?.firstOrNull()?.stepsRequired ?: config.defaultStepsToNextChoice,
            availableChoices = startNode?.choices ?: emptyList(),
            visitedNodes = listOf(config.startNodeId),
            choiceHistory = emptyList(),
            isAtEnd = startNode?.isEnd == true
        )
    }
    
    private fun startStepListening() {
        appScope.launch {
            stepCounter.startListening().collect { steps ->
                onEvent(GameEvent.StepsUpdated(steps))
            }
        }
    }
    
    private fun calculateStepsToNextChoice(node: LocationNode?): Int {
        if (node == null || node.choices.isNullOrEmpty()) return 0
        
        val progress = currentProgress ?: return node.choices.first().stepsRequired
        
        val currentChoice = node.choices.firstOrNull { 
            !progress.choiceHistory.contains(it.id) 
        } ?: return 0
        
        val stepsForThisChoice = currentProgress.stepsSinceLastChoice
        return (currentChoice.stepsRequired - stepsForThisChoice).coerceAtLeast(0)
    }
    
    override fun start() {
        appScope.launch {
            initializeGame()
        }
    }
    
    override fun onEvent(event: GameEvent) {
        when (event) {
            is GameEvent.StepsUpdated -> handleStepsUpdate(event.steps)
            is GameEvent.ChoiceMade -> makeChoice(event.choiceId)
            is GameEvent.GameStarted -> start()
            is GameEvent.GameReset -> resetGame()
        }
    }
    
    private fun handleStepsUpdate(newTotalSteps: Int) {
        val currentState = _state.value
        val oldTotalSteps = currentState.totalSteps
        
        if (newTotalSteps <= oldTotalSteps) return
        
        val stepsGained = newTotalSteps - oldTotalSteps
        val newStepsSinceLastChoice = (currentState.stepsToNextChoice - stepsGained).coerceAtLeast(0)
            .let { remaining -> 
                val node = currentState.currentNode
                if (node?.choices.isNullOrEmpty()) return@let 0
                
                val currentChoice = node.choices.firstOrNull { 
                    !(currentProgress?.choiceHistory?.contains(it.id) == true) 
                } ?: return@let 0
                
                currentChoice.stepsRequired - remaining
            }
        
        val updatedProgress = currentProgress?.copy(
            totalSteps = newTotalSteps,
            stepsSinceLastChoice = newStepsSinceLastChoice
        )
        
        currentProgress = updatedProgress
        updatedProgress?.let { appScope.launch { gameRepository.saveProgress(it) } }
        
        val shouldShowChoice = newStepsSinceLastChoice >= (currentState.currentNode?.choices?.firstOrNull { 
            !(currentProgress?.choiceHistory?.contains(it.id) == true) 
        }?.stepsRequired ?: Int.MAX_VALUE)
        
        _state.value = currentState.copy(
            totalSteps = newTotalSteps,
            stepsToNextChoice = (currentState.stepsToNextChoice - stepsGained).coerceAtLeast(0),
            showChoiceModal = shouldShowChoice && !currentState.isAtEnd
        )
    }
    
    override fun makeChoice(choiceId: String) {
        appScope.launch {
            val currentState = _state.value
            val currentNode = currentState.currentNode ?: return@launch
            
            val choice = currentNode.choices?.find { it.id == choiceId } ?: return@launch
            val nextNodes = mapService.getNextNodes(currentNode.id, choiceId)
            val nextNode = nextNodes.firstOrNull() ?: return@launch
            
            val updatedVisitedNodes = currentProgress?.visitedNodes?.plus(nextNode.id) ?: listOf(nextNode.id)
            val updatedChoiceHistory = currentProgress?.choiceHistory?.plus(choiceId) ?: listOf(choiceId)
            
            currentProgress = currentProgress?.copy(
                currentNodeId = nextNode.id,
                stepsSinceLastChoice = 0,
                visitedNodes = updatedVisitedNodes,
                choiceHistory = updatedChoiceHistory
            )
            
            currentProgress?.let { gameRepository.saveProgress(it) }
            
            _state.value = _state.value.copy(
                currentNode = nextNode,
                stepsToNextChoice = calculateStepsToNextChoice(nextNode),
                availableChoices = nextNode.choices ?: emptyList(),
                visitedNodes = updatedVisitedNodes,
                choiceHistory = updatedChoiceHistory,
                showChoiceModal = false,
                isAtEnd = nextNode.isEnd
            )
        }
    }
    
    override fun dismissChoiceModal() {
        _state.value = _state.value.copy(showChoiceModal = false)
    }
    
    override fun resetGame() {
        appScope.launch {
            gameRepository.resetProgress()
            currentProgress = null
            startNewGame()
        }
    }
}
