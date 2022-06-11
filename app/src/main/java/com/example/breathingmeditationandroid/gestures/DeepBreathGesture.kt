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

    override fun detect() {
        while (mService.mThorCorrected < Calibrator.calibratedThor.first * 0.8) {
            Thread.sleep(2)
        }

        while (mService.mExpiration == 0) {
        }
        Log.i("ThorBreath", "detected")

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

    override fun detect() {
        while (mService.mAbdoCorrected < Calibrator.calibratedAbdo.first * 0.8) {
            Thread.sleep(2)
        }

        while (mService.mExpiration == 0) {
        }
        Log.i("AbdoBreath", "detected")
    }

    // TODO: remove bad practice
    // https://kotlinlang.org/docs/composing-suspending-functions.html#structured-concurrency-with-async
    fun detected() = GlobalScope.async {
        detect()
        return@async true
    }
}