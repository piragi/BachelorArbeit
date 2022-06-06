package com.example.breathingmeditationandroid

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings.Global
import android.util.Log
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.breathingmeditationandroid.gestures.HoldBreathGesture
import com.plattysoft.leonids.ParticleSystem
import kotlinx.coroutines.*
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.floor

class HomeScreenActivity : ComponentActivity() {

    private lateinit var container: ViewGroup
    private lateinit var particlesMain: ParticleSystem
    private lateinit var particlesSupprt: ParticleSystem
    private lateinit var serviceIntent: Intent
    private lateinit var mService: BluetoothConnection
    private lateinit var breathingUtils: BreathingUtils

    private val xBorderLeft = 100 //TODO relative to device
    private val xBorderRight = 2000
    private val yBorderTop = 300
    private val yBorderBottom = 800
    private var currX: Double = 0.0
    private var currY: Double = 0.0
    private var prevX: Double = 0.0
    private var prevY: Double = 0.0

    private var prevAbdo: Double = 0.0
    private var prevThor: Double = 0.0

    private var selectionDetected: Boolean = false
    private lateinit var bubble1: ImageView
    private lateinit var bubble2: ImageView
    private lateinit var bubble3: ImageView
    private lateinit var holdBreathGesture: HoldBreathGesture

    private var bubble1Selected = false
    private var bubble2Selected = false
    private var bubble3Selected = false

    private var stop = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothConnection.LocalBinder
            mService = binder.getService()
            breathingUtils = BreathingUtils(mService)
            holdBreathGesture = HoldBreathGesture(mService, 5000.0)
            Log.i("init", "service connected")

            start()

        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService.stopService(intent)
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

        currX = xBorderLeft.toDouble()
        currY = yBorderBottom.toDouble()
        // stop = false
        serviceIntent = intent?.extras?.getParcelable("Intent")!!
        applicationContext.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(serviceIntent)
    }

    private fun start() {
        initializeParticleSystems()
        animateLeaves()
        changeToAboutScreen()
        changeToCalibrationScreen()
        changeToGameScreen()
        holdBreathGesture.detect()
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
        thread(start = true, isDaemon = true) {
            val coordinatesBubble1 = Pair(bubble1.left, bubble1.right)
            val coordinatesBubble2 = Pair(bubble2.left, bubble2.right)
            val coordinatesBubble3 = Pair(bubble3.left, bubble3.right)
            val currVal = breathingUtils.smoothValue()
            prevAbdo = currVal.first
            prevThor = currVal.second
            Log.i("init", "animate leaves started")
            // TODO detect selection funktioniert nich immer
            changeScreen(coordinatesBubble1, coordinatesBubble2, coordinatesBubble3)
            // breathingUtils.startFromBeginning()
            while (!stop) {
                val currValue = breathingUtils.smoothValue()
                val combinedValue = breathingUtils.calcCombinedValue(currValue.first, currValue.second)

                // currX = (combinedValue).times(Calibrator.flowFactorX).plus(xBorderLeft)
                // currY = (combinedValue).times(Calibrator.flowFactorY).plus(yBorderBottom)

                // for testing purposes

                currX = coordinatesBubble1.first.plus(100).toDouble()
                currY = 700.0

                lifecycleScope.launch {
                    moveLeaves(currX, currY, particlesMain)
                    moveLeaves(currX, currY, particlesSupprt)
                }

                selectionDetected =
                    inBubble(coordinatesBubble1) || inBubble(coordinatesBubble2) || inBubble(coordinatesBubble3)

                val prevValue = breathingUtils.smoothValue()

                // prevAbdo = prevValue.first
                // prevThor = prevValue.second

                currX = coordinatesBubble1.first.plus(100).toDouble()
                currY = 700.0

                prevX = currX
                prevY = currY
            }
        }
    }

    private fun stopLeaves() {
        particlesMain.stopEmitting()
        particlesSupprt.stopEmitting()
    }

    private fun moveLeaves(xValue: Double, yValue: Double, particleSystem: ParticleSystem) {
        val x = floor(xValue)
        val y = floor(yValue)
        if (x in xBorderLeft.toDouble()..xBorderRight.toDouble() && y in yBorderTop.toDouble()..yBorderBottom.toDouble()) {
            particleSystem.updateEmitPoint((abs(prevX.minus(currX))).toInt(), (abs(prevY.minus(currY))).toInt())
            particleSystem.updateEmitPoint(x.toInt(), y.toInt())
        } else if (y in yBorderTop.toDouble()..yBorderBottom.toDouble()) {
            if (x < xBorderLeft.toDouble()) {
                particleSystem.updateEmitPoint(xBorderLeft, (abs(prevY.minus(currY))).toInt())
                particleSystem.updateEmitPoint(xBorderLeft, y.toInt())
            }
            if (x > xBorderRight.toDouble()) {
                particleSystem.updateEmitPoint(xBorderRight, (abs(prevY.minus(currY))).toInt())
                particleSystem.updateEmitPoint(xBorderRight, y.toInt())
            }
        } else if (x in xBorderLeft.toDouble()..xBorderRight.toDouble()) {
            if (y > yBorderBottom.toDouble()) {
                particleSystem.updateEmitPoint((abs(prevX.minus(currX))).toInt(), yBorderBottom)
                particleSystem.updateEmitPoint(x.toInt(), yBorderBottom)
            }
            if (y < yBorderTop.toDouble()) {
                particleSystem.updateEmitPoint((abs(prevX.minus(currX))).toInt(), yBorderTop)
                particleSystem.updateEmitPoint(x.toInt(), yBorderTop)
            }
        }
    }

    private fun changeScreen(
        coordinatesBubble1: Pair<Int, Int>,
        coordinatesBubble2: Pair<Int, Int>,
        coordinatesBubble3: Pair<Int, Int>
    ) {
        thread(start = true, isDaemon = true) {
            while (!holdBreathGesture.hold) {
                //TODO implement changing screens
                Log.i("concurrency", "detectSelection running")
                if (selectionDetected) {
                    holdBreathGesture.resumeDetection()
                    if (inBubble(coordinatesBubble1)) {

                        holdBreathGesture.borderAbdo = Calibrator.holdBreathBufferOutAbdo
                        holdBreathGesture.borderThor = Calibrator.holdBreathBufferOutThor

                        bubble1Selected = true
                        bubble2Selected = false
                        bubble3Selected = false

                        markSelection(bubble1, 1.0f)

                    }
                    if (inBubble(coordinatesBubble2)) {

                        holdBreathGesture.borderAbdo = Calibrator.holdBreathBufferInAbdo
                        holdBreathGesture.borderThor = Calibrator.holdBreathBufferInThor

                        bubble1Selected = false
                        bubble2Selected = true
                        bubble3Selected = false

                        markSelection(bubble2, 1.0f)
                    }
                    if (inBubble(coordinatesBubble3)) {
                        holdBreathGesture.borderAbdo = Calibrator.holdBreathBufferInAbdo
                        holdBreathGesture.borderThor = Calibrator.holdBreathBufferInThor

                        bubble1Selected = false
                        bubble2Selected = false
                        bubble3Selected = true

                        markSelection(bubble3, 1.0f)
                    }
                } else {
                    holdBreathGesture.stopDetection()
                    markSelection(bubble1, 0.7f)
                    markSelection(bubble2, 0.7f)
                    markSelection(bubble3, 0.7f)
                }
                Thread.sleep(5)
            }
        }
    }

    private fun stopActivity() {
        stop = true
        holdBreathGesture.stopDetection()
        stopLeaves()
    }

    private fun changeToAboutScreen() {
        thread(start = true, isDaemon = true) {
            while (!holdBreathGesture.hold || !bubble1Selected) {
                Log.i("concurrency", "changeToAboutScreen running")
                continue
            }

            stopActivity()
            bubble1Selected = false

            Intent(this, AboutScreen::class.java).also { intent ->
                intent.putExtra("Intent", serviceIntent)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_down_top, R.anim.slide_down_bottom)
            }

        }
    }

    private fun changeToGameScreen() {
        thread(start = true, isDaemon = true) {
            while (!holdBreathGesture.hold || !bubble3Selected) {
                Log.i("concurrency", "changeToGameScreen running")
                continue
            }

            stopActivity()
            bubble2Selected = false

            Intent(this, CalibrationScreenActivity::class.java).also { intent ->
                intent.putExtra("Intent", serviceIntent)
                startActivity(intent)
                overridePendingTransition(
                    R.anim.slide_down_top, R.anim.slide_down_bottom
                )
            }

        }
    }

    private fun changeToCalibrationScreen() {
        thread(start = true, isDaemon = true) {
            while (!holdBreathGesture.hold || !bubble2Selected) {
                Log.i("concurrency", "changeToCalibrationScreen running")
                continue
            }

            stopActivity()
            bubble3Selected = false

            Intent(this, GameScreen::class.java).also { intent ->
                intent.putExtra("Intent", serviceIntent)
                startActivity(intent)
            }

        }
    }

    private fun inBubble(coordinatesBubble: Pair<Int, Int>): Boolean {
        return currX in coordinatesBubble.first.toDouble()..coordinatesBubble.second.toDouble()
    }

    private fun markSelection(imageView: ImageView, alpha: Float) {
        runOnUiThread {
            imageView.alpha = alpha
        }
    }
}