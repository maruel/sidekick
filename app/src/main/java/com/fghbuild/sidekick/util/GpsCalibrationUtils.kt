package com.fghbuild.sidekick.util

import com.fghbuild.sidekick.database.GpsMeasurementEntity
import kotlin.math.sqrt

object GpsCalibrationUtils {
    /**
     * Derive Kalman measurement noise from GPS accuracy measurements.
     * Uses variance of accuracy measurements with conservative scaling.
     */
    fun deriveKalmanMeasurementNoise(accuracies: List<Float>): Double {
        if (accuracies.isEmpty()) return 100.0

        val doubleAccuracies = accuracies.map { it.toDouble() }
        val mean = doubleAccuracies.average()
        val variance = doubleAccuracies.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance) * 2.0
    }

    /**
     * Calculate summary statistics from GPS measurements.
     * Returns: (avgAccuracy, p95Accuracy, avgBearingAccuracy)
     */
    fun calculateMeasurementStats(measurements: List<GpsMeasurementEntity>): Triple<Double, Double, Double> {
        if (measurements.isEmpty()) {
            return Triple(0.0, 0.0, 0.0)
        }

        val accuracies = measurements.map { it.accuracy.toDouble() }.sorted()
        val bearingAccuracies = measurements.map { it.bearingAccuracy.toDouble() }

        val avgAccuracy = accuracies.average()
        val p95Accuracy = accuracies[(accuracies.size * 0.95).toInt()]
        val avgBearingAccuracy = bearingAccuracies.average()

        return Triple(avgAccuracy, p95Accuracy, avgBearingAccuracy)
    }

    /**
     * Weighted average of two calibration sets.
     * Used to merge new measurements with existing calibration.
     */
    fun weightedAverage(
        currentAvg: Double,
        currentSampleCount: Int,
        newAvg: Double,
        newSampleCount: Int,
    ): Double {
        if (currentSampleCount == 0) return newAvg
        if (newSampleCount == 0) return currentAvg

        val totalSamples = currentSampleCount + newSampleCount
        val currentWeight = currentSampleCount.toDouble() / totalSamples
        val newWeight = newSampleCount.toDouble() / totalSamples

        return (currentAvg * currentWeight) + (newAvg * newWeight)
    }
}
