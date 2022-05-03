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
    private var currX: Double = 0.0
    private var currY: Double = 0.0

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
        currX = xBorderLeft.toDouble()
        currY = yBorderBottom.toDouble()
        Intent(applicationContext, BluetoothConnection::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
            intent.putExtra("Device", mDevice)
            startService(intent)
        }
    }

    private fun animateLeaves() {
        var prevInspiration = false
        var prevRespiration = false

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
            var prevAbdo = smoothValue().first
            var prevThor = smoothValue().second
            while (true) {
                if (detectInspiration(
                        Pair(prevAbdo, prevThor),
                        Pair(smoothValue().first, smoothValue().second)
                    )
                ) {
                    if (prevRespiration) {
                        resetEmitterInspiration(particlesMain)
                        resetEmitterInspiration(particlesSupprt)
                        currX = xBorderLeft.toDouble()
                        currY = yBorderBottom.toDouble()
                    }
                    prevInspiration = true
                    prevRespiration = false
                    currX = calcNewXValue(currX, '+')
                    currY = calcNewYValue(currY, '-')
                    moveLeavesUp(currX, currY, particlesMain)
                    moveLeavesUp(currX, currY, particlesSupprt)
                }
                if (detectRespiration(
                        Pair(prevAbdo, prevThor),
                        Pair(smoothValue().first, smoothValue().second)
                    )
                ) {
                    if (prevInspiration) {
                        resetEmitterRespiration(particlesMain)
                        resetEmitterRespiration(particlesSupprt)
                        currX = xBorderRight.toDouble()
                        currY = yBorderTop.toDouble()
                    }
                    prevRespiration = true
                    prevInspiration = false
                    currX = calcNewXValue(currX, '-')
                    currY = calcNewYValue(currY, '+')
                    Log.i("xyValues:", "X: $currX ; Y: $currY")
                    moveLeavesDown(currX, currY, particlesMain)
                    moveLeavesDown(currX, currY, particlesSupprt)
                }
                prevAbdo = smoothValue().first
                prevThor = smoothValue().second
                Thread.sleep(50)
            }
        }
    }

    //TODO funktioniert ned wirklich
    private fun moveLeavesUp(xValue: Double, yValue: Double, particleSystem: ParticleSystem) {
        val x = floor(xValue)
        val y = floor(yValue)
        if (x <= xBorderRight && y >= yBorderTop)
            particleSystem.updateEmitPoint(x.toInt(), y.toInt())
        else if (x > xBorderRight && y >= yBorderTop) {
            particleSystem.updateEmitPoint(xBorderRight, y.toInt())
            currX = xBorderRight.toDouble()
        } else if (x <= xBorderRight && y < yBorderTop) {
            particleSystem.updateEmitPoint(x.toInt(), yBorderTop)
            currY = yBorderTop.toDouble()
        } else {
            particleSystem.updateEmitPoint(xBorderRight, yBorderTop)
            currX = xBorderRight.toDouble()
            currY = yBorderTop.toDouble()
        }
    }

    private fun moveLeavesDown(xValue: Double, yValue: Double, particleSystem: ParticleSystem) {
        val x = floor(xValue)
        val y = floor(yValue)
        if (x >= xBorderLeft && y <= yBorderBottom)
            particleSystem.updateEmitPoint(x.toInt(), y.toInt())
        else if (x < xBorderLeft && y <= yBorderBottom) {
            particleSystem.updateEmitPoint(xBorderLeft, y.toInt())
            currX = xBorderLeft.toDouble()
        } else if (x >= xBorderLeft && y > yBorderBottom) {
            particleSystem.updateEmitPoint(x.toInt(), yBorderBottom)
            currY = yBorderBottom.toDouble()
        } else {
            particleSystem.updateEmitPoint(xBorderLeft, yBorderBottom)
            currX = xBorderLeft.toDouble()
            currY = yBorderBottom.toDouble()
        }
    }

    private fun detectRespiration(prev: Pair<Double, Double>, curr: Pair<Double, Double>): Boolean {
        return curr.first < prev.first && curr.second < prev.second
    }

    private fun detectInspiration(prev: Pair<Double, Double>, curr: Pair<Double, Double>): Boolean {
        return curr.first > prev.first && curr.second > prev.second
    }

    private fun calcNewXValue(xNew: Double, operator: Char): Double {
        when (operator) {
            '+' -> return xNew.plus(smoothValue().second.plus(5).plus(smoothValue().first.plus(3)).times(5))
            '-' -> return xNew.minus(smoothValue().second.plus(5).plus(smoothValue().first.plus(3)).times(5))
        }
        return 0.0
    }

    private fun calcNewYValue(yNew: Double, operator: Char): Double {
        when (operator) {
            '+' -> return yNew.plus((smoothValue().second.plus(5).plus(smoothValue().first.plus(3))).times(2))
            '-' -> return yNew.minus((smoothValue().second.plus(5).plus(smoothValue().first.plus(1))).times(2))
        }
        return 0.0
    }

    private fun resetEmitterRespiration(particleSystem: ParticleSystem) {
        particleSystem.updateEmitPoint(xBorderRight, yBorderTop)
    }

    private fun resetEmitterInspiration(particleSystem: ParticleSystem) {
        particleSystem.updateEmitPoint(xBorderLeft, yBorderBottom)
    }

    private fun smoothValue(): Pair<Double, Double> {
        val valueList = mutableListOf(Pair(mService.mAbdoCorrected, mService.mThorCorrected))
        while (valueList.size <= 6) {
            valueList.add(Pair(mService.mAbdoCorrected, mService.mThorCorrected))
        }
        return calculateMedian(valueList)
    }

    private fun calculateMedian(list: MutableList<Pair<Double, Double>>): Pair<Double, Double> {
        val medianThor = (list[list.size.div(2)].first.plus(list[list.size.div(2).plus(1)].first)).div(2)
        val medianAbdo = (list[list.size.div(2)].second.plus(list[list.size.div(2).plus(1)].second)).div(2)
        return Pair(medianAbdo, medianThor)
    }
}