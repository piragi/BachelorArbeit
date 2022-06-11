package com.example.breathingmeditationandroid

import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.breathingmeditationandroid.gestures.HoldBreathGesture
import kotlin.concurrent.thread

class GamePause(activity: ComponentActivity, breathingUtils: BreathingUtils, holdBreathGesture: HoldBreathGesture) {

    private var activity: ComponentActivity
    private var breathingUtils: BreathingUtils
    private var holdBreathGesture: HoldBreathGesture
    private var selectionUtils: SelectionUtils
    private lateinit var bubbles: Array<Pair<ImageView, Pair<Int, Int>>>
    private var background: RelativeLayout
    private var resumeBubble: ImageView
    private var endBubble: ImageView
    private var resumeText: TextView
    private var endText: TextView
    var resume = false
    var end = false

    init {
        this.activity = activity
        this.breathingUtils = breathingUtils
        this.holdBreathGesture = holdBreathGesture
        this.selectionUtils = SelectionUtils(activity, breathingUtils, holdBreathGesture, bubbles)

        background = activity.findViewById(R.id.game_screen)
        resumeBubble = activity.findViewById(R.id.resumeBubble)
        endBubble = activity.findViewById(R.id.endBubble)
        resumeText = activity.findViewById(R.id.resume)
        endText = activity.findViewById(R.id.end)

        bubbles = arrayOf(
            Pair(endBubble, Pair(endBubble.left, endBubble.right)),
            Pair(resumeBubble, Pair(resumeBubble.left, resumeBubble.right))
        )
    }

    fun pauseGame() {
        activity.runOnUiThread {
            background.alpha = 0.5f
            resumeBubble.alpha = 0.7f
            endBubble.alpha = 0.7f
            resumeText.alpha = 1.0f
            endText.alpha = 1.0f
        }
        startAnimation()
        detectScreenChange()
    }

    private fun startAnimation() {
        thread(start = true, isDaemon = true) {
            while (!holdBreathGesture.hold) {
                selectionUtils.animateLeaves()
                breathingUtils.smoothValue()
                Thread.sleep(2)
            }
        }
    }

    private fun detectScreenChange() {
        thread(start = true, isDaemon = true) {
            while (!holdBreathGesture.hold)
                continue
            if (endBubble.tag == "selected")
                end = true
            else if (resumeBubble.tag == "selected") {
                resumeGame()
                resume = true
            }
        }
    }

    private fun resumeGame() {
        activity.runOnUiThread {
            background.alpha = 1.0f
            resumeBubble.alpha = 0.0F
            endBubble.alpha = 0.0F
            resumeText.alpha = 0.0F
            endText.alpha = 0.0F
        }
    }
}