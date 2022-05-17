package com.example.breathingmeditationandroid

class DeepChestBreathGesture(
    private val mService: BluetoothConnection,
    private val breathingUtils: BreathingUtils
) : IBreathingGesture {

    override fun detect() {
        while (mService.mThorCorrected < breathingUtils.calibratedThor.second * 0.95) {
            Thread.sleep(2)
        }

        while (mService.mExpiration == 0) {
        }
    }
}

class DeepAbdoBreathGesture(
    private val mService: BluetoothConnection,
    private val breathingUtils: BreathingUtils
) : IBreathingGesture {

    override fun detect() {
        while (mService.mAbdoCorrected < breathingUtils.calibratedAbdo.second * 0.95) {
            Thread.sleep(2)
        }

        while (mService.mExpiration == 0) {
        }
    }
}