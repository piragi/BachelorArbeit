package com.example.breathingmeditationandroid

import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.plattysoft.leonids.ParticleSystem
import kotlinx.coroutines.*
import kotlin.concurrent.thread

class CalibrationScreenActivity : ComponentActivity() {
    private var mDevice: BluetoothDevice? = null
    private lateinit var mService: BluetoothConnection
    private var mBound = false
    private lateinit var greySky: ImageView
    private lateinit var clouds: ImageView
    private lateinit var background: ImageView
    private var isSnowing = true
    private lateinit var breathingUtils: BreathingUtils
    private var calibrationDetected = false
    private var fadeOutSky = false
    private lateinit var text: TextView

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothConnection.LocalBinder
            mService = binder.getService()
            Calibrator.initialize(mService)
            mBound = true
            breathingUtils = BreathingUtils(mService)
            lifecycleScope.launch {
                handleCalibration()
            }
            Log.i("test", "onServiceConnected done")
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            mService.stopService(intent)
            mBound = false
            return
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.calibration_screen)
        greySky = findViewById(R.id.grey_sky)
        clouds = findViewById(R.id.clouds)
        background = findViewById(R.id.calibration_background)
        text = findViewById(R.id.text)
        snow()
        text.text = getString(R.string.calibration_is_starting)
        mDevice = intent?.extras?.getParcelable("Device")
        Intent(applicationContext, BluetoothConnection::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
            intent.putExtra("Device", mDevice)
            startService(intent)
        }
    }

    private fun guideUser(string: String) {
        runOnUiThread {
            text.text = string
        }
    }

    private suspend fun handleCalibration() = withContext(Dispatchers.Default) {
        guideUser(getString(R.string.instruction_breathe_in))
        delay(3000)
        lifecycleScope.launch {
            displayTimer()
        }
        Log.i("test", "handle calibration launched")

        calibrationDetected = breathingUtils.detectFiveSecondInspiration()
        guideUser("Follow the instructions to clear up the sky!")
        val job = launch {
            breathingUtils.startFromBeginning()
        }
        delay(5000)
        job.join()
        guideUser("Breathe in deep for a minimum of 5 seconds")
        lifecycleScope.launch {
            displayTimer()
        }
        launch {
            Calibrator.calibrate()
        }
        //TODO display text correctly
        Log.i("calibration", "calibration detected")
        // fadeOutGreySky()
        delay(5000)
        guideUser("Hold your breath for 5 seconds")
        lifecycleScope.launch {
            displayTimer()
        }
        Calibrator.calibrateBreathHold(5000, "in")
        guideUser("Breathe out for 5 seconds")
        lifecycleScope.launch {
            displayTimer()
        }
        guideUser("Hold your breath for 5 seconds")
        lifecycleScope.launch {
            displayTimer()
        }
        Calibrator.calibrateBreathHold(5000, "out")
        guideUser("Calibration finished. Now entering Home Menu...")
        delay(5000)
        changeScreen()
    }

    private fun changeScreen() {
        Intent(this, HomeScreenActivity::class.java).also { intent ->
            startService(intent)
            overridePendingTransition(R.anim.slide_up_top, R.anim.slide_up_bottom)
        }
    }

    private fun displayTimer() {
        runOnUiThread {
            text.visibility = View.VISIBLE
        }
        val timer = object : CountDownTimer(6000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                guideUser(millisUntilFinished.div(1000).toString())
            }

            override fun onFinish() {
                if (!calibrationDetected) {
                    Thread.sleep(2000)
                    displayTimer()
                } else runOnUiThread {
                    text.visibility = View.GONE
                }
            }
        }
        timer.start()
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
        val ps1 = setParticleSystem()
        val ps2 = setParticleSystem()
        val ps3 = setParticleSystem()
        ps1.emit(0, 100, 4)
        ps2.emit(1140, 100, 4)
        ps3.emit(2000, 100, 4)
        thread(start = true, isDaemon = true) {
            while (true) {
                ps1.updateEmitPoint(0, 100)
                ps2.updateEmitPoint(1140, 100)
                ps3.updateEmitPoint(2000, 100)
            }
        }
        /* val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fadeout)
        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {

            }

            override fun onAnimationEnd(animation: Animation) {
            }

            override fun onAnimationRepeat(p0: Animation?) {}
        })*/
    }

    private fun setParticleSystem(): ParticleSystem {
        return ParticleSystem(this, 80, R.drawable.white_dot, 10000)
            .setSpeedModuleAndAngleRange(0f, 0.1f, 0, 100)
            .setRotationSpeed(144f)
            .setAcceleration(0.000017f, 90)
    }

    private fun initializeSnow() {
        var iteration = 0
        /* while (true) {
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
} */
    }

    private fun fadeOutGreySky() {
        thread(start = true, isDaemon = true) {
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
    }

    private fun animateBackground() {
        Intent(this, HomeScreenActivity::class.java).also { intent ->
            startService(intent)
            overridePendingTransition(R.anim.slide_up_top, R.anim.slide_up_bottom)

        }
    }


}