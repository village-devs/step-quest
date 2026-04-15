package com.stepquest.android

import android.app.Application
import app.cash.sqldelight.android.driver.AndroidSqliteDriver
import com.stepquest.data.*
import com.stepquest.domain.GameRepository
import com.stepquest.domain.MapService
import com.stepquest.domain.StepCounter
import com.stepquest.presentation.GameViewModel
import com.stepquest.presentation.GameViewModelImpl
import kotlinx.coroutines.runBlocking

class StepQuestApplication : Application() {
    
    lateinit var stepCounter: StepCounter by lazy {
        PlatformStepCounter(applicationContext)
    }
    
    lateinit val databaseDriver: AndroidSqliteDriver by lazy {
        AndroidSqliteDriver(StepQuestDatabase.Schema, applicationContext, "stepquest.db")
    }
    
    lateinit val progressStorage: ProgressStorage by lazy {
        SqlDelightProgressStorage(databaseDriver)
    }
    
    lateinit val configProvider: ConfigProvider by lazy {
        ResourceConfigProvider()
    }
    
    lateinit val gameRepository: GameRepository by lazy {
        GameRepositoryImpl(configProvider, progressStorage)
    }
    
    var mapService: MapService? = null
    
    lateinit val viewModel: GameViewModel by lazy {
        GameViewModelImpl(gameRepository, mapService!!, stepCounter)
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize map service after config is loaded
        Thread {
            val config = runBlocking { configProvider.loadConfig() }
            mapService = MapServiceImpl(config)
        }.start()
    }
}
