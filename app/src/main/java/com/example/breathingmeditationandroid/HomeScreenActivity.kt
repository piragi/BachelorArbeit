package com.example.breathingmeditationandroid

import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import androidx.activity.ComponentActivity
import com.bullfrog.particle.IParticleManager
import com.bullfrog.particle.Particles
import com.plattysoft.leonids.ParticleSystem
import kotlin.concurrent.thread
import kotlin.math.floor

class HomeScreenActivity : ComponentActivity() {

    private lateinit var container: ViewGroup
    private var particleManager: IParticleManager? = null
    private lateinit var particlesMain: ParticleSystem
    private lateinit var particlesSupprt: ParticleSystem
    private var mDevice: BluetoothDevice? = null
    private lateinit var mService: BluetoothConnection
    private var mBound = false
    private lateinit var breathingUtils: BreathingUtils
    private val xBorderLeft = 100
    private val xBorderRight = 2000
    private val yBorderTop = 300
    private val yBorderBottom = 800

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothConnection.LocalBinder
            mService = binder.getService()
            breathingUtils = BreathingUtils(mService)
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
        particlesMain = ParticleSystem(this, 10, R.drawable.leaf2, 1000)
        particlesMain.setScaleRange(0.7f, 1.3f)
            .setSpeedRange(0.05f, 0.1f)
            .setRotationSpeedRange(50f, 120f)
            .setFadeOut(300, AccelerateInterpolator())
            .emit(xBorderLeft, yBorderBottom, 10)
        particlesSupprt = ParticleSystem(this, 10, R.drawable.leaf3, 800)
        particlesSupprt.setScaleRange(0.7f, 1.3f)
            .setSpeedRange(0.05f, 0.1f)
            .setRotationSpeedRange(10f, 100f)
            .setFadeOut(300, AccelerateInterpolator())
            .emit(xBorderLeft, yBorderBottom, 10)

        thread(start = true, isDaemon = true) {
            var xNew = xBorderLeft.toDouble()
            var yNew = yBorderBottom.toDouble()
            var prevAbdo = mService.mAbdoCorrected
            var prevThor = mService.mThorCorrected
            while (true) {
                if (detectInspiration(
                        Pair(prevAbdo, prevThor),
                        Pair(mService.mAbdoCorrected, mService.mThorCorrected)
                    )
                ) {
                    xNew = calcNewXValue(xNew, '+')
                    yNew = calcNewYValue(yNew, '-')
                    moveLeavesUp(xNew, yNew, particlesMain)
                    moveLeavesUp(xNew, yNew, particlesSupprt)
                }
                if (detectRespiration(
                        Pair(prevAbdo, prevThor),
                        Pair(mService.mAbdoCorrected, mService.mThorCorrected)
                    )
                ) {
                    xNew = calcNewXValue(xNew, '-')
                    yNew = calcNewYValue(yNew, '+')
                    moveLeavesDown(xNew, yNew, particlesMain)
                    moveLeavesDown(xNew, yNew, particlesSupprt)
                }
                prevAbdo = mService.mAbdoCorrected
                prevThor = mService.mThorCorrected
                Thread.sleep(100)
            }
        }
    }

    //TODO funktioniert ned wirklich
    private fun moveLeavesUp(xValue: Double, yValue: Double, particleSystem: ParticleSystem) {
        val x = floor(xValue)
        val y = floor(yValue)
        if (x < xBorderRight && y < yBorderTop)
            particleSystem.updateEmitPoint(x.toInt(), y.toInt())
        else if (x > xBorderRight && y < yBorderTop)
            particleSystem.updateEmitPoint(xBorderRight, y.toInt())
        else if (x < xBorderRight && y > yBorderTop)
            particleSystem.updateEmitPoint(x.toInt(), yBorderTop)
        else particleSystem.updateEmitPoint(xBorderRight, yBorderTop)
    }

    private fun moveLeavesDown(xValue: Double, yValue: Double, particleSystem: ParticleSystem) {
        val x = floor(xValue)
        val y = floor(yValue)
        if (x > xBorderLeft && y > yBorderBottom)
            particleSystem.updateEmitPoint(x.toInt(), y.toInt())
        else if (x < xBorderLeft && y > yBorderBottom)
            particleSystem.updateEmitPoint(xBorderLeft, y.toInt())
        else if (x > xBorderLeft && y < yBorderBottom)
            particleSystem.updateEmitPoint(x.toInt(), yBorderBottom)
        else particleSystem.updateEmitPoint(xBorderLeft, yBorderBottom)
    }

    private fun detectRespiration(prev: Pair<Double, Double>, curr: Pair<Double, Double>): Boolean {
        return curr.first < prev.first && curr.second < prev.second
    }

    private fun detectInspiration(prev: Pair<Double, Double>, curr: Pair<Double, Double>): Boolean {
        return curr.first > prev.first && curr.second > prev.second
    }

    private fun calcNewXValue(xNew: Double, operator: Char): Double {
        when (operator) {
            '+' -> return xNew.plus(mService.mThorCorrected.plus(5).plus(mService.mAbdoCorrected.plus(3)).times(5))
            '-' -> return xNew.minus(mService.mThorCorrected.plus(5).plus(mService.mAbdoCorrected.plus(3)).times(5))
        }
        return 0.0
    }

    private fun calcNewYValue(yNew: Double, operator: Char): Double {
        when (operator) {
            '+' -> return yNew.plus((mService.mThorCorrected.plus(5).plus(mService.mAbdoCorrected.plus(3))).times(2))
            '-' -> return yNew.minus((mService.mThorCorrected.plus(5).plus(mService.mAbdoCorrected.plus(1))).times(2))
        }
        return 0.0
    }
}