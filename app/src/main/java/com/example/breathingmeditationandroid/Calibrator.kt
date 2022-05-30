package com.example.breathingmeditationandroid

import java.lang.System.currentTimeMillis
import kotlin.math.abs
import kotlin.math.max

object Calibrator {

    var calibratedAbdo: Pair<Double, Double> = Pair(0.0, 0.0)
    var calibratedThor: Pair<Double, Double> = Pair(0.0, 0.0)

    // Buffers for how much difference there can be for holding breath
    var holdBreathBufferInAbdo = 0.0
    var holdBreathBufferInThor = 0.0

    var holdBreathBufferMiddleAbdo = 0.0
    var holdBreathBufferMiddleThor = 0.0

    var holdBreathBufferOutAbdo = 0.0
    var holdBreathBufferOutThor = 0.0

    var flowFactorX = 0.0
    var flowFactorY = 0.0

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

    private fun calibrateMinAndMax() {

        val minimaAbdo: ArrayList<Double> = ArrayList()
        val maximaAbdo: ArrayList<Double> = ArrayList()

        var lokalMinima = 0.0
        var lokalMaxima = 0.0

        while (mService.mInspiration == 0)
            continue

        //first abdo

        while (mService.mExpiration == 0) {
            if (!lokalMinima.equals(0.0)) {
                minimaAbdo.add(lokalMinima)
                lokalMinima = 0.0
            }
            if (mService.mAbdoCorrected > lokalMaxima) {
                lokalMaxima = mService.mAbdoCorrected
            }
        }

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

        //then thor

        while (mService.mExpiration == 0) {
            if (!lokalMinima.equals(0.0)) {
                minimaThor.add(lokalMinima)
                lokalMinima = 0.0
            }
            if (mService.mThorCorrected > lokalMaxima) {
                lokalMaxima = mService.mThorCorrected

            }
        }

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
    }

    private fun calibrateFlow() {
        calibrateMinAndMax()
        calcFlowFactor()
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
        flowFactorX = (xEnd.minus(xStart)).div(combinedValueEnd.plus(correction))
        flowFactorY = (yEnd.minus(yStart)).div(combinedValueEnd.plus(correction))

    }
}