package com.example.breathingmeditationandroid

import android.util.Log
import java.lang.System.currentTimeMillis
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object Calibrator {

    var calibratedAbdo: Pair<Double, Double> = Pair(0.0, 0.0)
    var calibratedThor: Pair<Double, Double> = Pair(0.0, 0.0)

    // Buffers for how much difference there can be for holding breath
    var holdBreathBufferInAbdo = 0.0
    var holdBreathBufferInThor = 0.0

    //TODO check if this can be calculated from in and out buffer
    var holdBreathBufferMiddleAbdo = 0.0
    var holdBreathBufferMiddleThor = 0.0

    var holdBreathBufferOutAbdo = 0.0
    var holdBreathBufferOutThor = 0.0

    var flowFactorXUp = 0.0
    var flowFactorYUp = 0.0

    var correction = 0.0

    private lateinit var breathingUtils: BreathingUtils
    private lateinit var mService: BluetoothConnection


    fun initialize(mService: BluetoothConnection) {
        this.mService = mService
        breathingUtils = BreathingUtils(mService)
    }

    fun calibrate() {
        calibrateFlow()
    }

    //TODO: muss doch smarter gehen
    //TODO: als Coroutine dann kann sich der screen schön bewegen dazwischen
    //TODO: 0.0 als minima fixen

    private fun calibrateMinAndMax() {

        val minimaAbdo: ArrayList<Double> = ArrayList()
        val maximaAbdo: ArrayList<Double> = ArrayList()

        var lokalMinima = 0.0
        var lokalMaxima = 0.0

        Log.i("calibration", "calibrate min max starting")
        while (mService.mInspiration == 0)
            continue
        Log.i("Calibration:", "Abdo")
        //first abdo
        Log.i("Calibration", "inspiration")

        while (mService.mExpiration == 0) {
            if (!lokalMinima.equals(0.0)) {
                minimaAbdo.add(lokalMinima)
                lokalMinima = 0.0
            }
            if (mService.mAbdoCorrected > lokalMaxima) {
                lokalMaxima = mService.mAbdoCorrected
            }
        }

        Log.i("Calibration", "expiration")
        while (mService.mInspiration == 0) {
            if (!lokalMaxima.equals(0.0)) {
                maximaAbdo.add(lokalMaxima)
                lokalMaxima = 0.0
            }
            if (mService.mAbdoCorrected < lokalMinima) {
                lokalMinima = mService.mAbdoCorrected
            }
        }
        calibratedAbdo =
            Pair(mService.calculateMedian(maximaAbdo) * 1.2, mService.calculateMedian(minimaAbdo) * 1.2)

        val minimaThor: ArrayList<Double> = ArrayList()
        val maximaThor: ArrayList<Double> = ArrayList()

        Log.i("Calibration:", "thor")
        //then thor
        Log.i("Calibration", "inspiration")

        while (mService.mExpiration == 0) {
            if (!lokalMinima.equals(0.0)) {
                minimaThor.add(lokalMinima)
                lokalMinima = 0.0
            }
            if (mService.mThorCorrected > lokalMaxima) {
                lokalMaxima = mService.mThorCorrected

            }
        }

        Log.i("Calibration", "expiration")
        while (mService.mInspiration == 0) {
            if (!lokalMaxima.equals(0.0)) {
                maximaThor.add(lokalMaxima)
                lokalMaxima = 0.0
            }
            if (mService.mThorCorrected < lokalMinima) {
                lokalMinima = mService.mThorCorrected
            }
        }
        calibratedThor =
            Pair(mService.calculateMedian(maximaThor) * 1.2, mService.calculateMedian(minimaThor) * 1.2)
        Log.i("calibration", "max abdo: ${calibratedAbdo.first}")

        Log.i("calibration", "min and max done")
    }

    fun calibrateMinMax() {

        var minAbdo = Double.POSITIVE_INFINITY
        var minThor = Double.POSITIVE_INFINITY

        var maxAbdo = Double.NEGATIVE_INFINITY
        var maxThor = Double.NEGATIVE_INFINITY

        breathingUtils.startFromBeginning()
        repeat(2) {
            while (mService.mExpiration == 0) {
                val mins = determineMin(minAbdo, minThor)
                val maxs = determineMax(maxAbdo, maxThor)

                minAbdo = mins.first
                minThor = mins.second

                maxAbdo = maxs.first
                maxThor = maxs.second
            }
            while (mService.mInspiration == 0) {
                val mins = determineMin(minAbdo, minThor)
                val maxs = determineMax(maxAbdo, maxThor)

                minAbdo = mins.first
                minThor = mins.second

                maxAbdo = maxs.first
                maxThor = maxs.second
            }
        }
        calibratedAbdo = Pair(maxAbdo, minAbdo)
        calibratedThor = Pair(maxThor, minThor)
    }

    private fun determineMin(currMinAbdo: Double, currminThor: Double): Pair<Double, Double> {
        return Pair(min(currMinAbdo, mService.mAbdoCorrected), min(currminThor, mService.mThorCorrected))
    }

    private fun determineMax(currMaxAbdo: Double, currMaxThor: Double): Pair<Double, Double> {
        return Pair(max(currMaxAbdo, mService.mAbdoCorrected), max(currMaxThor, mService.mThorCorrected))

    }

    fun calibrateMinMaxThor() {

    }

    fun calibrateMinMaxAbdo() {

    }


    fun calibrateBreathHold(time: Int, pos: String) {
        val diffValuesThor = arrayListOf<Double>()
        val diffValuesAbdo = arrayListOf<Double>()
        val startTime = currentTimeMillis()
        var prevValueAbdo = breathingUtils.smoothValue().first
        var prevValueThor = breathingUtils.smoothValue().second
        while (currentTimeMillis().minus(startTime) < time) {
            diffValuesAbdo.add(abs(breathingUtils.smoothValue().first.minus(prevValueAbdo)))
            diffValuesThor.add(abs(breathingUtils.smoothValue().second.minus(prevValueThor)))
            prevValueAbdo = breathingUtils.smoothValue().first
            prevValueThor = breathingUtils.smoothValue().second
        }
        when (pos) {
            "in" -> {
                holdBreathBufferInAbdo = max(diffValuesAbdo.average(), holdBreathBufferInAbdo)
                holdBreathBufferInThor = max(diffValuesThor.average(), holdBreathBufferInThor)
            }
            "out" -> {
                holdBreathBufferOutAbdo = max(diffValuesAbdo.average(), holdBreathBufferOutAbdo)
                holdBreathBufferOutThor = max(diffValuesThor.average(), holdBreathBufferOutThor)
            }
            "mid" -> {
                holdBreathBufferMiddleAbdo = max(diffValuesAbdo.average(), holdBreathBufferMiddleAbdo)
                holdBreathBufferMiddleThor = max(diffValuesThor.average(), holdBreathBufferMiddleThor)
            }
        }
        Log.i("Calibration", "BufferInAbdo: $holdBreathBufferInAbdo")
        Log.i("Calibration", "BufferInThor: $holdBreathBufferInThor")
        Log.i("Calibration", "BufferOutAbdo: $holdBreathBufferOutAbdo")
        Log.i("Calibration", "BufferOutThor: $holdBreathBufferOutThor")

        Log.i("calibration", "breath hold calibration done")
    }

    private fun calibrateFlow() {
        Log.i("calibration", "calibrating flow")
        calibrateMinMax()
        calcFlowFactor()
        Log.i("calibration", "finished calibrating flow")
        Log.i("calibration", "min Abdo: ${calibratedAbdo.second} max Abdo: ${calibratedAbdo.first}")
        Log.i("calibration", "min Thor: ${calibratedThor.second} max Thor: ${calibratedThor.first}")

    }

    private fun calcFlowFactor() {
        //TODO make specific to device
        val xStart = 100
        val xEnd = 2000
        val yStart = 800
        val yEnd = 300
        val combinedValueStart: Double =
            (calibratedAbdo.second.plus(calibratedThor.second))
        val combinedValueEnd: Double =
            (calibratedAbdo.first.times(0.6).plus(calibratedThor.first.times(0.6)))
        correction = 0 - combinedValueStart
        flowFactorXUp = (xEnd.minus(xStart)).div(combinedValueEnd.plus(correction))
        flowFactorYUp = (yEnd.minus(yStart)).div(combinedValueEnd.plus(correction))
        Log.i("Calibration", "Flow factor x up: $flowFactorXUp")
        Log.i("Calibration", "Flow factor y up: $flowFactorYUp")

    }
}