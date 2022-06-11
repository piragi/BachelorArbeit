package com.example.breathingmeditationandroid.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.breathingmeditationandroid.BluetoothConnection
import com.example.breathingmeditationandroid.R
import com.example.breathingmeditationandroid.gestures.HoldBreathGesture
import com.example.breathingmeditationandroid.utils.BreathingUtils
import com.example.breathingmeditationandroid.utils.SelectionUtils
import kotlinx.coroutines.Deferred
import kotlin.concurrent.thread

class HomeScreenActivity : ComponentActivity() {

    private lateinit var serviceIntent: Intent
    private lateinit var mService: BluetoothConnection
    private lateinit var breathingUtils: BreathingUtils

    private lateinit var bubble1: Pair<ImageView, Pair<Int, Int>>
    private lateinit var bubble2: Pair<ImageView, Pair<Int, Int>>
    private lateinit var bubble3: Pair<ImageView, Pair<Int, Int>>

    private lateinit var bubbles: Array<Pair<ImageView, Pair<Int, Int>>>

    private lateinit var breathHoldDetected: Deferred<Boolean>

    private lateinit var holdBreathGesture: HoldBreathGesture
    private lateinit var selectionUtils: SelectionUtils

    private var bubble1Selected = false
    private var bubble2Selected = false
    private var bubble3Selected = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothConnection.LocalBinder
            mService = binder.getService()
            breathingUtils = BreathingUtils(mService)
            holdBreathGesture = HoldBreathGesture(mService, 5000.0)
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

        bubble1 = Pair(findViewById(R.id.bubble1), Pair(0, 0))
        bubble2 = Pair(findViewById(R.id.bubble2), Pair(0, 0))
        bubble3 = Pair(findViewById(R.id.bubble3), Pair(0, 0))

        Log.i("init", "create")
    }

    override fun onStart() {
        super.onStart()
        Log.i("init", "start")
        serviceIntent = intent?.extras?.getParcelable("Intent")!!
        applicationContext.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(serviceIntent)
    }

    private fun start() {
        initializeBubbles()
        selectionUtils = SelectionUtils(this@HomeScreenActivity, breathingUtils, holdBreathGesture, bubbles)
        animateLeaves()
        detectScreenChange()
    }

    private fun initializeBubbles() {
        bubbles = arrayOf(
            Pair(bubble1.first, Pair(bubble1.first.left, bubble1.first.right)),
            Pair(bubble2.first, Pair(bubble2.first.left, bubble2.first.right)),
            Pair(bubble3.first, Pair(bubble3.first.left, bubble3.first.right))
        )
    }

    private fun animateLeaves() {
        thread(start = true, isDaemon = true) {
            while (!holdBreathGesture.hold) {
                selectionUtils.animateLeavesDiagonal()
                breathingUtils.smoothValue()
                Thread.sleep(2)
            }
        }
    }

    private fun stopActivity() {
        holdBreathGesture.stopDetection()
        selectionUtils.stopLeaves()
    }


    //TODO bugfix in screen change
    private fun detectScreenChange() {
        thread(start = true, isDaemon = true) {
            holdBreathGesture.detect()
            while (!holdBreathGesture.hold)
                continue
            if (bubble1.first.tag == "selected" && !(bubble2.first.tag == "selected" || bubble3.first.tag == "selected"))
                changeToAboutScreen()
            else if ((bubble2.first.tag == "selected" && !(bubble1.first.tag == "selected" || bubble3.first.tag == "selected")))
                changeToCalibrationScreen()
            else if (bubble3.first.tag == "selected" && !(bubble1.first.tag == "selected" || bubble2.first.tag == "selected"))
                changeToGameScreen()
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
    }

    private fun changeToGameScreen() {
        stopActivity()
        bubble3Selected = false
        Intent(this, GameScreen::class.java).also { intent ->
            intent.putExtra("Intent", serviceIntent)
            startActivity(intent)
        }
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
    }
}