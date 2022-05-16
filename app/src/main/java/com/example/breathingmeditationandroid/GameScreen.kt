package com.example.breathingmeditationandroid

import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.View.INVISIBLE
import android.view.animation.*
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

    private lateinit var snow: ImageView

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothConnection.LocalBinder
            mService = binder.getService()
            mBound = true


            breathingUtils = BreathingUtils(mService)
            //breathingUtils.calibrateBreathing()
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
        snow = findViewById<View>(R.id.snow) as ImageView

        deepBreathAnimation()

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
            lifecycleScope.launch {
                //breathingUtils.deepBreathDetected()
                deepBreathAnimation()
            }
        }
    }

    private fun deepBreathAnimation() {
        val snowParticleSystem = ParticleSystem(this, 30, R.drawable.snowflake, 1000)
            .setScaleRange(0.1f, 0.2f)
            .setSpeedModuleAndAngleRange(0.07f, 0.16f, 200, 300)
            .setRotationSpeedRange(90f, 180f)
            .setAcceleration(0.00013f, 90)
            .setFadeOut(200, AccelerateInterpolator())

        val snowParticleSystem2 = ParticleSystem(this, 30, R.drawable.snowflake, 1000)
            .setScaleRange(0.1f, 0.2f)
            .setSpeedModuleAndAngleRange(0.07f, 0.16f, 200, 300)
            .setRotationSpeedRange(90f, 180f)
            .setAcceleration(0.00013f, 90)
            .setFadeOut(200, AccelerateInterpolator())

        val snowParticleSystem3 = ParticleSystem(this, 30, R.drawable.snowflake, 1000)
            .setScaleRange(0.1f, 0.2f)
            .setSpeedModuleAndAngleRange(0.07f, 0.16f, 200, 300)
            .setRotationSpeedRange(90f, 180f)
            .setAcceleration(0.00013f, 90)
            .setFadeOut(200, AccelerateInterpolator())

        val snowParticleSystem4 = ParticleSystem(this, 30, R.drawable.snowflake, 1000)
            .setScaleRange(0.1f, 0.2f)
            .setSpeedModuleAndAngleRange(0.07f, 0.16f, 200, 300)
            .setRotationSpeedRange(90f, 180f)
            .setAcceleration(0.00013f, 90)
            .setFadeOut(200, AccelerateInterpolator())

        val snowParticleSystem5 = ParticleSystem(this, 30, R.drawable.snowflake, 1000)
            .setScaleRange(0.1f, 0.2f)
            .setSpeedModuleAndAngleRange(0.07f, 0.16f, 200, 300)
            .setRotationSpeedRange(90f, 180f)
            .setAcceleration(0.00013f, 90)
            .setFadeOut(200, AccelerateInterpolator())

        val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fadeout)
        snow.startAnimation(fadeOut)

        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                Log.i("info", "onStartOben")
                snowParticleSystem.emit(50,200, 5)
                snowParticleSystem2.emit(600, 200, 5)
                snowParticleSystem3.emit(1200, 200, 5)
                snowParticleSystem4.emit(1500, 380, 5)
                snowParticleSystem5.emit(1750, 150, 5)
                Log.i("info", "onStartUnten")

            }
            override fun onAnimationEnd(animation: Animation) {
                snow.visibility = INVISIBLE
                Log.i("info", "onEndOben")
                snowParticleSystem.stopEmitting()
                snowParticleSystem2.stopEmitting()
                snowParticleSystem3.stopEmitting()
                snowParticleSystem4.stopEmitting()
                snowParticleSystem5.stopEmitting()
                Log.i("info", "onEndUnten")
            }
            override fun onAnimationRepeat(p0: Animation?) {}
        })

        fadeOut.cancel()





        //on Animation End make it invisible
    }


}