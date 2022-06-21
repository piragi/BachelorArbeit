package com.example.breathingmeditationandroid.gestures

import android.util.Log
import com.example.breathingmeditationandroid.BluetoothConnection
import com.example.breathingmeditationandroid.utils.BreathingUtils
import com.example.breathingmeditationandroid.Calibrator
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

class DeepThorBreathGesture(
    private val mService: BluetoothConnection,
    private val breathingUtils: BreathingUtils
) : IBreathingGesture {
    private var stop = false
    override fun detect() = GlobalScope.async {
        var thorBreathDetected = false
        while (!thorBreathDetected) {
            if (!stop) {
                if (mService.mThorCorrected < Calibrator.calibratedThor.first * 0.8) {
                    Thread.sleep(2)
                }

                if (mService.mExpiration == 0) {
                }
                Log.i("ThorBreath", "detected")
                thorBreathDetected = true
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

class DeepAbdoBreathGesture(
    private val mService: BluetoothConnection,
    private val breathingUtils: BreathingUtils
) : IBreathingGesture {

    private var stop = false

    override fun detect() = GlobalScope.async {
        var abdoBreathDetected = false
        while (!abdoBreathDetected) {
            if (mService.mAbdoCorrected < Calibrator.calibratedAbdo.first * 0.8) {
                Thread.sleep(2)
            }
            Log.i("AbdoBreath", "detected")
            abdoBreathDetected = true
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