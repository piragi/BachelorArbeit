package com.example.breathingmeditationandroid

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import androidx.activity.ComponentActivity
import com.example.breathingmeditationandroid.gestures.HoldBreathGesture
import com.plattysoft.leonids.ParticleSystem
import kotlin.concurrent.thread

class AboutScreen : ComponentActivity() {
    private lateinit var serviceIntent: Intent
    private lateinit var mService: BluetoothConnection
    private lateinit var breathingUtils: BreathingUtils
    private lateinit var particlesMain: ParticleSystem
    private lateinit var particlesSupport: ParticleSystem
    private lateinit var holdBreathGesture: HoldBreathGesture

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothConnection.LocalBinder
            mService = binder.getService()
            holdBreathGesture = HoldBreathGesture(mService, 5000.0)
            breathingUtils = BreathingUtils(mService)

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
        val startX = ScreenUtils.xBorderLeft
        val startY = ScreenUtils.yBorderBottom
        val factor = ScreenUtils.xBorderRight.minus(startX).div(5000)
        moveLeaves(startX, startY, factor)
    }

    private fun moveLeaves(startX: Int, y: Int, updateFactor: Int) {
        runOnUiThread {
            val cloud = findViewById<ImageView>(R.id.cloud)
            val cloudLeft = cloud.left
            val cloudRight = cloud.right
            var newX = startX
            while (true) {
                if (mService.mExpiration == 0 && newX <= ScreenUtils.xBorderRight) {
                    newX = newX.plus(updateFactor)
                    particlesMain.updateEmitPoint(newX, y)
                }
                if (mService.mInspiration == 0 && newX >= startX) {
                    newX = newX.minus(updateFactor)
                    particlesMain.updateEmitPoint(newX, y)
                }
                holdBreathGesture.stop = newX !in cloudLeft..cloudRight
                if (!holdBreathGesture.stop)
                    cloud.alpha = 1.0f
            }
        }
    }

    private fun returnToHomeScreen() {
        thread(start = true, isDaemon = true) {
            while (!holdBreathGesture.hold)
                continue
            Intent(this, HomeScreenActivity::class.java).also { intent ->
                intent.putExtra("Intent", serviceIntent)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_up_top, R.anim.slide_up_bottom)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(serviceIntent)
    }
}