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
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothConnection.LocalBinder
            mService = binder.getService()
            mBound = true

            //TODO: move this out of here
            //val calibratedValues = calibrateBreathing()
            //Log.i("calibrated to:", "$calibratedValues")
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
        bg2.x = bg1.x + bg1.width
        bg2.y = 0f
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

        with(backgroundAnimator) {
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
        thread(start = true, isDaemon = true) {
            while (true) {
                val smoothedPosition = smoothPlayerPosition()
                //Log.i("smoothedPosition:", "$smoothedPosition")
                movePlayer(smoothedPosition.toFloat())

            }
        }
    }

    private fun smoothPlayerPosition(): Double {
        var bufferAbdo: ArrayList<Double> = ArrayList()
        var bufferThor: ArrayList<Double> = ArrayList()

        while (bufferAbdo.size <= 4 || bufferThor.size <= 6) {
            if (bufferAbdo.isEmpty() || !bufferAbdo[bufferAbdo.size - 1].equals(mService.mAbdoCorrected)) {
                bufferAbdo.add(mService.mAbdoCorrected)
            }
            if (bufferThor.isEmpty() || bufferThor[bufferThor.size - 1] != mService.mThorCorrected) {
                bufferThor.add(mService.mThorCorrected)
            }
        }
        val medianAbdo = mService.smoothData(bufferAbdo)
        val medianThor = mService.smoothData(bufferThor)
        Log.i("medianAbdo", "$medianAbdo")
        Log.i("bufferAbdo", "$bufferAbdo")
        Log.i("medianThor", "$medianThor")
        Log.i("bufferThor", "$bufferThor")


        bufferThor.clear()
        bufferAbdo.clear()
        val combinedBuffer = (((medianThor * 0.6) + (medianAbdo * 0.4)) * 300)
        val steps = (1000.0 / 300.0)
        return combinedBuffer / steps + 300.0
    }

    private fun movePlayer(calculate: Float) {
        val posPlayer = player.y
        runOnUiThread {
            ObjectAnimator.ofFloat(player, "translationY", posPlayer, calculate)
                .apply {
                    duration = 0
                    start()
                }
        }
    }

    //TODO: muss doch smarter gehen
    //TODO: als Coroutine dann kann sich der screen schön bewegen dazwischen
    private fun calibrateBreathing(): Pair<Pair<Double, Double>, Pair<Double, Double>> {

        var minimaAbdo: ArrayList<Double> = ArrayList()
        var maximaAbdo: ArrayList<Double> = ArrayList()
        var minimaThor: ArrayList<Double> = ArrayList()
        var maximaThor: ArrayList<Double> = ArrayList()
        //TODO: lokal mit k?!?!
        var lokalMinimaAbdo = 0.0
        var lokalMaximaAbdo = 0.0
        var lokalMinimaThor = 0.0
        var lokalMaximaThor = 0.0


        Toast.makeText(
            applicationContext,
            "Calibration started, Inhale/Exhale 5 times",
            Toast.LENGTH_LONG
        )



        repeat(4) {
            while (mService.mExpiration == 0) {
                if (!lokalMinimaAbdo.equals(0.0) && !lokalMinimaThor.equals(0.0)) {
                    minimaAbdo.add(lokalMinimaAbdo)
                    minimaThor.add(lokalMinimaThor)

                    lokalMinimaAbdo = 0.0
                    lokalMinimaThor = 0.0
                }
                if (mService.mThorCorrected > lokalMaximaThor) {
                    lokalMaximaThor = mService.mThorCorrected

                }
                if (mService.mAbdoCorrected > lokalMinimaAbdo) {
                    lokalMaximaAbdo = mService.mAbdoCorrected
                }
            }

            while (mService.mInspiration == 0) {
                if (!lokalMaximaAbdo.equals(0.0) && !lokalMaximaThor.equals(0.0)) {
                    maximaAbdo.add(lokalMaximaAbdo)
                    maximaThor.add(lokalMaximaThor)

                    lokalMaximaAbdo = 0.0
                    lokalMaximaThor = 0.0
                }
                if (mService.mThorCorrected < lokalMinimaThor) {
                    lokalMinimaThor = mService.mThorCorrected
                }
                if (mService.mAbdoCorrected < lokalMinimaAbdo) {
                    lokalMinimaAbdo = mService.mAbdoCorrected
                }
            }
        }

        Log.i("arrayAbdoMax:", "$maximaAbdo")

        return Pair(
            Pair(mService.calculateMedian(maximaAbdo), mService.calculateMedian(maximaThor)),
            Pair(mService.calculateMedian(minimaAbdo), mService.calculateMedian(minimaThor))
        )

    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(applicationContext, BluetoothConnection::class.java))
        exitProcess(0)
    }
}
