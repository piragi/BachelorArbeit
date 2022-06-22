package com.example.breathingmeditationandroid

import android.util.Log
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.breathingmeditationandroid.gestures.HoldBreathGesture
import com.example.breathingmeditationandroid.utils.BreathingUtils
import com.example.breathingmeditationandroid.utils.ScreenUtils
import com.example.breathingmeditationandroid.utils.SelectionUtils
import kotlinx.coroutines.*
import kotlin.concurrent.thread

class GamePause(
    activity: ComponentActivity,
    breathingUtils: BreathingUtils
) {

    private var activity: ComponentActivity
    private var breathingUtils: BreathingUtils
    private lateinit var selectionUtils: SelectionUtils
    private lateinit var whiteBox: ImageView
    private var background: ViewGroup
    private var resumeBubble: ImageView
    private var endBubble: ImageView
    private var resumeText: TextView
    private var endText: TextView
    private lateinit var bubbles: ArrayList<Pair<ImageView, Pair<Int, Int>>>
    private var resume = false
    private var end = false
    private var stop = false

    init {
        this.activity = activity
        this.breathingUtils = breathingUtils

        background = activity.findViewById(R.id.game_screen)
        resumeBubble = activity.findViewById(R.id.resumeBubble)
        endBubble = activity.findViewById(R.id.endBubble)
        resumeText = activity.findViewById(R.id.resume)
        endText = activity.findViewById(R.id.end)

        getUiResources()
        initializeBubbles()
    }

    fun waitForResourcesAsync() = GlobalScope.async {
        while (!this@GamePause::bubbles.isInitialized)
            continue
        Log.i("bubbles", "${bubbles[0].second}, ${bubbles[1].second}")
        return@async true
    }

    private fun startAnimation() {
        Log.i("bubbles", "animation started")
        Log.i("bubbles", "${bubbles[0].second}, ${bubbles[1].second}")
        this.selectionUtils =
            SelectionUtils(activity, breathingUtils, bubbles)
        thread(start = true, isDaemon = true) {
            while (!resume && !end) {
                try {
                    selectionUtils.animateLeavesDiagonal()
                    Thread.sleep(2)
                } catch (e: ConcurrentModificationException) {
                    continue
                }
            }
        }
    }


    private suspend fun resumeGame() {
        val selectionDetected = selectionUtils.detectSafeStopAsync()
        if (selectionDetected.await()) {
            selectionUtils.stopLeaves()
            if (endBubble.tag == "selected") {
                end = true
            } else {
                resume = true
            }
        }
    }

    fun gameResumedAsync() = GlobalScope.async {
        while (!resume)
            continue
        stop = true
        return@async true
    }

    fun gameEndedAsync() = GlobalScope.async {
        while (!end)
            continue
        stop = true
        return@async true
    }

    private suspend fun pauseTextDisplay() = coroutineScope {
        val text = activity.findViewById<TextView>(R.id.pauseText)
        activity.runOnUiThread {
            text.text = "Game paused"
            text.animate()
                .alpha(1.0f)
                .y(50f)
                .setDuration(1000)
                .setListener(null)
        }
        delay(1000)
    }

    private suspend fun pauseTextReposition() = coroutineScope {
        val text = activity.findViewById<TextView>(R.id.pauseText)
        activity.runOnUiThread {
            text.text = "Game resumed"
            text.animate()
                .y(ScreenUtils.yDimension.div(2).toFloat())
                .setDuration(2000)
                .setListener(null)
        }
        delay(2000)
        activity.runOnUiThread {
            text.animate()
                .alpha(0.0f)
                .setDuration(500)
                .setListener(null)
        }
    }

    fun pauseGame() {
        GlobalScope.launch {
            pauseTextDisplay()
        }
        activity.runOnUiThread {
            whiteBox.alpha = 0.5f
            resumeBubble.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.fadein))
            resumeBubble.alpha = 0.7f
            endBubble.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.fadein))
            endBubble.alpha = 0.7f
            resumeText.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.fadein))
            resumeText.alpha = 1.0f
            endText.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.fadein))
            endText.alpha = 1.0f
        }
        startAnimation()
        GlobalScope.launch { resumeGame() }
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
            resumeBubble.viewTreeObserver.addOnGlobalLayoutListener(object :
                OnGlobalLayoutListener {
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
    }

    fun resumeAnimation() {
        selectionUtils.stopLeaves()
        GlobalScope.launch {
            pauseTextReposition()
        }
        activity.runOnUiThread {
            whiteBox.alpha = 0.0f
            resumeBubble.startAnimation(
                AnimationUtils.loadAnimation(
                    activity,
                    R.anim.fadeout_quick
                )
            )
            resumeBubble.alpha = 0.0f
            endBubble.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.fadeout_quick))
            endBubble.alpha = 0.0f
            resumeText.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.fadeout_quick))
            resumeText.alpha = 0.0f
            endText.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.fadeout_quick))
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