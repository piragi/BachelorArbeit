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
) : IBreathingGesture {

    private var stop = false

    override fun detect() = GlobalScope.async {
        val bufferStaccato: MutableList<Double> = mutableListOf()
        var staccatoDetetected = false

        while (!staccatoDetetected) {
            if (!stop && mService.mAbdoCorrected > Calibrator.calibratedAbdo.first * 0.35) {
                //everytime there is a updated value -> add to buffer
                if (bufferStaccato.size == 4) {
                    if (bufferStaccato[3] != mService.mAbdoCorrected) {
                        Log.i("valuesBuffer", "$bufferStaccato")
                        Log.i("calibration", "${Calibrator.calibratedAbdo.first * 0.5}")
                        bufferStaccato.removeAt(0)
                        bufferStaccato.add(mService.mAbdoCorrected)
                    }

                    if (bufferStaccato[3] >= bufferStaccato[0] * 1.45) {
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

    override fun stopDetection() {
        stop = true
    }

    override fun resumeDetection() {
        stop = false
    }

    // TODO: remove bad practice
    // https://kotlinlang.org/docs/composing-suspending-functions.html#structured-concurrency-with-async
    fun detected() = GlobalScope.async {
        detect()
        return@async true
    }
}