package com.stepquest.data

import com.stepquest.domain.MapService
import com.stepquest.model.Choice
import com.stepquest.model.GameConfig
import com.stepquest.model.LocationNode

class MapServiceImpl(
    private val config: GameConfig
) : MapService {
    
    private val nodesMap = config.nodes.associateBy { it.id }
    
    override suspend fun getNode(nodeId: String): LocationNode? {
        return nodesMap[nodeId]
    }
    
    override suspend fun getNextNodes(currentNodeId: String, choiceId: String): List<LocationNode> {
        val currentNode = nodesMap[currentNodeId] ?: return emptyList()
        val choice = currentNode.choices?.find { it.id == choiceId } ?: return emptyList()
        val nextNode = nodesMap[choice.nextNodeId] ?: return emptyList()
        return listOf(nextNode)
    }
    
    override fun calculateStepsToNextChoice(currentSteps: Int, choice: Choice): Int {
        return (choice.stepsRequired - currentSteps).coerceAtLeast(0)
    }
}
