package com.example.breathingmeditationandroid

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import androidx.activity.ComponentActivity
import com.example.breathingmeditationandroid.gestures.HoldBreathGesture
import com.plattysoft.leonids.ParticleSystem
import java.lang.Math.ceil
import java.lang.Math.round
import kotlin.concurrent.thread
import kotlin.math.roundToInt

class AboutScreen : ComponentActivity() {
    private lateinit var serviceIntent: Intent
    private lateinit var mService: BluetoothConnection
    private lateinit var breathingUtils: BreathingUtils
    private lateinit var particlesMain: ParticleSystem
    private lateinit var particlesSupport: ParticleSystem
    private lateinit var holdBreathGesture: HoldBreathGesture
    private lateinit var cloud: ImageView
    private var stop = false

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

        serviceIntent = intent?.extras?.getParcelable("Intent")!!
        applicationContext.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun initializeParticles() {
        particlesMain = ParticleSystem(this, 10, R.drawable.leaf2, 1000)
        particlesMain.setScaleRange(0.7f, 1.3f)
            .setSpeedRange(0.05f, 0.1f)
            .setRotationSpeedRange(50f, 120f)
            .setFadeOut(500, AccelerateInterpolator())
            .emit(ScreenUtils.xBorderLeft, ScreenUtils.yBorderBottom, 10)
        particlesSupport = ParticleSystem(this, 2, R.drawable.leaf1, 500)
        particlesSupport.setScaleRange(0.7f, 1.3f)
            .setSpeedRange(0.05f, 0.1f)
            .setRotationSpeedRange(5f, 50f)
            .setFadeOut(250, AccelerateInterpolator())
            .emit(ScreenUtils.xBorderLeft, ScreenUtils.yBorderBottom, 10)
    }

    private fun animateLeaves() {
        initializeParticles()
        moveLeaves()
    }

    private fun moveLeaves() {
        thread(start = true, isDaemon = true) {
            val cloudLeft = cloud.left
            val cloudRight = cloud.right
            var newX: Double
            while (!stop) {

                val currValues = breathingUtils.smoothValue()

                newX = (breathingUtils.calcCombinedValue(currValues.first, currValues.second)
                    .times(Calibrator.flowFactorX))
                if (newX in ScreenUtils.xBorderLeft.toDouble()..ScreenUtils.xBorderRight.toDouble()) {
                    particlesMain.updateEmitPoint(newX.roundToInt(), ScreenUtils.yBorderBottom)
                    particlesSupport.updateEmitPoint(newX.roundToInt(), ScreenUtils.yBorderBottom)
                }

                //TODO selection not correct

                if (newX !in cloudLeft.toDouble()..cloudRight.toDouble()) {
                    holdBreathGesture.stopDetection()
                    runOnUiThread { cloud.alpha = 0.7f }
                } else {
                    holdBreathGesture.resumeDetection()
                    runOnUiThread { cloud.alpha = 1.0f }
                }
                Thread.sleep(5)
            }
        }
    }

    private fun returnToHomeScreen() {
        thread(start = true, isDaemon = true) {
            holdBreathGesture.borderAbdo = Calibrator.holdBreathBufferInAbdo
            holdBreathGesture.borderThor = Calibrator.holdBreathBufferInThor
            while (!holdBreathGesture.hold)
                continue
        }
        stop = true
        Intent(this, HomeScreenActivity::class.java).also { intent ->
            intent.putExtra("Intent", serviceIntent)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_up_top, R.anim.slide_up_bottom)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(serviceIntent)
    }
}