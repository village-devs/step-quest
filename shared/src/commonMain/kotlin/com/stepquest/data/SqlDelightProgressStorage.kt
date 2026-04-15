package com.stepquest.data

import app.cash.sqldelight.db.SqlDriver
import com.stepquest.model.PlayerProgress
import kotlinx.serialization.json.Json

class SqlDelightProgressStorage(
    private val driver: SqlDriver,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : ProgressStorage {
    
    override suspend fun save(progress: PlayerProgress) {
        driver.execute(null, """
            INSERT OR REPLACE INTO player_progress (id, current_node_id, total_steps, steps_since_last_choice, visited_nodes, choice_history)
            VALUES (1, ?, ?, ?, ?, ?)
        """.trimIndent(), 5) {
            bindString(1, progress.currentNodeId)
            bindLong(2, progress.totalSteps.toLong())
            bindLong(3, progress.stepsSinceLastChoice.toLong())
            bindString(4, json.encodeToString(progress.visitedNodes))
            bindString(5, json.encodeToString(progress.choiceHistory))
        }
    }
    
    override suspend fun get(): PlayerProgress? {
        return driver.executeQuery(null, "SELECT * FROM player_progress WHERE id = 1", mapper = { cursor ->
            PlayerProgress(
                currentNodeId = cursor.getString(1)!!,
                totalSteps = cursor.getLong(2)?.toInt() ?: 0,
                stepsSinceLastChoice = cursor.getLong(3)?.toInt() ?: 0,
                visitedNodes = json.decodeFromString(cursor.getString(4) ?: "[]"),
                choiceHistory = json.decodeFromString(cursor.getString(5) ?: "[]")
            )
        }, factor = 1).executeAsOneOrNull()
    }
    
    override suspend fun clear() {
        driver.execute(null, "DELETE FROM player_progress", 0)
    }
}
