package com.example.breathingmeditationandroid

import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

class DeepThorBreathGesture(
    private val mService: BluetoothConnection,
    private val breathingUtils: BreathingUtils
) : IBreathingGesture {

    override fun detect() {
        while (mService.mThorCorrected < breathingUtils.calibratedThor.first) {
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
        while (mService.mAbdoCorrected < breathingUtils.calibratedAbdo.first) {
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