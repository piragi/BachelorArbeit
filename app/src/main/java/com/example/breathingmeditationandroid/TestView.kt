package com.example.breathingmeditationandroid

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import androidx.activity.ComponentActivity
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class TestView : ComponentActivity() {

    //View
    private lateinit var player: ImageView
    private lateinit var bg1: ImageView
    private lateinit var bg2: ImageView

    //Bluetooth Connection
    private var mDevice: BluetoothDevice? = null

    //Binding service
    private lateinit var mService: BluetoothConnection
    private var mBound = false
    private lateinit var breathingUtils: BreathingUtils

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothConnection.LocalBinder
            mService = binder.getService()
            mBound = true

            //TODO: move this out of here
            breathingUtils = BreathingUtils(mService)
            val calibratedValues = breathingUtils.calibrateBreathing()
            Log.i("calibrated to:", "$calibratedValues")
            animatePlayer(calibratedValues)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService.stopService(intent)
            mBound = false
            return
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //setup view
        setContentView(R.layout.activity_device)
        player = findViewById<View>(R.id.player) as ImageView
        bg1 = findViewById<View>(R.id.bg1) as ImageView
        bg2 = findViewById<View>(R.id.bg2) as ImageView
        bg2.x = bg1.x + bg1.width
        bg2.y = 0f
        player.x = 650f
        player.y = 520f

        animateBackground()

        //setup and start bluetooth service
        mDevice = intent?.extras?.getParcelable("Device")

        Intent(applicationContext, BluetoothConnection::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
            intent.putExtra("Device", mDevice)
            startService(intent)
        }
    }

    private fun animateBackground() {
        runOnUiThread() {
            val backgroundAnimator = ValueAnimator.ofFloat(0.0f, 1.0f)

            with(backgroundAnimator) {
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                duration = 2000L
                addUpdateListener {
                    val progress = this.animatedValue as Float
                    val width = bg1.width
                    val translationX = width * (1-progress)
                    bg1.translationX = translationX
                    bg2.translationX = translationX - width
                }
                start()
            }
        }
    }

    fun animatePlayer(calibratedValue: Pair<Pair<Double,Double>, Pair<Double,Double>>) {
        var playerAnimator = ObjectAnimator()
        thread(start = true, isDaemon = true) {
            while (true) {
                val smoothedPosition = breathingUtils.smoothPlayerPosition()
                val relativePosition = breathingUtils.calculateRelativePosition(calibratedValue, smoothedPosition)
                if (playerAnimator.isRunning) {
                    runOnUiThread() {
                        playerAnimator.cancel()
                    }
                }
                playerAnimator = movePlayer(relativePosition.toFloat())
            }
        }
    }

    private fun movePlayer( calculate: Float) : ObjectAnimator {
        val playerAnimator = ObjectAnimator.ofFloat(player, "translationY", player.y, calculate)
            .apply {
                interpolator = LinearInterpolator()
                duration = 4
                addUpdateListener {
                    player.y = animatedValue as Float
                }
            }

        runOnUiThread {
            playerAnimator.start()
        }
        return playerAnimator
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(applicationContext, BluetoothConnection::class.java))
        exitProcess(0)
    }
}
