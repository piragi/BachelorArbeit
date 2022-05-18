package com.example.breathingmeditationandroid

import android.animation.ObjectAnimator
import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.provider.Telephony.Mms.Part
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.activity.ComponentActivity
import com.example.breathingmeditationandroid.gestures.HoldBreathGesture
import com.plattysoft.leonids.ParticleSystem
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.System.currentTimeMillis
import kotlin.concurrent.thread

class CalibrationScreenActivity : ComponentActivity() {

    private lateinit var container: ViewGroup
    private var mDevice: BluetoothDevice? = null
    private lateinit var mService: BluetoothConnection
    private var mBound = false
    private lateinit var greySky: ImageView
    private var isSnowing = true
    private lateinit var breathingUtils: BreathingUtils

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothConnection.LocalBinder
            mService = binder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            TODO("Not yet implemented")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.calibration_screen)
        // greySky = findViewById(R.id.grey_sky)
        mDevice = intent?.extras?.getParcelable("Device")
        breathingUtils = BreathingUtils(mService)
        // val cloud1 = findViewById<ImageView>(R.id.cloud1)
        // moveCloudToLeft(cloud1)
        snow()
        Intent(applicationContext, BluetoothConnection::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
            intent.putExtra("Device", mDevice)
            startService(intent)
        }
    }

    private suspend fun handleCalibration() = coroutineScope {
        // breathe in deep for 5 seconds
        val startTime = currentTimeMillis()
        var breatheIn = false
        launch {
            breatheIn = breathingUtils.detectFiveSecondInspiration()
        }
        while (!breatheIn)
            continue
        // hold breath for 5 sec and calibrate
    }

    private fun snow() {
        thread(start = true, isDaemon = true) {
            val ps1: ParticleSystem = ParticleSystem(this, 80, R.drawable.white_dot, 10000)
            ps1.setSpeedModuleAndAngleRange(0f, 0.1f, 180, 180)
                .setRotationSpeed(144f)
                .setAcceleration(0.000017f, 90)
                .emit(2280, 0, 8)
            val ps2: ParticleSystem = ParticleSystem(this, 80, R.drawable.white_dot, 10000)
            ps2.setSpeedModuleAndAngleRange(0f, 0.1f, 0, 0)
                .setRotationSpeed(144f)
                .setAcceleration(0.000017f, 90)
                .emit(0, 0, 8)
            val ps3: ParticleSystem = ParticleSystem(this, 80, R.drawable.white_dot, 10000)
            ps3.setSpeedModuleAndAngleRange(0f, 0.1f, 0, 0)
                .setRotationSpeed(144f)
                .setAcceleration(0.000017f, 90)
                .emit(1140, 0, 4)
            val ps4: ParticleSystem = ParticleSystem(this, 80, R.drawable.white_dot, 10000)
            ps4.setSpeedModuleAndAngleRange(0f, 0.1f, 0, 0)
                .setRotationSpeed(144f)
                .setAcceleration(0.000017f, 90)
                .emit(1140, 0, 4)
            while (true) {
                if (!isSnowing) {
                    ps1.stopEmitting()
                    ps2.stopEmitting()
                    ps3.stopEmitting()
                    ps4.stopEmitting()
                }
            }
        }
    }

    private fun fadeOutGreySky() {
        val animation = AnimationUtils.loadAnimation(this, R.anim.fadeout)
        greySky.startAnimation(animation)
        Handler(Looper.getMainLooper()).postDelayed({
            greySky.visibility = View.GONE
        }, 2000)
    }

    private fun moveCloudToLeft(view: ImageView) {
        runOnUiThread {
            ObjectAnimator.ofFloat(view, "translationX", 1000f).apply {
                duration = 3000
                start()
            }
        }
    }


}