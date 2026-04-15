package com.stepquest.model

import kotlinx.serialization.Serializable

@Serializable
data class LocationNode(
    val id: String,
    val name: String,
    val description: String,
    val isStart: Boolean = false,
    val isEnd: Boolean = false,
    val choices: List<Choice>? = null
)

@Serializable
data class Choice(
    val id: String,
    val text: String,
    val nextNodeId: String,
    val stepsRequired: Int = 100
)

@Serializable
data class GameConfig(
    val startNodeId: String,
    val endNodeId: String,
    val nodes: List<LocationNode>,
    val defaultStepsToNextChoice: Int = 100
)

@Serializable
data class PlayerProgress(
    val currentNodeId: String,
    val totalSteps: Int = 0,
    val stepsSinceLastChoice: Int = 0,
    val visitedNodes: List<String> = emptyList(),
    val choiceHistory: List<String> = emptyList()
)
