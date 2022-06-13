package com.example.breathingmeditationandroid

import android.util.Log
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.breathingmeditationandroid.gestures.HoldBreathGesture
import com.example.breathingmeditationandroid.utils.BreathingUtils
import com.example.breathingmeditationandroid.utils.SelectionUtils
import kotlin.concurrent.thread

class GamePause(
    activity: ComponentActivity,
    holdBreathGesture: HoldBreathGesture,
    breathingUtils: BreathingUtils
) {

    private var activity: ComponentActivity
    private var breathingUtils: BreathingUtils
    private var holdBreathGesture: HoldBreathGesture
    private lateinit var selectionUtils: SelectionUtils
    private lateinit var whiteBox: ImageView
    private var background: ViewGroup
    private var resumeBubble: ImageView
    private var endBubble: ImageView
    private var resumeText: TextView
    private var endText: TextView
    private lateinit var bubbles: ArrayList<Pair<ImageView, Pair<Int, Int>>>
    var resume = false
    var end = false
    private var paused = false
    private var stop = false

    init {
        this.activity = activity
        this.holdBreathGesture = holdBreathGesture
        this.breathingUtils = breathingUtils

        background = activity.findViewById(R.id.game_screen)
        resumeBubble = activity.findViewById(R.id.resumeBubble)
        endBubble = activity.findViewById(R.id.endBubble)
        resumeText = activity.findViewById(R.id.resume)
        endText = activity.findViewById(R.id.end)

        getUiResources()
        startAnimation()
        detectScreenChange()

    }

    private fun startAnimation() {
        initializeBubbles()
        if (this::bubbles.isInitialized) {
            thread(start = true, isDaemon = true) {
                while (!stop) {
                    if (!holdBreathGesture.hold && paused) {
                        selectionUtils.animateLeavesDiagonal()
                        breathingUtils.smoothValue()
                        Thread.sleep(2)
                    }
                }
            }
        }
    }

    private fun detectScreenChange() {
        thread(start = true, isDaemon = true) {
            while (!stop) {
                if (holdBreathGesture.hold && paused) {
                    if (endBubble.tag == "selected") {
                        end = true
                    } else if (resumeBubble.tag == "selected") {
                        resume = true
                    }
                    end = false
                    resume = false
                    selectionUtils.stopLeaves()
                }
            }
        }
    }

    // TODO leaves displayed double
    fun pauseGame() {
        holdBreathGesture.detect()
        selectionUtils = SelectionUtils(activity, breathingUtils, holdBreathGesture, bubbles)
        paused = true

        activity.runOnUiThread {
            whiteBox.alpha = 0.5f
            resumeBubble.alpha = 0.7f
            endBubble.alpha = 0.7f
            resumeText.alpha = 1.0f
            endText.alpha = 1.0f
        }
    }

    fun stopAll() {
        stop = true
    }

    private fun initializeBubbles() {
        if (endBubble.left == 0 || endBubble.right == 0 || resumeBubble.left == 0 || resumeBubble.right == 0) {
            bubbles = arrayListOf()
            Log.i("bubbles", "zero only")
            endBubble.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    val left: Int = endBubble.left
                    val right: Int = endBubble.right
                    bubbles.add(Pair(endBubble, Pair(left, right)))
                    endBubble.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })
            resumeBubble.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    val left: Int = resumeBubble.left
                    val right: Int = resumeBubble.right
                    bubbles.add(Pair(resumeBubble, Pair(left, right)))
                    resumeBubble.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })
        } else bubbles = arrayListOf(
            Pair(endBubble, Pair(endBubble.left, endBubble.right)),
            Pair(resumeBubble, Pair(resumeBubble.left, resumeBubble.right))
        )
        Log.i("bubbles", "(${endBubble.left}, ${endBubble.right}), (${resumeBubble.left}, ${resumeBubble.right})")
    }

    fun resumeGame() {
        selectionUtils.stopLeaves()
        holdBreathGesture.stopDetection()
        paused = false
        activity.runOnUiThread {
            whiteBox.alpha = 0.0f
            resumeBubble.alpha = 0.0f
            endBubble.alpha = 0.0f
            resumeText.alpha = 0.0f
            endText.alpha = 0.0f
        }
    }

    private fun getUiResources() {
        whiteBox = activity.findViewById(R.id.white_box)
        resumeBubble = activity.findViewById(R.id.resumeBubble)
        endBubble = activity.findViewById(R.id.endBubble)
        resumeText = activity.findViewById(R.id.resume)
        endText = activity.findViewById(R.id.end)
        bubbles = arrayListOf(
            Pair(endBubble, Pair(endBubble.left, endBubble.right)),
            Pair(resumeBubble, Pair(resumeBubble.left, resumeBubble.right))
        )
    }
}