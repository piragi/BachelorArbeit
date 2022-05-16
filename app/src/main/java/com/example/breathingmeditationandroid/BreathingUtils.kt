package com.example.breathingmeditationandroid

import android.util.Log
import kotlin.math.absoluteValue

class BreathingUtils(mService: BluetoothConnection) {

    private val mService: BluetoothConnection = mService

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

    fun detectRespiration(prev: Pair<Double, Double>, curr: Pair<Double, Double>): Boolean {
        return curr.first < prev.first && curr.second < prev.second
    }

    fun detectInspiration(prev: Pair<Double, Double>, curr: Pair<Double, Double>): Boolean {
        return curr.first > prev.first && curr.second > prev.second
    }

    fun startFromBeginning() {
        repeat(1) {
            while (mService.mExpiration == 0) {
                continue
            }
            Log.i("Calibration:", "inspiration done")
            while (mService.mInspiration == 0) {
                continue
            }
            Log.i("Calibration:", "expiration done")
        }
    }

    fun smoothValue(): Pair<Double, Double> {
        val valueListAbdo = arrayListOf(mService.mAbdoCorrected, mService.mThorCorrected)
        val valueListThor = arrayListOf(mService.mThorCorrected)
        while (valueListAbdo.size <= 6 && valueListThor.size <= 6) {
            valueListAbdo.add(mService.mAbdoCorrected)
            valueListThor.add(mService.mThorCorrected)
        }
        return Pair(mService.smoothData(valueListAbdo), mService.smoothData(valueListThor));
    }

    fun calcCombinedValue(valAbdo: Double, valThor: Double): Double {
        /* Log.i("CurrValues:", "Abdo: $valAbdo Thor: $valThor")
        Log.i("CurrValues:", "Before correction: ${valAbdo.plus(valThor)}")
        Log.i("CurrValues:", "After correction: ${valAbdo.plus(valThor).plus(Calibrator.correction)}")*/
        val combinedValue = (valAbdo.plus(valThor)).plus(Calibrator.correction)
        return if (combinedValue >= 0)
            combinedValue
        else {
            Calibrator.correction = 0 - combinedValue
            0.0
        }
    }


}