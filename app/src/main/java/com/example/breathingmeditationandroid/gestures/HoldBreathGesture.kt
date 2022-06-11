package com.example.breathingmeditationandroid.gestures

import android.util.Log
import com.example.breathingmeditationandroid.BluetoothConnection
import com.example.breathingmeditationandroid.utils.BreathingUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.lang.System.currentTimeMillis
import kotlin.concurrent.thread
import kotlin.math.abs

class HoldBreathGesture(mService: BluetoothConnection, time: Double) : IBreathingGesture {
    private val mService: BluetoothConnection
    private var breathingUtils: BreathingUtils
    var startTime: Long = 0
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

    override fun detect() {
        thread(start = true, isDaemon = true) {
            breathingUtils.smoothValue()
            startTime = currentTimeMillis()
            while (!hold) {
                borderAbdo = 1.0
                borderThor = 1.0
                if (!stop) {
                    breathingUtils.smoothValue()
                    if (!checkPrevValue(
                            Pair(breathingUtils.prevAbdo, breathingUtils.prevThor),
                            Pair(breathingUtils.currAbdo, breathingUtils.currThor)
                        )
                    ) {
                        Log.i("breathHold", "prevAbdo: ${breathingUtils.prevAbdo}, currAbdo: ${breathingUtils.currAbdo}")
                        Log.i("breathHold", "currAbdo: ${breathingUtils.prevThor}, currThor: ${breathingUtils.currThor}")

                        hold = false
                        startTime = currentTimeMillis()
                    } else if (currentTimeMillis().minus(startTime) >= time) {
                        hold = true
                    }
                    Thread.sleep(5)
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
        return abs(prev.first.minus(curr.first)) <= borderAbdo.times(1.2)
                && abs(prev.second.minus(curr.second)) <= borderThor.times(1.2)
    }
}