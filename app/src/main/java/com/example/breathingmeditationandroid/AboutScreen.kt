package com.example.breathingmeditationandroid

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.ImageView
import androidx.activity.ComponentActivity
import com.example.breathingmeditationandroid.gestures.HoldBreathGesture
import kotlin.concurrent.thread

class AboutScreen : ComponentActivity() {
    private lateinit var serviceIntent: Intent
    private lateinit var mService: BluetoothConnection
    private lateinit var breathingUtils: BreathingUtils
    private lateinit var holdBreathGesture: HoldBreathGesture
    private lateinit var cloud: ImageView
    private lateinit var selectionUtils: SelectionUtils

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothConnection.LocalBinder
            mService = binder.getService()
            holdBreathGesture = HoldBreathGesture(mService, 5000.0)
            breathingUtils = BreathingUtils(mService)
            animateLeaves()
            returnToHomeScreen()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService.stopService(intent)
            return
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.about_screen)
        cloud = findViewById(R.id.cloud)

        serviceIntent = intent?.extras?.getParcelable("Intent")!!
        applicationContext.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun animateLeaves() {
        selectionUtils = SelectionUtils(
            this@AboutScreen,
            breathingUtils,
            holdBreathGesture,
            arrayOf(Pair(cloud, Pair(cloud.left, cloud.right)))
        )
        thread(start = true, isDaemon = true) {
            while (!holdBreathGesture.hold) {
                selectionUtils.animateLeaves()
                Thread.sleep(2)
            }
        }
    }

    private fun returnToHomeScreen() {
        thread(start = true, isDaemon = true) {
            while (!holdBreathGesture.hold)
                continue
        }
        Intent(this, HomeScreenActivity::class.java).also { intent ->
            intent.putExtra("Intent", serviceIntent)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_up_top, R.anim.slide_up_bottom)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(serviceIntent)
    }
}