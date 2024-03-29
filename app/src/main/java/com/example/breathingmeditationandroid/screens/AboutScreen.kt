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
import com.example.breathingmeditationandroid.utils.BreathingUtils
import com.example.breathingmeditationandroid.utils.ScreenUtils
import com.example.breathingmeditationandroid.utils.SelectionUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

class AboutScreen : ComponentActivity() {
    private lateinit var serviceIntent: Intent
    private lateinit var mService: BluetoothConnection
    private lateinit var breathingUtils: BreathingUtils
    private lateinit var cloud: ImageView
    private lateinit var selectionUtils: SelectionUtils
    private lateinit var clouds: ArrayList<Pair<ImageView, Pair<Int, Int>>>

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
        setContentView(R.layout.about_screen)
    }

    private fun getBubbleCoordinates() {
        cloud = findViewById(R.id.cloud)
        clouds = arrayListOf(Pair(cloud, Pair(ScreenUtils.cloud.first, ScreenUtils.cloud.second)))
        /* Log.i("bubbles", "${cloud.left}, ${cloud.right}")
        if (cloud.left == 0 && cloud.right == 0) {
            clouds = arrayListOf()
            cloud.viewTreeObserver.addOnGlobalLayoutListener {
                val left: Int = cloud.left
                val right: Int = cloud.right
                clouds.add(Pair(cloud, Pair(left, right)))
            }
        } else clouds = arrayListOf(Pair(cloud, Pair(cloud.left, cloud.right))) */
    }

    private fun start() {
        getBubbleCoordinates()
        selectionUtils = SelectionUtils(this@AboutScreen, breathingUtils, clouds)
        Thread.sleep(1000)
        detectScreenChange()
        startAnimation()
    }

    private fun startAnimation() {
        thread(start = true, isDaemon = true) {
            while (true) {
                try {
                    selectionUtils.animateLeavesHorizontal()
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
                val startTime = System.currentTimeMillis()
                while (cloud.tag == "selected" && !screenChanged)
                    if (System.currentTimeMillis().minus(startTime) >= 3500) {
                        screenChanged = true
                        stopActivity()
                        changeScreen()
                    }
            }
        }
    }

    private fun stopActivity() {
        selectionUtils.stopLeaves()
    }

    private fun changeScreen() {
        Intent(this, HomeScreenActivity::class.java).also { intent ->
            intent.putExtra("Intent", serviceIntent)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_up_top, R.anim.slide_up_bottom)
            finish()
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