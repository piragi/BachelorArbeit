package com.example.breathingmeditationandroid
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.breathingmeditationandroid.gestures.HoldBreathGesture
import com.example.breathingmeditationandroid.utils.BreathingUtils
import com.example.breathingmeditationandroid.utils.SelectionUtils
import kotlin.concurrent.thread

class GamePause(activity: ComponentActivity, breathingUtils: BreathingUtils, holdBreathGesture: HoldBreathGesture, selectionUtils: SelectionUtils) {

    private var activity: ComponentActivity
    private var breathingUtils: BreathingUtils
    private var holdBreathGesture: HoldBreathGesture
    private var selectionUtils: SelectionUtils
    private var background: ViewGroup
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
        this.selectionUtils = selectionUtils

        background = activity.findViewById(R.id.game_screen)
        resumeBubble = activity.findViewById(R.id.resumeBubble)
        endBubble = activity.findViewById(R.id.endBubble)
        resumeText = activity.findViewById(R.id.resume)
        endText = activity.findViewById(R.id.end)

    }

    fun pauseGame() {
        startAnimation()
        detectScreenChange()
    }

    private fun startAnimation() {
        thread(start = true, isDaemon = true) {
            while (!holdBreathGesture.hold) {
                selectionUtils.animateLeavesDiagonal()
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
                resume = true
            }
        }
    }
}