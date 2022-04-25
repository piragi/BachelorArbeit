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
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.activity.ComponentActivity
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

            animatePlayer()
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
        bg2.x = 1920f
        player.x = 800f
        player.y = 600f
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

        val backgroundAnimator = ValueAnimator.ofFloat(0.0f, 1.0f)

        with (backgroundAnimator) {
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            duration = 2000L
            addUpdateListener {
                    val progress = this.animatedValue as Float
                    val width = bg1.width
                    val translationX = width * progress
                    bg1.translationX = translationX
                    bg2.translationX = translationX - width
            }
            start()
        }
    }

    fun animatePlayer() {
        var bufferAbdo: ArrayList<Double> = ArrayList()
        var bufferThor: ArrayList<Double> = ArrayList()
        thread(start = true, isDaemon = true) {
            while (true) {

                bufferAbdo.add(mService.mAbdoCorrected)
                bufferThor.add(mService.mThorCorrected)

                if(bufferAbdo.size >= 7) {
                    //Log.i("buffer:", "${mService.bufferAbdo}")
                    val medianAbdo = mService.smoothData(bufferAbdo)
                    //Log.i("buffer", "${mService.bufferThor}")
                    val medianThor = mService.smoothData(bufferThor)

                    val combined = (((mService.mThorCorrected*0.8)+(mService.mAbdoCorrected*0.2))*100).toFloat()
                    val combinedBuffer = (((medianThor*0.8)+(medianAbdo*0.2))*200).toFloat()
                    val steps:Float = (1000f/300f)
                    val calculate = combinedBuffer/steps + 300f

                    val posPlayer = player.y
                    runOnUiThread {
                        ObjectAnimator.ofFloat(player, "translationY", posPlayer, calculate)
                            .apply {
                                duration = 0
                                start()
                            }
                    }
                    bufferThor.clear()
                    bufferAbdo.clear()
                }

                //Thread.sleep(20)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(applicationContext, BluetoothConnection::class.java))
        exitProcess(0)
    }

}
