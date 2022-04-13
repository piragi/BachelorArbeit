package com.example.breathingmeditationandroid

import android.animation.ObjectAnimator
import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.core.animation.doOnEnd
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
        runOnUiThread {
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

                //if(mService.filteredAbdo > 0.0) {
                    val combined = (((mService.mAbdoCorrected)+2)*100).toFloat()
                    val steps:Float = (200f/300f)
                    val calculate = combined/steps + 100f

                    val posPlayer = player.y
                    runOnUiThread {

                        with(mService) {
                            ObjectAnimator.ofFloat(player, "translationY", posPlayer, calculate)
                                .apply {
                                    duration = 0
                                    start()
                                }
                        }
                    }
                //}
                Thread.sleep(1)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exitProcess(0)
    }

}