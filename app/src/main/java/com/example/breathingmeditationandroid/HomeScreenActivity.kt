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
import android.widget.ImageView
import androidx.activity.ComponentActivity
import com.plattysoft.leonids.ParticleSystem
import kotlin.concurrent.thread
import kotlin.math.floor

class HomeScreenActivity : ComponentActivity() {

    private lateinit var container: ViewGroup
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
    private lateinit var bubble1: ImageView
    private lateinit var bubble2: ImageView
    private lateinit var bubble3: ImageView
    private lateinit var holdBreathGesture: HoldBreathGesture

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothConnection.LocalBinder
            mService = binder.getService()
            breathingUtils = BreathingUtils(mService)
            mBound = true
            holdBreathGesture = HoldBreathGesture(mService)
            holdBreathGesture.detect()
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
        bubble1 = findViewById(R.id.imageViewSettings)
        bubble2 = findViewById(R.id.imageViewCalibrate)
        bubble3 = findViewById(R.id.imageViewPlay)
        mDevice = intent?.extras?.getParcelable("Device")
        currX = xBorderLeft.toDouble()
        currY = yBorderBottom.toDouble()
        Intent(applicationContext, BluetoothConnection::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
            intent.putExtra("Device", mDevice)
            startService(intent)
        }
    }

    private fun initializeParticleSystems() {
        particlesMain = ParticleSystem(this, 10, R.drawable.leaf2, 1000)
        particlesMain.setScaleRange(0.7f, 1.3f)
            .setSpeedRange(0.05f, 0.1f)
            .setRotationSpeedRange(50f, 120f)
            .setFadeOut(500, AccelerateInterpolator())
            .emit(xBorderLeft, yBorderBottom, 10)
        particlesSupprt = ParticleSystem(this, 2, R.drawable.leaf1, 500)
        particlesSupprt.setScaleRange(0.7f, 1.3f)
            .setSpeedRange(0.05f, 0.1f)
            .setRotationSpeedRange(5f, 50f)
            .setFadeOut(250, AccelerateInterpolator())
            .emit(xBorderLeft, yBorderBottom, 10)
    }

    private fun animateLeaves() {
        //TODO weg aus der methode
        val coordinatesBubble1 = Pair(bubble1.left, bubble1.right)
        val coordinatesBubble2 = Pair(bubble2.left, bubble2.right)
        val coordinatesBubble3 = Pair(bubble3.left, bubble3.right)

        initializeParticleSystems()
        thread(start = true, isDaemon = true) {
            var prevAbdo = breathingUtils.smoothValue().first
            var prevThor = breathingUtils.smoothValue().second
            while (true) {
                if (detectInspiration(
                        Pair(prevAbdo, prevThor),
                        Pair(breathingUtils.smoothValue().first, breathingUtils.smoothValue().second)
                    )
                ) {
                    currX = calcNewXValue(currX, '+')
                    currY = calcNewYValue(currY, '-')
                    moveLeavesUp(currX, currY, particlesMain)
                    moveLeavesUp(currX, currY, particlesSupprt)
                    detectSelections(coordinatesBubble1, coordinatesBubble2, coordinatesBubble3)
                }
                if (detectRespiration(
                        Pair(prevAbdo, prevThor),
                        Pair(breathingUtils.smoothValue().first, breathingUtils.smoothValue().second)
                    )
                ) {
                    currX = calcNewXValue(currX, '-')
                    currY = calcNewYValue(currY, '+')
                    moveLeavesDown(currX, currY, particlesMain)
                    moveLeavesDown(currX, currY, particlesSupprt)
                    detectSelections(coordinatesBubble1, coordinatesBubble2, coordinatesBubble3)
                }
                prevAbdo = breathingUtils.smoothValue().first
                prevThor = breathingUtils.smoothValue().second
                Thread.sleep(50)
            }
        }
    }

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
        val smoothedValues = breathingUtils.smoothValue()
        when (operator) {
            '+' -> return xNew.plus(calcCombinedValue(smoothedValues.second, smoothedValues.first, 50.0))
            '-' -> return xNew.minus(calcCombinedValue(smoothedValues.second, smoothedValues.first, 50.0))
        }
        return 0.0
    }

    private fun calcCombinedValue(abdo: Double, thor: Double, factor: Double): Double {
        return ((thor.plus(10)).div((abdo).plus(10))).times(factor)
    }

    private fun calcNewYValue(yNew: Double, operator: Char): Double {
        val smoothedValues = breathingUtils.smoothValue()
        when (operator) {
            '+' -> return yNew.plus(calcCombinedValue(smoothedValues.second, smoothedValues.first, 15.0))
            '-' -> return yNew.minus(calcCombinedValue(smoothedValues.second, smoothedValues.first, 15.0))
        }
        return 0.0
    }

    private fun detectSelections(
        coordinatesBubble1: Pair<Int, Int>,
        coordinatesBubble2: Pair<Int, Int>,
        coordinatesBubble3: Pair<Int, Int>
    ) {
        //TODO add Y value
        runOnUiThread {
            if (currX in coordinatesBubble1.first.toDouble()..coordinatesBubble1.second.toDouble()) {
                bubble1.alpha = 1.0f
                if (holdBreathGesture.getHold()) {
                    bubble1.alpha = 0.0f
                }
            } else bubble1.alpha = 0.7f
            if (currX in coordinatesBubble2.first.toDouble()..coordinatesBubble2.second.toDouble()) {
                bubble2.alpha = 1.0f
            } else bubble2.alpha = 0.7f
            if (currX in coordinatesBubble3.first.toDouble()..coordinatesBubble3.second.toDouble()) {
                bubble3.alpha = 1.0f
            } else bubble3.alpha = 0.7f
        }
    }
}