package com.example.breathingmeditationandroid

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.SurfaceView


class WindDrawable(context: Context) : SurfaceView(context) {
    private var startX = 0;
    private var startY = 0;

    private var endX = 0;
    private var endY = 0;

    private var paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        paint.isDither = true
        paint.color = Color.WHITE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawLine(startX.toFloat(), startY.toFloat(), endX.toFloat(), endY.toFloat(), paint)
        if (endX != 300 && endY != 300) { // set end points
            endY++
            endX++
            postInvalidateDelayed(15) // set time here
        }
    }



}