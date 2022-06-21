package com.example.breathingmeditationandroid.gestures

import android.util.Log
import com.example.breathingmeditationandroid.BluetoothConnection
import com.example.breathingmeditationandroid.utils.BreathingUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.lang.System.currentTimeMillis
import kotlin.concurrent.thread
import kotlin.math.abs

class HoldBreathGesture(mService: BluetoothConnection, time: Double = 4000.0) : IBreathingGesture {
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
        val buffer = 0.1 // 10% of values
        var valueCount = 0
        var errorCount = 0
        while (!hold) {
            if (!stop) {
                valueCount++
                if (!checkPrevValue(
                        Pair(localPrevAbdo, localPrevThor),
                        Pair(mService.mAbdoCorrected, mService.mThorCorrected)
                    )
                ) {
                    errorCount++
                    Log.i("buffer", "values: $valueCount")
                    Log.i("buffer", "errors: $errorCount")

                    if (valueCount >= 10 && valueCount * buffer < errorCount) {
                        valueCount = 0
                        errorCount = 0
                        localPrevAbdo = mService.mAbdoCorrected
                        localPrevThor = mService.mThorCorrected
                        startTime = currentTimeMillis()
                    }
                } else if (currentTimeMillis().minus(startTime) >= time) {
                    hold = true
                }
                Thread.sleep(15)
            } else {
                valueCount = 0
                errorCount = 0
                startTime = currentTimeMillis()
            }
        }
        return@async true

    }

    private fun checkPrevValue(prev: Pair<Double, Double>, curr: Pair<Double, Double>): Boolean {
        Log.i(
            "holdBreath",
            "${abs(prev.first.minus(curr.first)) <= borderAbdo.times(1.1)}, $borderAbdo"
        )
        Log.i(
            "holdBreath",
            "${abs(prev.second.minus(curr.second)) <= borderThor.times(1.1)}, $borderThor"
        )

        return abs(prev.first.minus(curr.first)) <= borderAbdo.times(1.1)
                && abs(prev.second.minus(curr.second)) <= borderThor.times(1.1)
    }
}