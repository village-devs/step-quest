expect class PlatformStepCounter : StepCounter {
    override fun startListening(): kotlinx.coroutines.flow.Flow<Int>
    override fun stopListening()
    override suspend fun getCurrentSteps(): Int
}
