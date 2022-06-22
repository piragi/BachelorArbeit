package com.example.breathingmeditationandroid.gestures

import android.util.Log
import com.example.breathingmeditationandroid.BluetoothConnection
import com.example.breathingmeditationandroid.Calibrator
import com.example.breathingmeditationandroid.utils.BreathingUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.lang.System.currentTimeMillis
import kotlin.concurrent.thread
import kotlin.math.abs

class HoldBreathGesture(mService: BluetoothConnection, time: Double = 2000.0) : IBreathingGesture {
    private val mService: BluetoothConnection
    private var startTime: Long = 0
    var hold = false
    private var stop = false
    var borderAbdo = 0.0
    var borderThor = 0.0
    private var time: Double

    init {
        this.time = time
        this.mService = mService
    }


    override fun stopDetection() {
        stop = true
    }

    override fun resumeDetection() {
        stop = false
    }


    //TODO not detected properly
    override fun detect() = GlobalScope.async {
        startTime = currentTimeMillis()
        hold = false
        var localPrevAbdo = 0.0
        var localPrevThor = 0.0
        var buffer = 0.1 // 10% of values
        var valueCount = 0
        var errorCount = 0
        while (!hold) {
            if (!stop) {
                valueCount++
                setBuffer()
                if (!checkPrevValue(
                        Pair(localPrevAbdo, localPrevThor),
                        Pair(mService.mAbdoCorrected, mService.mThorCorrected)
                    )
                ) {
                    errorCount++

                    if (valueCount * buffer < errorCount) {
                        valueCount = 0
                        errorCount = 0
                        localPrevAbdo = mService.mAbdoCorrected
                        localPrevThor = mService.mThorCorrected
                        startTime = currentTimeMillis()
                    }
                } else if (currentTimeMillis().minus(startTime) >= time) {
                    hold = true
                }
            } else {
                valueCount = 0
                errorCount = 0
                startTime = currentTimeMillis()
            }
        }
        return@async true
    }

    private fun setBuffer() {
        if (mService.mAbdoCorrected <= (Calibrator.calibratedAbdo.first).times(1 / 3))
            borderAbdo = Calibrator.holdBreathBufferOutAbdo
        else if (mService.mAbdoCorrected in (Calibrator.calibratedAbdo.first).times(1 / 3)..(Calibrator.calibratedAbdo.first).times(
                2 / 3
            )
        )
            borderAbdo = Calibrator.holdBreathBufferMiddleAbdo
        else if (mService.mAbdoCorrected >= (Calibrator.calibratedAbdo.first).times(2 / 3))
            borderAbdo = Calibrator.holdBreathBufferInAbdo

        if (mService.mThorCorrected <= (Calibrator.calibratedThor.first).times(1 / 3))
            borderThor = Calibrator.holdBreathBufferOutThor
        else if (mService.mThorCorrected in (Calibrator.calibratedThor.first).times(1 / 3)..(Calibrator.calibratedThor.first).times(
                2 / 3
            )
        )
            borderThor = Calibrator.holdBreathBufferMiddleThor
        else if (mService.mThorCorrected >= (Calibrator.calibratedThor.first).times(2 / 3))
            borderThor = Calibrator.holdBreathBufferInThor
    }

    private fun checkPrevValue(prev: Pair<Double, Double>, curr: Pair<Double, Double>): Boolean {
        return abs(prev.first.minus(curr.first)) <= borderAbdo.times(1.1)
                && abs(prev.second.minus(curr.second)) <= borderThor.times(1.1)
    }
}