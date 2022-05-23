package com.example.breathingmeditationandroid

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.Image
import android.os.*
import android.provider.Telephony.Mms.Part
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.breathingmeditationandroid.gestures.HoldBreathGesture
import com.plattysoft.leonids.ParticleSystem
import kotlinx.coroutines.*
import java.lang.System.currentTimeMillis
import kotlin.concurrent.thread

class CalibrationScreenActivity : ComponentActivity() {

    private lateinit var container: ViewGroup
    private var mDevice: BluetoothDevice? = null
    private lateinit var mService: BluetoothConnection
    private var mBound = false
    private lateinit var greySky: ImageView
    private lateinit var clouds: ImageView
    private lateinit var background: ImageView
    private lateinit var newBackground: ImageView
    private var isSnowing = true
    private lateinit var breathingUtils: BreathingUtils
    private var calibrationDetected = false
    private var fadeOutSky = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothConnection.LocalBinder
            mService = binder.getService()
            breathingUtils = BreathingUtils(mService)
            mBound = true
            lifecycleScope.launch {
                handleCalibration()
            }
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            TODO("Not yet implemented")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.calibration_screen)
        greySky = findViewById(R.id.grey_sky)
        clouds = findViewById(R.id.clouds)
        background = findViewById(R.id.calibration_background)
        newBackground = findViewById(R.id.home_screen_background_switch)
        mDevice = intent?.extras?.getParcelable("Device")
        snow()
        Intent(applicationContext, BluetoothConnection::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
            intent.putExtra("Device", mDevice)
            startService(intent)
        }
    }

    private suspend fun handleCalibration() = coroutineScope {
        launch {
            breathingUtils.detectFiveSecondInspiration()
        }.join()
        breathingUtils.startFromBeginning()
        //TODO display text "breathe in for 5 sec" + timer
        var iteration = 0
        Log.i("calibration", "calibration starting")
        calibrationDetected = true
        fadeOutGreySky()
        repeat(2) {
            iteration++
            launch {
                Calibrator.calibrateFlow(mService)
            }
            launch {
                delay(5000)
                Calibrator.calibrateBreathHold(5000, "in")
            }.join()
            launch {
                Calibrator.calibrateBreathHold(5000, "out")
                fadeOutSky = iteration == 2
            }.join()
        }
    }

    private fun decreaseSnowFlow(vararg particleSystems: ParticleSystem, factor: Int): Boolean {
        if (factor <= 0)
            return false
        for (ps in particleSystems) {
            ps.setFadeOut(factor.toLong())
        }
        return true
    }

    private fun snow() {
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
        var iteration = 0
        while (true) {
            if (calibrationDetected) {
                isSnowing = decreaseSnowFlow(ps1, ps2, ps3, ps4, factor = 10000.minus(iteration))

            }
            if (!isSnowing) {
                ps1.stopEmitting()
                ps2.stopEmitting()
                ps3.stopEmitting()
                ps4.stopEmitting()
                Thread.sleep(5000)
                fadeOutGreySky()
                break
            }
            iteration++
        }
    }

    private fun fadeOutGreySky() {
        while (!fadeOutSky)
            continue
        runOnUiThread {
            val animation = AnimationUtils.loadAnimation(this, R.anim.fadeout)
            greySky.startAnimation(animation)
            clouds.startAnimation(animation)
            Handler(Looper.getMainLooper()).postDelayed({
                greySky.visibility = View.GONE
                clouds.visibility = View.GONE
            }, 0)
            // animateBackground()
        }
    }

    private fun animateBackground() {
        Intent(this, HomeScreenActivity::class.java).also { intent ->
            startService(intent)
            overridePendingTransition(R.anim.slide_up_top, R.anim.slide_up_bottom)

        }
    }


}