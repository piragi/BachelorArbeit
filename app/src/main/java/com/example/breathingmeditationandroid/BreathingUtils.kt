package com.example.breathingmeditationandroid

import android.util.Log
import kotlin.math.absoluteValue

class BreathingUtils(mService: BluetoothConnection) {

    private val mService: BluetoothConnection = mService

    //TODO: muss doch smarter gehen
    //TODO: als Coroutine dann kann sich der screen schön bewegen dazwischen
    fun calibrateBreathing(): Pair<Pair<Double, Double>, Pair<Double, Double>> {

        val minimaAbdo: ArrayList<Double> = ArrayList()
        val maximaAbdo: ArrayList<Double> = ArrayList()
        val minimaThor: ArrayList<Double> = ArrayList()
        val maximaThor: ArrayList<Double> = ArrayList()
        //TODO: lokal mit k?!?!
        var lokalMinima = 0.0
        var lokalMaxima = 0.0

        Log.i("Calibration:", "Abdo")
        //first abdo
        repeat(4) {
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
        repeat(4) {
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

        Log.i("arrayAbdoMax:", "$maximaAbdo")

        return Pair(
            Pair(mService.calculateMedian(maximaAbdo)*1.2, mService.calculateMedian(minimaThor)*0.8),
            Pair(mService.calculateMedian(maximaThor)*1.2, mService.calculateMedian(minimaThor)*0.8)
        )

    }

    fun calculateRelativePosition(calibratedValue: Pair<Pair<Double,Double>, Pair<Double,Double>>, smoothedValue: Pair<Double, Double>) : Double {
        //TODO: was ist besser lesbar, mit den ganzen vals oder ohne?
        val medianAbdo = smoothedValue.first
        val medianThor = smoothedValue.second
        val calibrationAbdo = calibratedValue.first
        val calibrationThor = calibratedValue.second
        val absoluteDifference = ((calibrationAbdo.first + calibrationThor.first) - (calibrationThor.second + calibrationAbdo.second)).absoluteValue

        val combinedBuffer = (((medianThor * 0.6) + (medianAbdo * 0.4)) * 300)
        val steps = (absoluteDifference / 300.0)

        return combinedBuffer / steps + 300.0
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
        Log.i("medianAbdo", "$medianAbdo")
        Log.i("bufferAbdo", "$bufferAbdo")
        Log.i("medianThor", "$medianThor")
        Log.i("bufferThor", "$bufferThor")

        bufferThor.clear()
        bufferAbdo.clear()

        return Pair(medianAbdo, medianThor)
    }

    fun smoothValue(): Pair<Double, Double> {
        val valueList = mutableListOf(Pair(mService.mAbdoCorrected, mService.mThorCorrected))
        while (valueList.size <= 6) {
            valueList.add(Pair(mService.mAbdoCorrected, mService.mThorCorrected))
        }
        return calculateMedian(valueList)
    }

    private fun calculateMedian(list: MutableList<Pair<Double, Double>>): Pair<Double, Double> {
        val medianThor = (list[list.size.div(2)].first.plus(list[list.size.div(2).plus(1)].first)).div(2)
        val medianAbdo = (list[list.size.div(2)].second.plus(list[list.size.div(2).plus(1)].second)).div(2)
        return Pair(medianAbdo, medianThor)
    }
}