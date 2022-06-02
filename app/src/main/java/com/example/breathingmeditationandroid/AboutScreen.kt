package com.example.breathingmeditationandroid

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.animation.AccelerateInterpolator
import androidx.activity.ComponentActivity
import com.plattysoft.leonids.ParticleSystem

class AboutScreen : ComponentActivity() {
    private lateinit var serviceIntent: Intent
    private lateinit var mService: BluetoothConnection
    private lateinit var breathingUtils: BreathingUtils
    private lateinit var particlesMain: ParticleSystem
    private lateinit var particlesSupport: ParticleSystem

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothConnection.LocalBinder
            mService = binder.getService()
            breathingUtils = BreathingUtils(mService)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService.stopService(intent)
            return
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //view
        setContentView(R.layout.about_screen)

        //bind service to activity
        serviceIntent = intent?.extras?.getParcelable("Intent")!!
        applicationContext.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun initializeParticles() {
        particlesMain = ParticleSystem(this, 10, R.drawable.leaf2, 1000)
        particlesMain.setScaleRange(0.7f, 1.3f)
            .setSpeedRange(0.05f, 0.1f)
            .setRotationSpeedRange(50f, 120f)
            .setFadeOut(500, AccelerateInterpolator())
            .emit(100, 800, 10)
        particlesSupport = ParticleSystem(this, 2, R.drawable.leaf1, 500)
        particlesSupport.setScaleRange(0.7f, 1.3f)
            .setSpeedRange(0.05f, 0.1f)
            .setRotationSpeedRange(5f, 50f)
            .setFadeOut(250, AccelerateInterpolator())
            .emit(100, 800, 10)
    }

    private fun moveLeaves() {
        while(mService.mExpiration == 0) {

        }
    }

    private fun moveLeavesToRight() {

    }

    private fun moveLeavesToLeft() {

    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(serviceIntent)
    }
}