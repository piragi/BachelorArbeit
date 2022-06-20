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
) {

    fun detect() = GlobalScope.async {
        // mInhale detected
        // drop by 20% or something
        var sighDetected = false
        val bufferSigh: MutableList<Double> = mutableListOf()

        while (!sighDetected) {

            if (mService.mInspiration == 0 && mService.mThorCorrected > Calibrator.calibratedThor.first * 0.5) {
                if (bufferSigh.size == 4) {

                    if (bufferSigh[3] != mService.mThorCorrected) {
                        Log.i("sigh", "$bufferSigh")
                        Log.i("calibration", "${Calibrator.calibratedAbdo.first * 0.5}")
                        bufferSigh.removeAt(0)
                        bufferSigh.add(mService.mThorCorrected)
                    }

                    if (bufferSigh[3] <= bufferSigh[0]*0.8) {
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

    // TODO: remove bad practice
    // https://kotlinlang.org/docs/composing-suspending-functions.html#structured-concurrency-with-async
    fun detected() = GlobalScope.async {
        detect()
        return@async true
    }
}