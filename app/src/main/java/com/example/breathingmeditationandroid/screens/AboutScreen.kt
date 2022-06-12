package com.example.breathingmeditationandroid.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.ViewTreeObserver
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.breathingmeditationandroid.BluetoothConnection
import com.example.breathingmeditationandroid.R
import com.example.breathingmeditationandroid.gestures.HoldBreathGesture
import com.example.breathingmeditationandroid.utils.BreathingUtils
import com.example.breathingmeditationandroid.utils.SelectionUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

class AboutScreen : ComponentActivity() {
    private lateinit var serviceIntent: Intent
    private lateinit var mService: BluetoothConnection
    private lateinit var breathingUtils: BreathingUtils
    private lateinit var holdBreathGesture: HoldBreathGesture
    private lateinit var cloud: ImageView
    private lateinit var selectionUtils: SelectionUtils
    private lateinit var clouds: ArrayList<Pair<ImageView, Pair<Int, Int>>>

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothConnection.LocalBinder
            mService = binder.getService()
            holdBreathGesture = HoldBreathGesture(mService, 5000.0)
            breathingUtils = BreathingUtils(mService)
            holdBreathGesture.detect()
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
    }

    //TODO not detecting correctly sometimes
    private fun getBubbleCoordinates() {
        if (cloud.left == 0 && cloud.right == 0) {
            Log.i("bubbles", "only zero values")
            clouds = arrayListOf()
            cloud.viewTreeObserver.addOnGlobalLayoutListener {
                val left: Int = cloud.left
                val right: Int = cloud.right
                Log.i("bubbles", "$left, $right")
                clouds.add(Pair(cloud, Pair(left, right)))
            }
        } else clouds = arrayListOf(Pair(cloud, Pair(cloud.left, cloud.right)))
    }

    private fun animateLeaves() {
        getBubbleCoordinates()
        if (this::clouds.isInitialized) {
            Log.i("bubbles", "initialized")
            selectionUtils = SelectionUtils(
                this@AboutScreen,
                breathingUtils,
                holdBreathGesture,
                clouds
            )
            selectionUtils.resumeLeaves()
            thread(start = true, isDaemon = true) {
                while (!holdBreathGesture.hold) {
                    selectionUtils.animateLeavesHorizontal()
                    Thread.sleep(2)
                }
            }
        }
    }

    private fun returnToHomeScreen() {
        thread(start = true, isDaemon = true) {
            while (!holdBreathGesture.hold)
                continue
            Intent(this, HomeScreenActivity::class.java).also { intent ->
                intent.putExtra("Intent", serviceIntent)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_up_top, R.anim.slide_up_bottom)
                finish()
            }
        }
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