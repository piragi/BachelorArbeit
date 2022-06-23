package com.example.breathingmeditationandroid.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.example.breathingmeditationandroid.BluetoothConnection
import com.example.breathingmeditationandroid.R
import com.example.breathingmeditationandroid.gestures.HoldBreathGesture
import com.example.breathingmeditationandroid.utils.BreathingUtils
import com.example.breathingmeditationandroid.utils.ScreenUtils
import com.example.breathingmeditationandroid.utils.SelectionUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlin.concurrent.thread


class HomeScreenActivity : ComponentActivity() {

    private lateinit var serviceIntent: Intent
    private lateinit var mService: BluetoothConnection
    private lateinit var breathingUtils: BreathingUtils

    private lateinit var bubble1: ImageView
    private lateinit var bubble2: ImageView
    private lateinit var bubble3: ImageView

    private lateinit var bubbles: ArrayList<Pair<ImageView, Pair<Int, Int>>>

    private lateinit var view: ConstraintLayout
    private lateinit var selectionUtils: SelectionUtils

    private var bubble1Selected = false
    private var bubble2Selected = false
    private var bubble3Selected = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothConnection.LocalBinder
            mService = binder.getService()
            breathingUtils = BreathingUtils(mService)
            start()
            Log.i("init", "service connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService.stopService(intent)
            return
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.home_screen)
        view = findViewById(R.id.home_screen)

        Log.i("init", "create")
        serviceIntent = intent?.extras?.getParcelable("Intent")!!
        applicationContext.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(serviceIntent)
    }

    private fun start() {
        initializeBubbles()
        animateBubbles()
        for (bubble in bubbles) {
            Log.i("bubbles", "${bubble.second}")
        }
        selectionUtils =
            SelectionUtils(
                this@HomeScreenActivity,
                breathingUtils,
                bubbles
            )
        Thread.sleep(1000)
        animateLeaves()
        detectScreenChange()
    }

    private fun animateBubbles() {
        runOnUiThread {
            bubble1.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fadein))
            bubble2.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fadein))
            bubble3.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fadein))
        }
    }

    private fun initializeBubbles() {
        bubble1 = findViewById(R.id.bubble1)
        bubble2 = findViewById(R.id.bubble2)
        bubble3 = findViewById(R.id.bubble3)

        bubbles = arrayListOf(
            Pair(
                bubble1,
                Pair(ScreenUtils.aboutScreenBubble.first, ScreenUtils.aboutScreenBubble.second)
            ),
            Pair(
                bubble2,
                Pair(ScreenUtils.calibrationBubble.first, ScreenUtils.calibrationBubble.second)
            ),
            Pair(bubble3, Pair(ScreenUtils.playBubble.first, ScreenUtils.playBubble.second))

        )
        /* if (bubble1.left == 0 || bubble1.right == 0 || bubble2.left == 0 || bubble2.right == 0 || bubble3.left == 0 || bubble3.right == 0) {
            bubbles = arrayListOf()
            bubble1.viewTreeObserver.addOnGlobalLayoutListener {
                val left: Int = bubble1.left
                val right: Int = bubble1.right
                bubbles.add(Pair(bubble1, Pair(left, right)))
            }
            bubble2.viewTreeObserver.addOnGlobalLayoutListener {
                val left: Int = bubble2.left
                val right: Int = bubble2.right
                bubbles.add(Pair(bubble2, Pair(left, right)))
            }
            bubble3.viewTreeObserver.addOnGlobalLayoutListener {
                val left: Int = bubble3.left
                val right: Int = bubble3.right
                bubbles.add(Pair(bubble3, Pair(left, right)))
            }
        } else bubbles = arrayListOf(
            Pair(bubble1, Pair(bubble1.left, bubble1.right)),
            Pair(bubble2, Pair(bubble2.left, bubble2.right)),
            Pair(bubble3, Pair(bubble3.left, bubble3.right))
        ) */
    }

    private fun animateLeaves() {
        thread(start = true, isDaemon = true) {
            while (true) {
                try {
                    selectionUtils.animateLeavesDiagonal()
                    Thread.sleep(5)
                } catch (exception: ConcurrentModificationException) {
                    continue
                }
            }
        }
    }

    private fun stopActivity() {
        selectionUtils.stopLeaves()
    }

    private fun detectScreenChange() {
        GlobalScope.launch {
            val screenChangeDetected = selectionUtils.detectSafeStopAsync()
            if (screenChangeDetected.await()) {
                if (bubble1.tag == "selected" && !(bubble2.tag == "selected" || bubble3.tag == "selected"))
                    changeToAboutScreen()
                else if ((bubble2.tag == "selected" && !(bubble1.tag == "selected" || bubble3.tag == "selected")))
                    changeToCalibrationScreen()
                else if (bubble3.tag == "selected" && !(bubble1.tag == "selected" || bubble2.tag == "selected"))
                    changeToGameScreen()
            }
        }
    }

    private fun changeToAboutScreen() {
        stopActivity()
        bubble1Selected = false
        Intent(this, AboutScreen::class.java).also { intent ->
            intent.putExtra("Intent", serviceIntent)
            startActivity(intent)
            overridePendingTransition(
                R.anim.slide_down_top, R.anim.slide_down_bottom
            )
        }
        finish()
    }

    private fun changeToGameScreen() {
        stopActivity()
        bubble3Selected = false
        Intent(this, GameScreen::class.java).also { intent ->
            intent.putExtra("Intent", serviceIntent)
            startActivity(intent)
            overridePendingTransition(R.anim.fadein_fast_full, R.anim.fadeout_fast_full)
        }
        finish()
    }

    private fun changeToCalibrationScreen() {
        stopActivity()
        bubble2Selected = false
        Intent(this, CalibrationScreenActivity::class.java).also { intent ->
            intent.putExtra("Intent", serviceIntent)
            startActivity(intent)
            overridePendingTransition(
                R.anim.slide_down_top, R.anim.slide_down_bottom
            )
        }
        finish()
    }
}