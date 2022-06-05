package com.example.breathingmeditationandroid

import android.util.DisplayMetrics
import android.view.Display
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity


object ScreenUtils {
    var xDimension = 0
    var yDimension = 0
    var xBorderLeft = 0
    var yBorderBottom = 0
    var xBorderRight = 0
    var yBorderTop = 0

    fun initialize(activity: AppCompatActivity) {
        val metrics = activity.applicationContext.resources.displayMetrics
        xDimension = metrics.widthPixels
        yDimension = metrics.heightPixels

        xBorderLeft = 100
        xBorderRight = xDimension.minus(280)
        yBorderBottom = yDimension.minus(280)
        yBorderTop = 300
    }
}