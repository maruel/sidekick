package com.fghbuild.sidekick.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fghbuild.sidekick.data.HeartRateData
import com.fghbuild.sidekick.data.RunData
import com.fghbuild.sidekick.repository.RunRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RunDetailViewModel(
    private val runId: Long,
    private val runRepository: RunRepository,
) : ViewModel() {
    private val _runData = MutableStateFlow<RunData?>(null)
    val runData: StateFlow<RunData?> = _runData.asStateFlow()

    private val _heartRateData = MutableStateFlow(HeartRateData())
    val heartRateData: StateFlow<HeartRateData> = _heartRateData.asStateFlow()

    init {
        loadRunData()
    }

    private fun loadRunData() {
        viewModelScope.launch {
            val data = runRepository.getCompleteRunData(runId)
            _runData.value = data

            // Reconstruct HeartRateData from the heartRateHistory
            if (data != null && data.heartRateHistory.isNotEmpty()) {
                val measurements = data.heartRateHistory.map { it.bpm }.toMutableList()
                val avgBpm =
                    if (measurements.isNotEmpty()) {
                        measurements.average().toInt()
                    } else {
                        0
                    }
                _heartRateData.value =
                    HeartRateData(
                        currentBpm = measurements.lastOrNull() ?: 0,
                        measurements = measurements,
                        averageBpm = avgBpm,
                    )
            }
        }
    }
}
