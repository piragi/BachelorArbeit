package com.example.breathingmeditationandroid.utils

import androidx.appcompat.app.AppCompatActivity

object ScreenUtils {
    var xDimension = 0
    var yDimension = 0
    var xBorderLeft = 0
    var yBorderBottom = 0
    var xBorderRight = 0
    var yBorderTop = 0
    lateinit var absoluteX: Pair<Int, Int>
    lateinit var absoluteY: Pair<Int, Int>

    var initialized = false

    fun initialize(activity: AppCompatActivity) {
        if (!initialized) {
            val metrics = activity.applicationContext.resources.displayMetrics
            xDimension = metrics.widthPixels
            yDimension = metrics.heightPixels

            xBorderLeft = 100
            xBorderRight = xDimension.minus(100)
            yBorderBottom = yDimension.minus(300)
            yBorderTop = 300

            absoluteX = Pair(0, xDimension)
            absoluteY = Pair(yDimension, 0)
        }
    }
}