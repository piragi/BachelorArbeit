package com.example.breathingmeditationandroid.gestures

import android.util.Log
import com.example.breathingmeditationandroid.BluetoothConnection
import com.example.breathingmeditationandroid.utils.BreathingUtils
import com.example.breathingmeditationandroid.Calibrator
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

class StaccatoBreathGesture(
    private val mService: BluetoothConnection,
    private val breathingUtils: BreathingUtils
) {

    fun detect() = GlobalScope.async {
        val bufferStaccato: MutableList<Double> = mutableListOf()
        var staccatoDetetected = false

        while (!staccatoDetetected) {

            if (mService.mExpiration == 0 && mService.mAbdoCorrected > Calibrator.calibratedAbdo.first * 0.5) {
                //everytime there is a updated value -> add to buffer
                if (bufferStaccato.size == 4) {
                    if (bufferStaccato[3] != mService.mAbdoCorrected) {
                        Log.i("valuesBuffer", "$bufferStaccato")
                        Log.i("calibration", "${Calibrator.calibratedAbdo.first * 0.5}")
                        bufferStaccato.removeAt(0)
                        bufferStaccato.add(mService.mAbdoCorrected)
                    }

                    if (bufferStaccato[3] >= bufferStaccato[0] * 1.3) {
                        staccatoDetetected = true
                        Log.i("staccato", "detected")
                    }
                } else {
                    bufferStaccato.add(mService.mAbdoCorrected)
                }
            } else {
                bufferStaccato.clear()
            }
        }
        return@async true

    }

    // TODO: remove bad practice
    // https://kotlinlang.org/docs/composing-suspending-functions.html#structured-concurrency-with-async
    fun detected() = GlobalScope.async {
        detect()
        return@async true
    }
}