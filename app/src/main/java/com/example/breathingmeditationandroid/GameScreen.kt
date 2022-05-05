package com.example.breathingmeditationandroid

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
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

class GameScreen : ComponentActivity() {

    //Bluetooth Connection
    private var mDevice: BluetoothDevice? = null

    //Binding service
    private lateinit var mService: BluetoothConnection
    private var mBound = false
    private lateinit var breathingUtils: BreathingUtils

    private lateinit var snow: ImageView

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothConnection.LocalBinder
            mService = binder.getService()
            mBound = true


            breathingUtils = BreathingUtils(mService)
            breathingUtils.calibrateBreathing()
            startLevel()

        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService.stopService(intent)
            mBound = false
            return
        }
    }

    //View starten
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //view
        setContentView(R.layout.game_screen)
        snow = findViewById<View>(R.id.snow) as ImageView

        deepBreathAnimation()

        //setup and start bluetooth service
        mDevice = intent?.extras?.getParcelable("Device")

        Intent(applicationContext, BluetoothConnection::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
            intent.putExtra("Device", mDevice)
            startService(intent)
        }
    }

    fun startLevel() {
        thread(start = true, isDaemon = true) {
            lifecycleScope.launch {
                breathingUtils.deepBreathDetected()
                deepBreathAnimation()
            }
        }
    }

    private fun deepBreathAnimation() {
        val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fadeout)
        Log.i("we got here", "nice")
        snow.startAnimation(fadeOut)

        //on Animation End make it invisible
    }


}