package com.example.breathingmeditationandroid.gestures

import android.util.Log
import com.example.breathingmeditationandroid.BluetoothConnection
import com.example.breathingmeditationandroid.BreathingUtils
import java.lang.System.currentTimeMillis
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.round

class HoldBreathGesture(mService: BluetoothConnection, time: Double) : IBreathingGesture {
    private val mService: BluetoothConnection
    private var breathingUtils: BreathingUtils
    var startTime: Long = 0
    var hold = false
    var stop = false
    var borderAbdo = 0.0
    var borderThor = 0.0
    var time: Double

    init {
        this.time = time
        this.mService = mService
        breathingUtils = BreathingUtils(mService)
    }

    override fun detect() {
        thread(start = true, isDaemon = true) {
            var prevValue = breathingUtils.smoothValue()
            while (true) {
                if (!stop) {
                    // Log.i("BreathHold", "hold: $hold")
                    val currValue = breathingUtils.smoothValue()
                    if (!checkPrevValue(
                            Pair(prevValue.first, prevValue.second),
                            Pair(currValue.first, currValue.second)
                        )
                    ) {
                        hold = false
                        startTime = currentTimeMillis()
                    } else if (currentTimeMillis().minus(startTime) >= time) {
                        hold = true
                        startTime = currentTimeMillis()
                    }
                    Thread.sleep(5)
                    prevValue = breathingUtils.smoothValue()
                } else startTime = currentTimeMillis()
            }
        }
    }

    private fun checkPrevValue(prev: Pair<Double, Double>, curr: Pair<Double, Double>): Boolean {
        Log.i("BreathHold", "Prev Abdo: ${prev.first}")
        Log.i("BreathHold", "Curr Abdo: ${curr.first}")
        Log.i("BreathHold", "Abdo: ${prev.first.minus(curr.first)}, Border: ${borderAbdo.times(1.2)}")
        // Log.i("BreathHold", "Thor: ${prev.second.minus(curr.second)}, Border: ${borderThor.times(1.2)}")

        return abs(prev.first.minus(curr.first)) <= borderAbdo.times(1.2)
                && abs(prev.second.minus(curr.second)) <= borderThor.times(1.2)
    }
}