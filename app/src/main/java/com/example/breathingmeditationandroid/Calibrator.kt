package com.example.breathingmeditationandroid

import android.util.Log
import com.example.breathingmeditationandroid.utils.BreathingUtils
import com.example.breathingmeditationandroid.utils.ScreenUtils
import java.lang.System.currentTimeMillis
import kotlin.math.abs
import kotlin.math.max

object Calibrator {

    var calibratedAbdo: Pair<Double, Double> = Pair(1.0, 1.0)
    var calibratedThor: Pair<Double, Double> = Pair(1.0, 1.0)

    // Buffers for how much difference there can be for holding breath
    var holdBreathBufferInAbdo = 1.0
    var holdBreathBufferInThor = 1.0

    var holdBreathBufferMiddleAbdo = 1.0
    var holdBreathBufferMiddleThor = 1.0

    var holdBreathBufferOutAbdo = 1.0
    var holdBreathBufferOutThor = 1.0

    var flowFactorX = 500.0
    var flowFactorY = -250.0

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
        Log.i("Calibration", "Calibrator: Breathe in")
        while (mService.mExpiration == 0) {
            if (!lokalMinima.equals(0.0)) {
                minimaAbdo.add(lokalMinima)
                lokalMinima = 0.0
            }
            if (mService.mAbdoCorrected > lokalMaxima) {
                lokalMaxima = mService.mAbdoCorrected
            }
        }

        Log.i("Calibration", "Calibrator: Breathe out")

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
            Pair(
                mService.calculateMedian(maximaAbdo) * 1.2,
                mService.calculateMedian(minimaAbdo) * 1.2
            )

        val minimaThor: ArrayList<Double> = ArrayList()
        val maximaThor: ArrayList<Double> = ArrayList()

        //then thor
        Log.i("Calibration", "Calibrator: Breathe in")
        while (mService.mExpiration == 0) {
            if (!lokalMinima.equals(0.0)) {
                minimaThor.add(lokalMinima)
                lokalMinima = 0.0
            }
            if (mService.mThorCorrected > lokalMaxima) {
                lokalMaxima = mService.mThorCorrected

            }
        }
        Log.i("Calibration", "Calibrator: Breathe out")
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
            Pair(
                mService.calculateMedian(maximaThor) * 1.2,
                mService.calculateMedian(minimaThor) * 1.2
            )
        Log.i("Calibration", "MaxAbdo: ${calibratedAbdo.first} MinAbdo: ${calibratedAbdo.second}")
        Log.i("Calibration", "MaxThor: ${calibratedThor.first} MinThor: ${calibratedThor.second}")
    }

    fun calibrateBreathHold(time: Int, pos: String) {
        val diffValuesThor = arrayListOf<Double>()
        val diffValuesAbdo = arrayListOf<Double>()
        val startTime = currentTimeMillis()
        breathingUtils.smoothValue()
        var prevValueAbdo = breathingUtils.currAbdo
        var prevValueThor = breathingUtils.currThor
        Log.i("Calibration", "Calibrator: Hold breath")
        while (currentTimeMillis().minus(startTime) < time) {
            breathingUtils.smoothValue()
            diffValuesAbdo.add(abs(breathingUtils.currAbdo.minus(prevValueAbdo)))
            diffValuesThor.add(abs(breathingUtils.currThor.minus(prevValueThor)))
            prevValueAbdo = breathingUtils.currAbdo
            prevValueThor = breathingUtils.currThor
        }
        diffValuesAbdo.sort()
        diffValuesThor.sort()
        when (pos) {
            "in" -> {
                holdBreathBufferInAbdo =
                    max(diffValuesAbdo[diffValuesAbdo.size - 1], holdBreathBufferInAbdo)
                holdBreathBufferInThor =
                    max(diffValuesThor[diffValuesThor.size - 1], holdBreathBufferInThor)
            }
            "out" -> {
                holdBreathBufferOutAbdo =
                    max(diffValuesAbdo[diffValuesAbdo.size - 1], holdBreathBufferOutAbdo)
                holdBreathBufferOutThor =
                    max(diffValuesThor[diffValuesThor.size - 1], holdBreathBufferOutThor)
            }
            "mid" -> {
                holdBreathBufferMiddleAbdo =
                    max(diffValuesAbdo[diffValuesAbdo.size - 1], holdBreathBufferMiddleAbdo)
                holdBreathBufferMiddleThor =
                    max(diffValuesThor[diffValuesThor.size - 1], holdBreathBufferMiddleThor)
            }
        }
        Thread.sleep(15)
    }

    private fun calibrateFlow() {
        calibrateMinAndMax()
        calcFlowFactor()
    }

    private fun calcFlowFactor() {
        val xStart = ScreenUtils.xBorderLeft
        val xEnd = ScreenUtils.xBorderRight
        val yStart = ScreenUtils.yBorderBottom
        val yEnd = ScreenUtils.yBorderTop
        val combinedValueStart: Double =
            (calibratedAbdo.second.plus(calibratedThor.second))
        val combinedValueEnd: Double =
            (calibratedAbdo.first.times(0.6).plus(calibratedThor.first.times(0.6)))
        correction = 0 - combinedValueStart
        flowFactorX = (xEnd.minus(xStart)).div(combinedValueEnd.plus(correction))
        flowFactorY = (yEnd.minus(yStart)).div(combinedValueEnd.plus(correction))
    }
}