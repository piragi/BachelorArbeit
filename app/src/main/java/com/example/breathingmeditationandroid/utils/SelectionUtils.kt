package com.example.breathingmeditationandroid.utils

import android.util.Log
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import androidx.activity.ComponentActivity
import com.example.breathingmeditationandroid.Calibrator
import com.example.breathingmeditationandroid.R
import com.example.breathingmeditationandroid.gestures.HoldBreathGesture
import com.plattysoft.leonids.ParticleSystem
import kotlin.math.floor

class SelectionUtils(
    activity: ComponentActivity,
    breathingUtils: BreathingUtils,
    holdBreathGesture: HoldBreathGesture? = null,
    bubbles: Array<Pair<ImageView, Pair<Int, Int>>>
) {
    private lateinit var leavesMain: ParticleSystem
    private lateinit var leavesSupport: ParticleSystem
    private var bubbles: Array<Pair<ImageView, Pair<Int, Int>>>

    private var activity: ComponentActivity
    private var breathingUtils: BreathingUtils
    private var xBorderLeft = ScreenUtils.xBorderLeft
    private var xBorderRight = ScreenUtils.xBorderRight
    private var yBorderBottom = ScreenUtils.yBorderBottom
    private var yBorderTop = ScreenUtils.yBorderTop
    private var holdBreathGesture: HoldBreathGesture
    private var currX: Double = 0.0
    private var currY: Double = 0.0


    init {
        this.activity = activity
        this.breathingUtils = breathingUtils
        this.holdBreathGesture = holdBreathGesture!!
        this.bubbles = bubbles
        initializeLeaves()
    }

    fun animateLeavesDiagonal() {
        moveLeaves(leavesMain, calcXMovement(), calcYMovement())
        moveLeaves(leavesSupport, calcXMovement(), calcYMovement())
        detectSelection()
        breathingUtils.smoothValue()
    }

    fun animateLeavesHorizontal() {
        moveLeaves(leavesMain, calcXMovement(), ScreenUtils.yBorderBottom.toDouble())
        moveLeaves(leavesSupport, calcXMovement(), ScreenUtils.yBorderBottom.toDouble())
        detectSelection()
        breathingUtils.smoothValue()
    }

    private fun moveLeaves(
        particleSystem: ParticleSystem,
        newX: Double,
        newY: Double,
    ) {
        val x = floor(newX)
        val y = floor(newY)
        if (x in xBorderLeft.toDouble()..xBorderRight.toDouble() && y in yBorderTop.toDouble()..yBorderBottom.toDouble()) {
            particleSystem.updateEmitPoint(x.toInt(), y.toInt())
        } else if (y in yBorderTop.toDouble()..yBorderBottom.toDouble()) {
            if (x < xBorderLeft.toDouble()) {
                particleSystem.updateEmitPoint(xBorderLeft, y.toInt())
            }
            if (x > xBorderRight.toDouble()) {
                particleSystem.updateEmitPoint(xBorderRight, y.toInt())
            }
        } else if (x in xBorderLeft.toDouble()..xBorderRight.toDouble()) {
            if (y > yBorderBottom.toDouble()) {
                particleSystem.updateEmitPoint(x.toInt(), yBorderBottom)
            }
            if (y < yBorderTop.toDouble()) {
                particleSystem.updateEmitPoint(x.toInt(), yBorderTop)
            }
        }
        currX = newX
        currY = newY
    }

    private fun detectSelection() {
        var selectionDetected = false
        for (bubble in bubbles) {
            if (inBubble(bubble.second)) {
                if (bubble.second.second <= (1 / 2).times(ScreenUtils.xDimension)) {
                    holdBreathGesture.borderAbdo = Calibrator.holdBreathBufferOutAbdo
                    holdBreathGesture.borderThor = Calibrator.holdBreathBufferOutThor
                } else {
                    holdBreathGesture.borderAbdo = Calibrator.holdBreathBufferInAbdo
                    holdBreathGesture.borderThor = Calibrator.holdBreathBufferInThor
                }
                Log.i("selection", "selection detected")
                holdBreathGesture.resumeDetection()
                selectionDetected = true
                markSelection(bubble.first, 1.0f)
            }
        }
        if (!selectionDetected) {
            holdBreathGesture.stopDetection()
            for (bubble in bubbles)
                markSelection(bubble.first, 0.7f)
        }
    }

    private fun inBubble(coordinatesBubble: Pair<Int, Int>): Boolean {
        return currX in coordinatesBubble.first.toDouble()..coordinatesBubble.second.toDouble()
    }

    private fun markSelection(imageView: ImageView, alpha: Float) {
        val selected = alpha == 1.0f
        activity.runOnUiThread {
            if (selected)
                imageView.tag = "selected"
            else imageView.tag = "unselected"
            imageView.alpha = alpha
        }
    }

    private fun initializeLeaves() {
        leavesMain = ParticleSystem(activity, 10, R.drawable.leaf2, 1000)
        leavesMain.setScaleRange(0.7f, 1.3f)
            .setSpeedRange(0.05f, 0.1f)
            .setRotationSpeedRange(50f, 120f)
            .setFadeOut(500, AccelerateInterpolator())
            .emit(ScreenUtils.xBorderLeft, ScreenUtils.xBorderRight, 10)
        leavesSupport = ParticleSystem(activity, 2, R.drawable.leaf1, 500)
        leavesSupport.setScaleRange(0.7f, 1.3f)
            .setSpeedRange(0.05f, 0.1f)
            .setRotationSpeedRange(5f, 50f)
            .setFadeOut(250, AccelerateInterpolator())
            .emit(ScreenUtils.xBorderLeft, ScreenUtils.xBorderRight, 10)
    }

    fun stopLeaves() {
        leavesMain.stopEmitting()
        leavesSupport.stopEmitting()
    }

    private fun calcXMovement(): Double {
        return (breathingUtils.calcCombinedValue()).times(Calibrator.flowFactorX).plus(xBorderLeft)
    }

    private fun calcYMovement(): Double {
        return (breathingUtils.calcCombinedValue()).times(Calibrator.flowFactorY).plus(yBorderBottom)
    }
}