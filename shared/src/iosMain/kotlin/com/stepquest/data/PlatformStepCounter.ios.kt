package com.stepquest.data

import com.stepquest.domain.StepCounter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.CoreMotion.CMPedometer
import platform.Foundation.NSDate
import platform.Foundation.NSUserDefaults

actual class PlatformStepCounter : StepCounter {
    
    private val pedometer = CMPedometer()
    private var currentSteps: Int = 0
    private val mutex = Mutex()
    private var isListening = false
    
    actual override fun startListening(): Flow<Int> = callbackFlow {
        if (!isListening) {
            isListening = true
            
            // Load saved steps from UserDefaults
            val savedSteps = NSUserDefaults.standardUserDefaults.integerForKey("currentSteps")
            currentSteps = savedSteps.toInt()
            trySend(currentSteps)
            
            // Start pedometer updates
            pedometer.startUpdatesFromDate(NSDate()) { data, error ->
                if (error == null && data != null) {
                    val steps = data.numberOfSteps.intValue
                    currentSteps = steps
                    
                    // Save to UserDefaults for persistence
                    NSUserDefaults.standardUserDefaults.setInteger(steps.toLong(), "currentSteps")
                    NSUserDefaults.standardUserDefaults.synchronize()
                    
                    trySend(steps)
                }
            }
        }
        
        awaitClose {
            stopListening()
        }
    }
    
    actual override fun stopListening() {
        if (isListening) {
            pedometer.stopUpdates()
            isListening = false
        }
    }
    
    actual override suspend fun getCurrentSteps(): Int = mutex.withLock {
        currentSteps
    }
}
