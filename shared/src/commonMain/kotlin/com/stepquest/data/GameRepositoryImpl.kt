package com.stepquest.data

import com.stepquest.domain.GameRepository
import com.stepquest.model.GameConfig
import com.stepquest.model.PlayerProgress
import kotlinx.serialization.json.Json

class GameRepositoryImpl(
    private val configProvider: ConfigProvider,
    private val progressStorage: ProgressStorage
) : GameRepository {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    override suspend fun loadConfig(): GameConfig {
        return configProvider.loadConfig()
    }
    
    override suspend fun saveProgress(progress: PlayerProgress) {
        progressStorage.save(progress)
    }
    
    override suspend fun getProgress(): PlayerProgress? {
        return progressStorage.get()
    }
    
    override suspend fun resetProgress() {
        progressStorage.clear()
    }
}

interface ConfigProvider {
    suspend fun loadConfig(): GameConfig
}

interface ProgressStorage {
    suspend fun save(progress: PlayerProgress)
    suspend fun get(): PlayerProgress?
    suspend fun clear()
}
