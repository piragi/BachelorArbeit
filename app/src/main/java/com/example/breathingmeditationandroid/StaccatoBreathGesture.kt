package com.example.breathingmeditationandroid

import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

class StaccatoBreathGesture(
    private val mService: BluetoothConnection,
    private val breathingUtils: BreathingUtils
) :
    IBreathingGesture {

    override fun detect() {
        val bufferStaccato: MutableList<Double> = mutableListOf()
        var staccatoDetetected = false

        while (!staccatoDetetected) {

            if (mService.mExpiration == 0 && mService.mAbdoCorrected > breathingUtils.calibratedAbdo.first * 0.5) {
                //everytime there is a updated value -> add to buffer
                if (bufferStaccato.size == 4) {
                    if (bufferStaccato[3] != mService.mAbdoCorrected) {
                        Log.i("valuesBuffer", "$bufferStaccato")
                        Log.i("calibration", "${breathingUtils.calibratedAbdo.first * 0.5}")
                        bufferStaccato.removeAt(0)
                        bufferStaccato.add(mService.mAbdoCorrected)
                    }

                    if (bufferStaccato[3] >= bufferStaccato[0] * 1.4) {
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
    }

    // TODO: remove bad practice
    // https://kotlinlang.org/docs/composing-suspending-functions.html#structured-concurrency-with-async
    fun detected() = GlobalScope.async {
        detect()
        return@async true
    }
}