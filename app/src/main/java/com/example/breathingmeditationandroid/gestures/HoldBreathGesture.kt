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
    private var breathingUtils: BreathingUtils
    private var startTime: Long = 0
    var hold = false
    private var stop = false
    var borderAbdo = 0.0
    var borderThor = 0.0
    private var time: Double

    init {
        this.time = time
        this.mService = mService
        breathingUtils = BreathingUtils(mService)
    }


    fun stopDetection() {
        stop = true
    }

    fun resumeDetection() {
        stop = false
    }


    //TODO not detected properly
    override fun detect() {
        thread(start = true, isDaemon = true) {
            breathingUtils.smoothValue()
            startTime = currentTimeMillis()
            hold = false
            var localPrevAbdo = 0.0
            var localPrevThor = 0.0
            val buffer = 0.2 // 10% of values
            var valueCount = 0
            var errorCount = 0
            while (!hold) {
                if (!stop) {
                    breathingUtils.smoothValue()
                    valueCount++
                    if (!checkPrevValue(
                            Pair(localPrevAbdo, localPrevThor),
                            Pair(breathingUtils.currAbdo, breathingUtils.currThor)
                        )
                    ) {
                        errorCount++
                        Log.i("buffer", "values: $valueCount")
                        Log.i("buffer", "errors: $errorCount")

                        if (valueCount >= 10 && valueCount * buffer < errorCount) {
                            valueCount = 0
                            errorCount = 0
                            localPrevAbdo = breathingUtils.currAbdo
                            localPrevThor = breathingUtils.currThor
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
        }
    }

    fun detected() = GlobalScope.async {
        detect()
        return@async true
    }

    private fun checkPrevValue(prev: Pair<Double, Double>, curr: Pair<Double, Double>): Boolean {
        Log.i(
            "holdBreath", "${abs(prev.first.minus(curr.first)) <= borderAbdo.times(1.1)}, $borderAbdo"
        )
        Log.i("holdBreath", "${abs(prev.second.minus(curr.second)) <= borderThor.times(1.1)}, $borderThor")

        Log.i("holdBreath", "prevAbdo: ${breathingUtils.prevAbdo}, currAbdo: ${breathingUtils.currAbdo}")
        Log.i("holdBreath", "prevThor: ${breathingUtils.prevThor}, currThor: ${breathingUtils.currThor}")

        return abs(prev.first.minus(curr.first)) <= borderAbdo.times(1.1)
                && abs(prev.second.minus(curr.second)) <= borderThor.times(1.1)
    }
}