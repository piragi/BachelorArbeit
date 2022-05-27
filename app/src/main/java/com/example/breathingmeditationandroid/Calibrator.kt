package com.example.breathingmeditationandroid

import android.util.Log
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.lang.System.currentTimeMillis
import kotlin.math.abs

object Calibrator {

    var calibratedAbdo: Pair<Double, Double> = Pair(0.0, 0.0)
    var calibratedThor: Pair<Double, Double> = Pair(0.0, 0.0)

    // Buffers for how much difference there can be for holding breath
    private var holdBreathBufferInAbdo = 0.0
    private var holdBreathBufferInThor = 0.0

    //TODO check if this can be calculated from in and out buffer
    private var holdBreathBufferMiddleAbdo = 0.0
    private var holdBreathBufferMiddleThor = 0.0

    private var holdBreathBufferOutAbdo = 0.0
    private var holdBreathBufferOutThor = 0.0

    var flowFactorXUp = 0.0
    var flowFactorYUp = 0.0

    var correction = 0.0

    private lateinit var breathingUtils: BreathingUtils
    private lateinit var mService: BluetoothConnection


    fun initialize(mService: BluetoothConnection) {
        this.mService = mService
        breathingUtils = BreathingUtils(mService)
    }

    suspend fun calibrate() = coroutineScope {
        launch {
            calibrateFlow()
        }
    }

    //TODO: muss doch smarter gehen
    //TODO: als Coroutine dann kann sich der screen schön bewegen dazwischen

    private fun calibrateMinAndMax() {
        Log.i("calibration", "calibrate min max starting")
        val minimaAbdo: ArrayList<Double> = ArrayList()
        val maximaAbdo: ArrayList<Double> = ArrayList()
        val minimaThor: ArrayList<Double> = ArrayList()
        val maximaThor: ArrayList<Double> = ArrayList()
        //TODO: lokal mit k?!?!
        var lokalMinima = 0.0
        var lokalMaxima = 0.0

        Log.i("Calibration:", "Abdo")
        //first abdo
        repeat(1) {
            while (mService.mExpiration == 0) {
                if (!lokalMinima.equals(0.0)) {
                    minimaAbdo.add(lokalMinima)
                    lokalMinima = 0.0
                }
                if (mService.mAbdoCorrected > lokalMinima) {
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
        }
        Log.i("Calibration:", "thor")
        //then thor
        repeat(1) {
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
        }
        Log.i("calibration", "min and max done")
        calibratedAbdo =
            Pair(mService.calculateMedian(maximaAbdo) * 1.2, mService.calculateMedian(minimaAbdo) * 1.2)
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
                holdBreathBufferInAbdo = diffValuesAbdo.average()
                holdBreathBufferInThor = diffValuesThor.average()
            }
            "out" -> {
                holdBreathBufferOutAbdo = diffValuesAbdo.average()
                holdBreathBufferOutThor = diffValuesThor.average()
            }
            "mid" -> {
                holdBreathBufferMiddleAbdo = diffValuesAbdo.average()
                holdBreathBufferMiddleThor = diffValuesThor.average()
            }
        }
        Log.i("calibration", "breath hold calibration done")
    }

    private suspend fun calibrateFlow() = coroutineScope {
        Log.i("calibration", "calibrating flow")
        calibrateMinAndMax()
        calcFlowFactor()
        Log.i("calibration", "finished calibrating flow")
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
            (calibratedAbdo.first.times(0.9).plus(calibratedThor.first.times(0.9)))
        correction = 0 - combinedValueStart
        flowFactorXUp = (xEnd.minus(xStart)).div(combinedValueEnd.plus(correction))
        flowFactorYUp = (yEnd.minus(yStart)).div(combinedValueEnd.plus(correction))
    }
}