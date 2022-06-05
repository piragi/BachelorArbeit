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
    private var calibrationDetected = false
    private var calibrationFinished = false
    private lateinit var serviceIntent: Intent
    private lateinit var mService: BluetoothConnection
    private lateinit var greySky: ImageView
    private lateinit var clouds: ImageView
    private lateinit var background: ImageView
    private lateinit var breathingUtils: BreathingUtils
    private lateinit var text: TextView
    private lateinit var ps1: ParticleSystem
    private lateinit var ps2: ParticleSystem
    private lateinit var ps3: ParticleSystem


    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothConnection.LocalBinder
            mService = binder.getService()
            Calibrator.initialize(mService)
            breathingUtils = BreathingUtils(mService)
            lifecycleScope.launch { handleCalibration() }
            lifecycleScope.launch { changeScreen() }
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            mService.stopService(intent)
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
        serviceIntent = intent?.extras?.getParcelable("Intent")!!
        applicationContext.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(serviceIntent)
    }

    private suspend fun handleCalibration() = withContext(Dispatchers.Default) {
        displayText("Calibration is starting...", 5000)
        displayText("Follow the instructions to clear up the sky!", 5000)
        launch {
            displayText("Breathe in deeply...", Double.POSITIVE_INFINITY.toLong())
        }
        calibrationDetected = breathingUtils.detectFiveSecondInspiration()
        launch {
            displayText("Breathe out...", Double.POSITIVE_INFINITY.toLong())
        }
        delay(6000)
        launch { Calibrator.calibrate() }
        var iteration = 0
        repeat(2) {
            iteration++
            Log.i("Calibration", "Screen: Breathe in")
            when (iteration) {
                1 -> displayText("Breathe in deeply into your stomach...", 5000)
                2 -> displayText("Breathe in deeply into your chest...", 5000)
            }
            Log.i("Calibration", "Screen: Hold breath")
            displayText("Hold your breath...", 2000)

            when (iteration) {
                1 -> decreaseSnowFlow(0.5)
                2 -> decreaseSnowFlow(1.0)
            }

            launch { Calibrator.calibrateBreathHold(5000, "in") }
            lifecycleScope.launch {
                displayTimer(4000)
            }
            delay(4000)
            Log.i("Calibration", "Screen: Breathe out")
            displayText("Breathe out...", 5000)
            Log.i("Calibration", "Screen: Hold breath")
            displayText("Hold your breath...", 2000)
            launch {
                Calibrator.calibrateBreathHold(5000, "out")
            }
            if (iteration == 2)
                launch { fadeOutGreySky() }
            lifecycleScope.launch { displayTimer(4000) }
            delay(4000)
        }
        displayText("Calibration finished!", 5000)
        calibrationFinished = true
    }

    private fun changeScreen() {
        thread(start = true, isDaemon = true) {
            while (!calibrationFinished) {
                continue
            }
            Intent(this, HomeScreenActivity::class.java).also { intent ->
                intent.putExtra("Intent", serviceIntent)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_up_top, R.anim.slide_up_bottom)
            }
        }
    }

    private fun displayTimer(time: Long) {
        runOnUiThread {
            text.visibility = View.VISIBLE
        }
        val timer = object : CountDownTimer(time, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                lifecycleScope.launch {
                    displayText(millisUntilFinished.div(1000).toString(), 0)
                }
            }

            override fun onFinish() {
                if (!calibrationDetected) {
                    lifecycleScope.launch {
                        displayTimer(time)
                        runOnUiThread {
                            text.visibility = View.GONE
                        }
                    }
                }
            }
        }
        timer.start()
    }

    private suspend fun displayText(string: String, time: Long) {
        lifecycleScope.launch {
            runOnUiThread {
                text.text = string
            }
            delay(time)
        }.join()
    }

    private fun decreaseSnowFlow(percent: Double) {
        runOnUiThread {
            var iteration = 0
            val fadeout = 10000
            while (iteration < fadeout.times(percent)) {
                ps1.setFadeOut(fadeout.minus(iteration).toLong())
                ps2.setFadeOut(fadeout.minus(iteration).toLong())
                ps3.setFadeOut(fadeout.minus(iteration).toLong())

                iteration++
            }
            if (percent == 1.0) {
                ps1.stopEmitting()
                ps2.stopEmitting()
                ps3.stopEmitting()
            }
        }
    }

    private fun snow() {
        ps1 = setParticleSystem()
        ps2 = setParticleSystem()
        ps3 = setParticleSystem()

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
    }

    private fun setParticleSystem(): ParticleSystem {
        return ParticleSystem(this, 80, R.drawable.white_dot, 10000)
            .setSpeedModuleAndAngleRange(0f, 0.1f, 0, 100)
            .setRotationSpeed(144f)
            .setAcceleration(0.000017f, 90)
    }

    private fun fadeOutGreySky() {
        runOnUiThread {
            val animation = AnimationUtils.loadAnimation(this, R.anim.fadeout)
            greySky.startAnimation(animation)
            clouds.startAnimation(animation)
            Handler(Looper.getMainLooper()).postDelayed({
                greySky.visibility = View.GONE
                clouds.visibility = View.GONE
            }, 0)
        }
    }
}