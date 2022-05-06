package com.example.breathingmeditationandroid

import android.util.Log
import java.lang.System.currentTimeMillis
import kotlin.concurrent.thread
import kotlin.math.abs

class HoldBreathGesture(mService: BluetoothConnection) : IBreathingGesture {
    private val mService: BluetoothConnection
    private var breathingUtils: BreathingUtils
    private var startTime: Long = 0
    private var prevTime: Long = 0
    var hold = false
    private var values = arrayListOf<Pair<Double, Double>>()

    init {
        this.mService = mService
        breathingUtils = BreathingUtils(mService)
    }

    override fun detect() {
        var steps = 0
        thread(start = true, isDaemon = true) {
            while (true) {
                if (startTime == 0.toLong()) {
                    hold = false
                    startTime = currentTimeMillis()
                    prevTime = startTime
                    values = arrayListOf()
                }
                val currTime = currentTimeMillis()
                if (currTime.minus(startTime) < 3000) {
                    val values = breathingUtils.smoothValue()
                    if (steps.mod(100) == 0) {
                        this.values.add(Pair(values.first.times(100), values.second.times(100)))
                    }
                    steps++
                } else {
                    hold = checkSimilarities()
                    Log.i("hold:", "$hold")
                    Thread.sleep(20)
                    startTime = 0
                    steps = 0
                }
            }
        }
    }

    private fun checkSimilarities(): Boolean {
        if (values.size > 0) {
            val valuesFirst = values[0]
            val valuesLast = values[values.size.minus(1)]
            val firstLastAvgAbdo = (valuesFirst.first.plus(valuesLast.first)).div(2)
            val firstLasAvgThor = (valuesFirst.second.plus(valuesLast.second)).div(2)
            var abdoAvg = 0.0
            var thorAvg = 0.0
            for (pair in values) {
                abdoAvg = abdoAvg.plus(pair.first)
                thorAvg = thorAvg.plus(pair.second)
            }
            abdoAvg = abdoAvg.div(values.size)
            thorAvg = thorAvg.div(values.size)
            return abs(abdoAvg.minus(firstLastAvgAbdo)) < 10 && abs(thorAvg.minus(firstLasAvgThor)) < 5
        }
        return false
    }
}