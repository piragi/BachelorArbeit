package com.example.breathingmeditationandroid

import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.bullfrog.particle.IParticleManager
import com.bullfrog.particle.Particles
import com.plattysoft.leonids.ParticleSystem
import kotlin.concurrent.thread

class HomeScreenActivity : ComponentActivity() {

    private lateinit var container: ViewGroup
    private var particleManager: IParticleManager? = null
    private lateinit var particlesMain: ParticleSystem
    private lateinit var particlesSupprt: ParticleSystem
    private var mDevice: BluetoothDevice? = null
    private lateinit var mService: BluetoothConnection
    private var mBound = false
    private val startingValueX = 100
    private val startingValueY = 800

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothConnection.LocalBinder
            mService = binder.getService()
            mBound = true
            animateLeaves()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService.stopService(intent)
            mBound = false
            return
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_screen)
        container = findViewById(R.id.home_screen)
        particleManager = Particles.with(this, container)
        mDevice = intent?.extras?.getParcelable("Device")
        Intent(applicationContext, BluetoothConnection::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
            intent.putExtra("Device", mDevice)
            startService(intent)
        }
    }

    private fun animateLeaves() {
        // y = a * x + b
        // Create a particle system and start emiting
        var x = startingValueX
        var y = startingValueY
        particlesMain = ParticleSystem(this, 10, R.drawable.leaf2, 1000)
        particlesMain.setScaleRange(0.7f, 1.3f)
            .setSpeedRange(0.05f, 0.1f)
            .setRotationSpeedRange(50f, 120f)
            .setFadeOut(300, AccelerateInterpolator())
            .emit(x, y, 10)
        particlesSupprt = ParticleSystem(this, 10, R.drawable.leaf3, 800)
        particlesSupprt.setScaleRange(0.7f, 1.3f)
            .setSpeedRange(0.05f, 0.1f)
            .setRotationSpeedRange(10f, 100f)
            .setFadeOut(300, AccelerateInterpolator())
            .emit(x, y, 10)
        thread (start = true, isDaemon = true) {
            while (true) {
                val combined = (((mService.mThorCorrected * 0.8) + (mService.mAbdoCorrected * 0.2)) * 100)
                // Log.i("thorValue", mService.mThorCorrected.toString())
                // Log.i("abdoValue", mService.mAbdoCorrected.toString())
                if (x < 2000 && y < 900) {
                    x = x.times(combined).plus(100).toInt()
                    Log.i("xValue", x.toString())
                    y = (y.minus(100)).div(combined).toInt()
                    Log.i("yValue", y.toString())
                    particlesMain.updateEmitPoint(x, y)
                    particlesSupprt.updateEmitPoint(x, y)
                } else {
                    x = x.times(combined).minus(100).toInt()
                    y = (y.plus(100)).div(combined).toInt()
                    particlesMain.updateEmitPoint(x, y)
                    particlesSupprt.updateEmitPoint(x, y)
                }
                Thread.sleep(20)
            }
        }
    }
}