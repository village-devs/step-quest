package com.stepquest.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.stepquest.domain.GameEvent
import com.stepquest.domain.GameViewModel
import com.stepquest.model.Choice
import com.stepquest.model.LocationNode

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "StepQuest",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A4A4A)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Current location card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    state.currentNode?.let { node ->
                        Text(
                            text = node.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = node.description,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                    
                    if (state.isAtEnd) {
                        Text(
                            text = "🎉 Путь завершён!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Virtual map
            VirtualMap(
                currentNode = state.currentNode,
                visitedNodes = state.visitedNodes,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Steps indicator
            StepsIndicator(
                totalSteps = state.totalSteps,
                stepsToNextChoice = state.stepsToNextChoice,
                currentChoices = state.availableChoices
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Reset button
            if (!state.isLoading) {
                Button(
                    onClick = { viewModel.resetGame() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Начать заново")
                }
            }
        }
        
        // Choice modal
        if (state.showChoiceModal && !state.isAtEnd) {
            ChoiceModal(
                choices = state.availableChoices,
                onChoiceSelected = { choiceId ->
                    viewModel.makeChoice(choiceId)
                },
                onDismiss = { viewModel.dismissChoiceModal() }
            )
        }
        
        // Loading indicator
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

@Composable
fun VirtualMap(
    currentNode: LocationNode?,
    visitedNodes: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                
                // Draw path nodes
                val nodes = listOf(
                    Offset(centerX - 100f, centerY + 80f),
                    Offset(centerX - 50f, centerY + 20f),
                    Offset(centerX + 50f, centerY + 20f),
                    Offset(centerX, centerY - 60f),
                    Offset(centerX, centerY - 140f)
                )
                
                // Draw connections
                nodes.forEachIndexed { index, offset ->
                    if (index < nodes.size - 1) {
                        drawLine(
                            color = Color(0xFF90CAF9),
                            start = offset,
                            end = nodes[index + 1],
                            strokeWidth = 4f,
                            cap = StrokeCap.Round
                        )
                    }
                }
                
                // Draw nodes
                nodes.forEachIndexed { index, offset ->
                    val isVisited = index < visitedNodes.size
                    val isCurrent = index == visitedNodes.size - 1
                    
                    drawCircle(
                        color = when {
                            isCurrent -> Color(0xFF2196F3)
                            isVisited -> Color(0xFF64B5F6)
                            else -> Color(0xFFBBDEFB)
                        },
                        radius = 20f,
                        center = offset
                    )
                    
                    if (isCurrent) {
                        drawCircle(
                            color = Color.White,
                            radius = 10f,
                            center = offset
                        )
                    }
                }
            }
            
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Виртуальная карта",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun StepsIndicator(
    totalSteps: Int,
    stepsToNextChoice: Int,
    currentChoices: List<Choice>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Всего шагов",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = totalSteps.toString(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Circular progress
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(80.dp)) {
                        drawArc(
                            color = Color(0xFFFFCC80),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 8f)
                        )
                        
                        val nextChoice = currentChoices.firstOrNull()
                        val stepsRequired = nextChoice?.stepsRequired ?: 100
                        val progress = if (stepsRequired > 0) {
                            ((stepsRequired - stepsToNextChoice).toFloat() / stepsRequired).coerceIn(0f, 1f)
                        } else 0f
                        
                        drawArc(
                            color = Color(0xFFFF9800),
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = Stroke(width = 8f, cap = StrokeCap.Round)
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stepsToNextChoice.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "до выбора",
                            fontSize = 8.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
            
            if (currentChoices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Следующий выбор: ${currentChoices.firstOrNull()?.text ?: "..."}`,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun ChoiceModal(
    choices: List<Choice>,
    onChoiceSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Развилка пути!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A4A4A)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Выберите направление:",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                choices.forEach { choice ->
                    Button(
                        onClick = { onChoiceSelected(choice.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = choice.text,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${choice.stepsRequired} шагов",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}
