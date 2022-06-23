package com.example.breathingmeditationandroid.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.breathingmeditationandroid.BluetoothConnection
import com.example.breathingmeditationandroid.R
import com.example.breathingmeditationandroid.utils.BreathingUtils
import com.example.breathingmeditationandroid.utils.ScreenUtils
import com.example.breathingmeditationandroid.utils.SelectionUtils
import kotlinx.coroutines.*
import kotlin.concurrent.thread
import java.lang.System.currentTimeMillis

class GamePauseScreen : ComponentActivity() {
    private lateinit var serviceIntent: Intent
    private lateinit var mService: BluetoothConnection
    private lateinit var breathingUtils: BreathingUtils
    private lateinit var selectionUtils: SelectionUtils
    private lateinit var bubbles: ArrayList<Pair<ImageView, Pair<Int, Int>>>
    private lateinit var endBubble: ImageView
    private lateinit var resumeBubble: ImageView

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothConnection.LocalBinder
            mService = binder.getService()
            breathingUtils = BreathingUtils(mService)
            start()
            lifecycleScope.launch {
                pauseTextDisplay()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService.stopService(intent)
            return
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.game_pause_screen)

        serviceIntent = intent?.extras?.getParcelable("Intent")!!
        applicationContext.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun start() {
        endBubble = findViewById(R.id.endBubble)
        resumeBubble = findViewById(R.id.resumeBubble)
        bubbles = arrayListOf(
            Pair(
                endBubble,
                Pair(ScreenUtils.endBubble.first, ScreenUtils.endBubble.second)
            ),
            Pair(
                resumeBubble,
                Pair(ScreenUtils.resumeBubble.first, ScreenUtils.resumeBubble.second)
            )
        )
        selectionUtils =
            SelectionUtils(
                this@GamePauseScreen,
                breathingUtils,
                bubbles
            )
        Thread.sleep(1000)
        animateLeaves()
        detectScreenChange()
    }

    private fun initializeBubbles() {
        if (endBubble.left == 0 || endBubble.right == 0 || resumeBubble.left == 0 || resumeBubble.right == 0) {
            endBubble.viewTreeObserver.addOnGlobalLayoutListener {
                val left: Int = endBubble.left
                val right: Int = endBubble.right
                if (this::bubbles.isInitialized)
                    bubbles.add(Pair(endBubble, Pair(left, right)))
                else bubbles = arrayListOf(Pair(endBubble, Pair(left, right)))
            }
            resumeBubble.viewTreeObserver.addOnGlobalLayoutListener {
                val left: Int = resumeBubble.left
                val right: Int = resumeBubble.right
                if (this::bubbles.isInitialized)
                    bubbles.add(Pair(resumeBubble, Pair(left, right)))
                else bubbles = arrayListOf(Pair(resumeBubble, Pair(left, right)))
            }
        } else bubbles = arrayListOf(
            Pair(endBubble, Pair(endBubble.left, endBubble.right)),
            Pair(resumeBubble, Pair(resumeBubble.left, resumeBubble.right))
        )
    }

    private fun animateLeaves() {
        thread(start = true, isDaemon = true) {
            while (true) {
                try {
                    selectionUtils.animateLeavesDiagonal()
                    Thread.sleep(2)
                } catch (error: ConcurrentModificationException) {
                    continue
                }
            }
        }
    }

    private fun detectScreenChange() {
        var screenChanged = false
        thread(start = true, isDaemon = true) {
            while (!screenChanged) {
                val startTime = currentTimeMillis()
                while (endBubble.tag == "selected" && resumeBubble.tag != "selected" && !screenChanged)
                    if (currentTimeMillis().minus(startTime) >= 3500) {
                        screenChanged = true
                        stopActivity()
                        changeToHomeScreen()
                    }
                while (endBubble.tag != "selected" && resumeBubble.tag == "selected" && !screenChanged)
                    if (currentTimeMillis().minus(startTime) >= 3500) {
                        screenChanged = true
                        stopActivity()
                        changeToGameScreen()
                    }
            }
        }
    }

    private fun stopActivity() {
        selectionUtils.stopLeaves()
    }

    private fun changeToHomeScreen() {
        Intent(this, HomeScreenActivity::class.java).also { intent ->
            intent.putExtra("Intent", serviceIntent)
            startActivity(intent)
            overridePendingTransition(R.anim.fadein_fast_full, R.anim.fadeout_fast_full)
        }
        finish()
    }

    private fun changeToGameScreen() {
        Intent(this, GameScreen::class.java).also { intent ->
            intent.putExtra("Intent", serviceIntent)
            startActivity(intent)
            overridePendingTransition(R.anim.fadein_fast_full, R.anim.fadeout_fast_full)
        }
        finish()
    }

    private suspend fun pauseTextDisplay() = coroutineScope {
        val text = findViewById<TextView>(R.id.pauseText)
        runOnUiThread {
            text.text = "Game paused"
            text.animate()
                .alpha(1.0f)
                .y(50f)
                .setDuration(1000)
                .setListener(null)
        }
        delay(1000)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(serviceIntent)
    }
}