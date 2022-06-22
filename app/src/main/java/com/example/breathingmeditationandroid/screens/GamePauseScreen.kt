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
import com.example.breathingmeditationandroid.utils.SelectionUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

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
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService.stopService(intent)
            return
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.game_pause_screen)
        endBubble = findViewById(R.id.endBubble)
        resumeBubble = findViewById(R.id.resumeBubble)
        lifecycleScope.launch {
            pauseTextDisplay()
        }
    }

    private fun start() {
        initializeBubbles()
        if (this::bubbles.isInitialized) {
            for (bubble in bubbles) {
                Log.i("bubbles", "${bubble.second}")
            }
            lifecycleScope.launch {
                selectionUtils =
                    SelectionUtils(
                        this@GamePauseScreen,
                        breathingUtils,
                        bubbles
                    )
                animateLeaves()
                detectScreenChange()
            }
        }
    }

    private fun initializeBubbles() {
        if (endBubble.left == 0 || endBubble.right == 0 || resumeBubble.left == 0 || resumeBubble.right == 0) {
            bubbles = arrayListOf()
            endBubble.viewTreeObserver.addOnGlobalLayoutListener {
                val left: Int = endBubble.left
                val right: Int = endBubble.right
                bubbles.add(Pair(endBubble, Pair(left, right)))
            }
            resumeBubble.viewTreeObserver.addOnGlobalLayoutListener {
                val left: Int = resumeBubble.left
                val right: Int = resumeBubble.right
                bubbles.add(Pair(resumeBubble, Pair(left, right)))
            }
        } else bubbles = arrayListOf(
            Pair(endBubble, Pair(endBubble.left, endBubble.right)),
            Pair(resumeBubble, Pair(resumeBubble.left, resumeBubble.right))
        )
    }

    private fun animateLeaves() {
        if (this::bubbles.isInitialized) {
            selectionUtils = SelectionUtils(this@GamePauseScreen, breathingUtils, bubbles)
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
    }

    private fun detectScreenChange() {
        GlobalScope.launch {
            val screenChangeDetected = selectionUtils.detectSafeStopAsync()
            if (screenChangeDetected.await()) {
                if (endBubble.tag == "selected" && resumeBubble.tag != "selected")
                    changeToHomeScreen()
                else if ((endBubble.tag != "selected" && (resumeBubble.tag == "selected")))
                    changeToGameScreen()
            }
        }
    }

    private fun stopActivity() {
        selectionUtils.stopLeaves()
    }

    private fun changeToHomeScreen() {
        stopActivity()
        Intent(this, HomeScreenActivity::class.java).also { intent ->
            intent.putExtra("Intent", serviceIntent)
            startActivity(intent)
            overridePendingTransition(R.anim.fadein_fast_full, R.anim.fadeout_fast_full)
            finish()
        }
    }

    private fun changeToGameScreen() {
        stopActivity()
        Intent(this, GameScreen::class.java).also { intent ->
            intent.putExtra("Intent", serviceIntent)
            startActivity(intent)
            overridePendingTransition(R.anim.fadein_fast_full, R.anim.fadeout_fast_full)
            finish()
        }
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

    override fun onStart() {
        super.onStart()
        serviceIntent = intent?.extras?.getParcelable("Intent")!!
        applicationContext.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(serviceIntent)
    }
}