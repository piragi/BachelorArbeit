package com.example.breathingmeditationandroid

import android.util.Log
import java.lang.System.currentTimeMillis
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.round

class HoldBreathGesture(mService: BluetoothConnection) : IBreathingGesture {
    private val mService: BluetoothConnection
    private var breathingUtils: BreathingUtils
    var startTime: Long = 0
    var hold = false
    var stop = true
    var firstBorder = 0.0
    var secondBorder = 0.0

    init {
        this.mService = mService
        breathingUtils = BreathingUtils(mService)
    }

    override fun detect() {
        var prevValue = breathingUtils.smoothPlayerPosition()
        thread(start = true, isDaemon = true) {
            startTime = currentTimeMillis()
            while (true) {
                if (!stop) {
                    val currTime = currentTimeMillis()
                    val currValue = breathingUtils.smoothPlayerPosition()
                    if (!checkPrevValue(
                            Pair(prevValue.second.plus(10), prevValue.second.plus(10)),
                            Pair(currValue.first.plus(10), currValue.second.plus(10))
                        )
                    ) {
                        hold = false
                        startTime = currentTimeMillis()
                    } else if (currTime.minus(startTime) >= 3000) {
                        hold = true
                        startTime = currentTimeMillis()
                    }
                    prevValue = breathingUtils.smoothPlayerPosition()
                }
            }
        }
    }

    private fun checkPrevValue(prev: Pair<Double, Double>, curr: Pair<Double, Double>): Boolean {
        Log.i("values:", "first: ${abs(prev.first.minus(curr.first))}")
        Log.i("values:", "second: ${abs(prev.second.minus(curr.second))}")
        // IDEE calibration und dabei "mittlere aenderung" messen und dann in die breathing gesture miteinbeziehen
        return (round(abs(prev.first.minus(curr.first))) <= 1 && round(abs(prev.second.minus(curr.second))) <= 1)
    }
}