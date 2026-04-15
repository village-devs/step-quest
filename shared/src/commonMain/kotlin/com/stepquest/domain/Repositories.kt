package com.stepquest.domain

interface StepCounter {
    fun startListening(): kotlinx.coroutines.flow.Flow<Int>
    fun stopListening()
    suspend fun getCurrentSteps(): Int
}

interface GameRepository {
    suspend fun loadConfig(): com.stepquest.model.GameConfig
    suspend fun saveProgress(progress: com.stepquest.model.PlayerProgress)
    suspend fun getProgress(): com.stepquest.model.PlayerProgress?
    suspend fun resetProgress()
}

interface MapService {
    suspend fun getNode(nodeId: String): com.stepquest.model.LocationNode?
    suspend fun getNextNodes(currentNodeId: String, choiceId: String): List<com.stepquest.model.LocationNode>
    fun calculateStepsToNextChoice(currentSteps: Int, choice: com.stepquest.model.Choice): Int
}
