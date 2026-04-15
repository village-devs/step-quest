package com.stepquest.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.stepquest.domain.StepCounter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

actual class PlatformStepCounter(
    private val context: Context
) : StepCounter {
    
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    
    private var currentSteps = 0
    private val mutex = Mutex()
    private var isListening = false
    
    actual override fun startListening(): Flow<Int> = callbackFlow {
        if (!isListening && stepSensor != null) {
            isListening = true
            
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    when (event.sensor.type) {
                        Sensor.TYPE_STEP_COUNTER -> {
                            currentSteps = event.values[0].toInt()
                            trySend(currentSteps)
                        }
                        Sensor.TYPE_STEP_DETECTOR -> {
                            currentSteps++
                            trySend(currentSteps)
                        }
                    }
                }
                
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            
            sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_UI)
            stepDetector?.let { 
                sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) 
            }
            
            // Get initial steps
            stepSensor?.let {
                sensorManager.registerListener(object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        currentSteps = event.values[0].toInt()
                        trySend(currentSteps)
                        sensorManager.unregisterListener(this)
                    }
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }, it, SensorManager.SENSOR_DELAY_UI)
            }
        } else if (stepSensor == null) {
            // Fallback to manual increment for testing
            var mockSteps = currentSteps
            mockSteps++
            currentSteps = mockSteps
            trySend(currentSteps)
        }
        
        awaitClose {
            stopListening()
        }
    }
    
    actual override fun stopListening() {
        if (isListening) {
            sensorManager.unregisterListener(null)
            isListening = false
        }
    }
    
    actual override suspend fun getCurrentSteps(): Int = mutex.withLock {
        currentSteps
    }
}
