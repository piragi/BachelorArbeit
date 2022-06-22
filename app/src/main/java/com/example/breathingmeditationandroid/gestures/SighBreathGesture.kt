package com.example.breathingmeditationandroid.gestures

import android.util.Log
import com.example.breathingmeditationandroid.BluetoothConnection
import com.example.breathingmeditationandroid.utils.BreathingUtils
import com.example.breathingmeditationandroid.Calibrator
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

class SighBreathGesture(
    private val mService: BluetoothConnection,
    private val breathingUtils: BreathingUtils
) : IBreathingGesture {

    private var stop = false

    override fun detect() = GlobalScope.async {
        // mInhale detected
        // drop by 20% or something
        var sighDetected = false
        val bufferSize = 10
        val bufferSigh: MutableList<Double> = mutableListOf()

        while (!sighDetected) {
            if (!stop && mService.mThorCorrected > Calibrator.calibratedThor.first * 0.6) {
                if (bufferSigh.size == bufferSize) {

                    if (bufferSigh[bufferSize - 1] != mService.mThorCorrected) {
                        Log.i("sigh", "$bufferSigh")
                        Log.i("calibration", "${Calibrator.calibratedAbdo.first * 0.5}")
                        bufferSigh.removeAt(0)
                        bufferSigh.add(mService.mThorCorrected)
                    }

                    if (bufferSigh[bufferSize - 1] <= bufferSigh[0] * 0.75) {
                        sighDetected = true

                        Log.i("sigh", "detected")

                    }
                } else {
                    bufferSigh.add(mService.mThorCorrected)
                }
            } else {
                bufferSigh.clear()
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