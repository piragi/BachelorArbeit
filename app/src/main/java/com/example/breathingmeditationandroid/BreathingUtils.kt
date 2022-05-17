package com.example.breathingmeditationandroid

import android.util.Log
import androidx.fragment.app.Fragment
import kotlin.math.absoluteValue

class BreathingUtils(mService: BluetoothConnection) {

    private val mService: BluetoothConnection = mService

    lateinit var calibratedAbdo: Pair<Double, Double>
    lateinit var calibratedThor: Pair<Double, Double>

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

        calibratedAbdo = Pair(mService.calculateMedian(maximaAbdo) * 1.2, mService.calculateMedian(minimaAbdo) * 1.2)
        calibratedThor = Pair(mService.calculateMedian(maximaThor) * 1.2, mService.calculateMedian(minimaThor) * 1.2)

        return Pair(
            calibratedAbdo,
            calibratedThor
        )
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

    fun deepBellyBreathDetected() {
        while (mService.mAbdoCorrected < calibratedAbdo.second * 0.95)
        {
            Thread.sleep(2)
        }

        while (mService.mExpiration == 0) {
        }
    }

    fun deepChestBreathDetected() {
        while (mService.mThorCorrected < calibratedThor.second * 0.95)
        {
            Thread.sleep(2)
        }

        while (mService.mExpiration == 0) {
        }
    }

    fun staccatoBreathDetected() {

        //for every breath > half of max
        //Expiration == 0
        //for every changed value
        //buffer of 3
        //if difference of 20% between first and last
        //staccato ->

        val bufferStaccato: ArrayList<Double> = ArrayList()

        if(mService.mExpiration == 0 && mService.mThorCorrected > calibratedThor.second * 0.6) {
            //everytime there is a updated value -> add to buffer
            if(bufferStaccato.size == 3) {
                bufferStaccato.removeAt(0)
                bufferStaccato.add(mService.mThorCorrected)
                if (bufferStaccato[2] >= bufferStaccato[0]*1.2) {
                    Log.i("staccato", "detected")
                }
            } else {
                bufferStaccato.add(mService.mThorCorrected)
            }
        }

    }

    fun detectRespiration(prev: Pair<Double, Double>, curr: Pair<Double, Double>): Boolean {
        return curr.first < prev.first && curr.second < prev.second
    }

    fun detectInspiration(prev: Pair<Double, Double>, curr: Pair<Double, Double>): Boolean {
        return curr.first > prev.first && curr.second > prev.second
    }

    fun startFromBeginning(prevAbdo: Double, prevThor: Double) {
        // damit man nicht mitten in der atmung anfaengt
        if (detectInspiration(
                Pair(prevAbdo, prevThor),
                Pair(smoothPlayerPosition().first, smoothPlayerPosition().second)
            )
        ) {
            while (!detectRespiration(
                    Pair(prevAbdo, prevThor),
                    Pair(smoothPlayerPosition().first, smoothPlayerPosition().second)
                )
            )
                continue
        } else
            while (!detectInspiration(
                    Pair(prevAbdo, prevThor),
                    Pair(smoothPlayerPosition().first, smoothPlayerPosition().second)
                )
            )
                continue
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