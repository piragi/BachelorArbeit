package com.example.breathingmeditationandroid.gestures

import android.util.Log
import com.example.breathingmeditationandroid.BluetoothConnection
import com.example.breathingmeditationandroid.utils.BreathingUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.lang.System.currentTimeMillis
import kotlin.concurrent.thread
import kotlin.math.abs

class HoldBreathGesture(mService: BluetoothConnection, time: Double = 5000.000) : IBreathingGesture {
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
            while (!hold) {
                if (!stop) {
                    breathingUtils.smoothValue()
                    if (!checkPrevValue(
                            Pair(breathingUtils.prevAbdo, breathingUtils.prevThor),
                            Pair(breathingUtils.currAbdo, breathingUtils.currThor)
                        )
                    ) {
                        startTime = currentTimeMillis()
                    } else if (currentTimeMillis().minus(startTime) >= time) {
                        hold = true
                    }
                    Thread.sleep(2)
                    breathingUtils.smoothValue()
                } else startTime = currentTimeMillis()
            }
        }
    }

    fun detected() = GlobalScope.async {
        detect()
        return@async true
    }

    private fun checkPrevValue(prev: Pair<Double, Double>, curr: Pair<Double, Double>): Boolean {
        Log.i(
            "holdBreath", "${abs(prev.first.minus(curr.first)) == 0.0}, ${abs(prev.second.minus(curr.second)) == 0.0}"
        )
        return abs(prev.first.minus(curr.first)) == 0.0
                && abs(prev.second.minus(curr.second)) == 0.0
    }
}