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
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.animation.doOnEnd
import kotlin.concurrent.thread

class TestView : ComponentActivity() {

    //View
    private lateinit var textSteps: TextView
    private lateinit var textAbdominalCorrected: TextView
    private lateinit var textThorasicRaw: TextView
    private lateinit var player: ImageView
    private lateinit var bg1: ImageView
    private lateinit var bg2: ImageView


    //Bluetooth Connection
    private var mDevice: BluetoothDevice? = null

    //Binding service
    private lateinit var mService: BluetoothConnection
    private var mBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothConnection.LocalBinder
            mService = binder.getService()
            mBound = true

            updateView()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            return
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //setup View
        setContentView(R.layout.activity_device)
        player = findViewById<View>(R.id.player) as ImageView
        bg1 = findViewById<View>(R.id.bg1) as ImageView
        bg2 = findViewById<View>(R.id.bg2) as ImageView
        bg2.x = 1920f

        player.x = 800f
        player.y = 300f



        //setup and start bluetooth service
        mDevice = intent?.extras?.getParcelable("Device")

        Intent(applicationContext, BluetoothConnection::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
            intent.putExtra("Device", mDevice)
            startService(intent)
        }
    }

    fun updateView() {
        runOnUiThread() {
            var endBg1 = -1920f
            var endBg2 = 0f
            val bg1Animation = ObjectAnimator.ofFloat(bg1, "translationX", bg1.x, endBg1).apply {
                duration=2000
                start()
            }
            val bg2Animation = ObjectAnimator.ofFloat(bg2, "translationX", bg2.x, endBg2).apply {
                duration=2000
                start()
            }
            bg2Animation.doOnEnd {
                bg1.x = 1920f
                endBg1 = 0f
                bg1Animation.start()
                endBg2 = -1920f
                bg2Animation.start()
            }
        }

        thread(start = true, isDaemon = true) {

            while (true) {
            val offset = (((mService.mAbdoCorrected)+2)*100).toFloat()
            val steps:Float = (200f/300f)
            val calculate = offset/steps + 200f

            /*Log.i("calculate", "$calculate")
            Log.i("offset", "$offset")
            Log.i("steps", "$steps")
            Log.i("player", "${player.y}")*/
            val posPlayer = player.y
                    runOnUiThread {

                        with(mService) {
                                ObjectAnimator.ofFloat(player, "translationY", posPlayer, calculate).apply {
                                    duration = 0
                                    start()
                                }
                        }
                    }

                Thread.sleep(10)
            }
        }
    }

}