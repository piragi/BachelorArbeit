package com.example.breathingmeditationandroid.utils

import android.util.Log
import com.example.breathingmeditationandroid.BluetoothConnection
import com.example.breathingmeditationandroid.Calibrator
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.lang.System.currentTimeMillis
import kotlin.concurrent.thread
import kotlin.math.absoluteValue

class BreathingUtils(private val mService: BluetoothConnection) {

    var inspiration = false
    var expiration = false
    var currAbdo = 0.0
    var currThor = 0.0
    var prevAbdo = 0.0
    var prevThor = 0.0

    init {
        detectInspirationAndExpiration()
    }

    fun calculateRelativePosition(
        calibratedValue: Pair<Pair<Double, Double>, Pair<Double, Double>>,
        smoothedValue: Pair<Double, Double>
    ): Double {
        //TODO: was ist besser lesbar, mit den ganzen vals oder ohne?
        val medianAbdo = smoothedValue.first
        val medianThor = smoothedValue.second
        val calibrationAbdo = calibratedValue.first
        val calibrationThor = calibratedValue.second
        val absoluteDifference =
            ((calibrationAbdo.first + calibrationThor.first) - (calibrationThor.second + calibrationAbdo.second)).absoluteValue

        val combinedBuffer = (((medianThor * 0.6) + (medianAbdo * 0.4)))
        val steps = (absoluteDifference / 250.0)

        return combinedBuffer / steps + 430.0
    }

    fun smoothPlayerPosition(): Pair<Double, Double> {
        val bufferAbdo: ArrayList<Double> = ArrayList()
        val bufferThor: ArrayList<Double> = ArrayList()

        while (bufferAbdo.size <= 4 || bufferThor.size <= 6) {
            if (bufferAbdo.isEmpty() || !bufferAbdo[bufferAbdo.size - 1].equals(mService.mAbdoCorrected)) {
                bufferAbdo.add(mService.mAbdoCorrected)
            }
            if (bufferThor.isEmpty() || bufferThor[bufferThor.size - 1] != mService.mThorCorrected) {
                bufferThor.add(mService.mThorCorrected)
            }
        }
        val medianAbdo = mService.smoothData(bufferAbdo)
        val medianThor = mService.smoothData(bufferThor)

        bufferThor.clear()
        bufferAbdo.clear()

        return Pair(medianAbdo, medianThor)
    }

    fun deepBreathDetected() {
        while (mService.mAbdoCorrected < Calibrator.calibratedAbdo.second * 0.95
            || mService.mThorCorrected < Calibrator.calibratedThor.second * 0.95
        ) {
            Thread.sleep(2)
        }

        while (mService.mExpiration == 0) {
        }
    }


    // mehr "real time" als inspiration und expiration von mService. Ist teilweise wichtig
    private fun detectInspirationAndExpiration() {
        var abdoBuffer = arrayListOf<Double>()
        var thorBuffer = arrayListOf<Double>()

        val bufferSize = 100

        thread(start = true, isDaemon = true) {
            while (true) {

                // Log.i("utils", "inspiration: $inspiration")
                // Log.i("utils", "expiration: $expiration")

                smoothValue()

                if (abdoBuffer.size < bufferSize && thorBuffer.size < bufferSize) {
                    abdoBuffer.add(currAbdo)
                    thorBuffer.add(currThor)
                } else {
                    val abdoAvg = abdoBuffer.average()
                    val thorAvg = thorBuffer.average()

                    if (abdoAvg != currAbdo && thorAvg != currThor) {
                        expiration = abdoAvg > currAbdo && thorAvg > currThor
                        inspiration = abdoAvg < currAbdo && thorAvg < currThor
                    }

                    abdoBuffer = arrayListOf()
                    thorBuffer = arrayListOf()
                }

                Thread.sleep(10)
            }
        }
    }

    private fun detectRespiration(
        abdoBuffer: ArrayList<Double>,
        thorBuffer: ArrayList<Double>,
        curr: Pair<Double, Double>
    ): Boolean {
        return curr.first < abdoBuffer.average() && curr.second < thorBuffer.average()
    }

    private fun detectInspiration(
        abdoBuffer: ArrayList<Double>,
        thorBuffer: ArrayList<Double>,
        curr: Pair<Double, Double>
    ): Boolean {
        return curr.first > abdoBuffer.average() && curr.second > thorBuffer.average()
    }

    fun startFromBeginning() {
        repeat(1) {
            while (mService.mExpiration == 0) {
                continue
            }
            while (mService.mInspiration == 0) {
                continue
            }
        }
    }

    fun smoothValue() {
        val valueListAbdo = arrayListOf(mService.mAbdoCorrected, mService.mThorCorrected)
        val valueListThor = arrayListOf(mService.mThorCorrected)
        while (valueListAbdo.size <= 6 && valueListThor.size <= 6) {
            valueListAbdo.add(mService.mAbdoCorrected)
            valueListThor.add(mService.mThorCorrected)
        }
        prevAbdo = currAbdo
        prevThor = currThor

        currAbdo = mService.smoothData(valueListAbdo)
        currThor = mService.smoothData(valueListThor)
    }

    fun calcCombinedValue(): Double {
        val combinedValue = (currAbdo.plus(currThor)).plus(Calibrator.correction)
        return if (combinedValue >= 0)
            combinedValue
        else 0.0
    }

    private fun calculateMedian(list: MutableList<Pair<Double, Double>>): Pair<Double, Double> {
        val medianThor =
            (list[list.size.div(2)].first.plus(list[list.size.div(2).plus(1)].first)).div(2)
        val medianAbdo =
            (list[list.size.div(2)].second.plus(list[list.size.div(2).plus(1)].second)).div(2)
        return Pair(medianAbdo, medianThor)
    }


    fun detectInspiration() = GlobalScope.async {
        while (mService.mExpiration == 0)
            continue
        return@async true
    }

    fun detectExpiration() = GlobalScope.async {
        while (mService.mInspiration == 0)
            continue
        return@async true
    }

    fun detectFiveSecondInspiration(): Boolean {
        var startTime = currentTimeMillis()
        while (true) {
            if (mService.mExpiration == 0 && currentTimeMillis().minus(startTime) >= 4000) {
                Log.i("calibration", "breathe in for 4 sec detected")
                return true
            } else if (mService.mInspiration == 0) startTime = currentTimeMillis()
        }
    }
}