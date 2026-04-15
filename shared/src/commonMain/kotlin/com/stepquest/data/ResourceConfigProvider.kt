package com.stepquest.data

import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath

class ResourceConfigProvider(
    private val json: Json = Json { ignoreUnknownKeys = true }
) : ConfigProvider {
    
    override suspend fun loadConfig(): com.stepquest.model.GameConfig {
        val content = FileSystem.RESOURCES.read("game_config.json".toPath()) {
            readUtf8()
        }
        return json.decodeFromString(content)
    }
}
