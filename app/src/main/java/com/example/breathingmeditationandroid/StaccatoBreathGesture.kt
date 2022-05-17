package com.example.breathingmeditationandroid

import android.util.Log

class StaccatoBreathGesture(private val mService: BluetoothConnection,
                            private val breathingUtils: BreathingUtils
) :
    IBreathingGesture {

    override fun detect() {
        val bufferStaccato: ArrayList<Double> = ArrayList()
        var staccatoDetetected = false

        while (!staccatoDetetected) {

            if (mService.mExpiration == 0 && mService.mThorCorrected > breathingUtils.calibratedThor.first * 0.6) {
                //everytime there is a updated value -> add to buffer
                if (bufferStaccato.size == 3) {
                    bufferStaccato.removeAt(0)
                    bufferStaccato.add(mService.mThorCorrected)
                    if (bufferStaccato[2] >= bufferStaccato[0] * 1.5) {
                        staccatoDetetected = true
                        Log.i("valuesBuffer", "$bufferStaccato")
                        Log.i("calibration", "${breathingUtils.calibratedThor.first * 0.6}")
                        Log.i("staccato", "detected")
                    }
                } else {
                    bufferStaccato.add(mService.mThorCorrected)
                }
                Thread.sleep(2)
            }
        }
    }


}