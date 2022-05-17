package com.example.breathingmeditationandroid

import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.view.View.INVISIBLE
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.plattysoft.leonids.ParticleSystem
import kotlinx.coroutines.launch
import kotlin.concurrent.thread


class GameScreen : ComponentActivity() {

    //Bluetooth Connection
    private var mDevice: BluetoothDevice? = null

    //Binding service
    private lateinit var mService: BluetoothConnection
    private var mBound = false

    private lateinit var breathingUtils: BreathingUtils
    private lateinit var deepAbdoBreathGesture: DeepAbdoBreathGesture
    private lateinit var staccatoBreathGesture: StaccatoBreathGesture

    private lateinit var snow: ImageView

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothConnection.LocalBinder
            mService = binder.getService()
            mBound = true


            breathingUtils = BreathingUtils(mService)
            deepAbdoBreathGesture = DeepAbdoBreathGesture(mService, breathingUtils)
            staccatoBreathGesture = StaccatoBreathGesture(mService, breathingUtils)
            startLevel()

        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService.stopService(intent)
            mBound = false
            return
        }
    }

    //View starten
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        //view
        setContentView(R.layout.game_screen)
        //snow = findViewById<View>(R.id.snow) as ImageView

        //deepBellyBreathAnimation()

        //setup and start bluetooth service
        mDevice = intent?.extras?.getParcelable("Device")

        Intent(applicationContext, BluetoothConnection::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
            intent.putExtra("Device", mDevice)
            startService(intent)
        }
    }

    fun startLevel() {
        thread(start = true, isDaemon = true) {
            try {
                lifecycleScope.launch {
                    //breathingUtils.deepBellyBreathDetected()
                    //deepBellyBreathAnimation()
                    //deepAbdoBreathGesture.detect()
                    breathingUtils.calibrateBreathing()
                    staccatoBreathGesture.detect()
                }
            } catch (consumed: InterruptedException) {
                Thread.currentThread().interrupt()
            }

        }
    }

    private fun deepBellyBreathAnimation() {
        val snowParticleSystem2 = setSnowParticleSystem()
        val snowParticleSystem3 = setSnowParticleSystem()
        val snowParticleSystem4 = setSnowParticleSystem()
        val snowParticleSystem5 = setSnowParticleSystem()
        val snowParticleSystem = setSnowParticleSystem()

        val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fadeout)

        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                snowParticleSystem.emit(50, 200, 7)
                snowParticleSystem2.emit(600, 200, 7)
                snowParticleSystem3.emit(1200, 200, 7)
                snowParticleSystem4.emit(1500, 380, 7)
                snowParticleSystem5.emit(1750, 150, 7)
            }

            override fun onAnimationEnd(animation: Animation) {
                snowParticleSystem2.stopEmitting()
                snowParticleSystem3.stopEmitting()
                snowParticleSystem4.stopEmitting()
                snowParticleSystem5.stopEmitting()
                snowParticleSystem.stopEmitting()
                snow.visibility = INVISIBLE
                fadeOut.cancel()
            }

            override fun onAnimationRepeat(p0: Animation?) {}
        })

        snow.startAnimation(fadeOut)
    }

    private fun setSnowParticleSystem(): ParticleSystem {
        return ParticleSystem(this, 30, R.drawable.snowflake, 200)
            .setScaleRange(0.1f, 0.2f)
            .setSpeedModuleAndAngleRange(0.07f, 0.16f, 200, 300)
            .setRotationSpeedRange(90f, 180f)
            .setAcceleration(0.00013f, 90)
            .setFadeOut(150, AccelerateInterpolator())
    }


}