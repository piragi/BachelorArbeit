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
import androidx.lifecycle.lifecycleScope
import com.example.breathingmeditationandroid.gestures.HoldBreathGesture
import com.plattysoft.leonids.ParticleSystem
import kotlinx.coroutines.launch
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.floor

class HomeScreenActivity : ComponentActivity() {

    private lateinit var particlesMain: ParticleSystem
    private lateinit var particlesSupprt: ParticleSystem
    private lateinit var serviceIntent: Intent
    private lateinit var mService: BluetoothConnection
    private lateinit var breathingUtils: BreathingUtils

    private lateinit var bubble1: ImageView
    private lateinit var bubble2: ImageView
    private lateinit var bubble3: ImageView

    private lateinit var coordinatesBubble1: Pair<Int, Int>
    private lateinit var coordinatesBubble2: Pair<Int, Int>
    private lateinit var coordinatesBubble3: Pair<Int, Int>

    private val xBorderLeft = ScreenUtils.xBorderLeft //TODO relative to device
    private val xBorderRight = ScreenUtils.xBorderRight
    private val yBorderTop = ScreenUtils.yBorderTop
    private val yBorderBottom = ScreenUtils.yBorderBottom

    private var currX: Double = 0.0
    private var currY: Double = 0.0
    private var prevX: Double = 0.0
    private var prevY: Double = 0.0

    private var prevAbdo: Double = 0.0
    private var prevThor: Double = 0.0

    private var selectionDetected: Boolean = false

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

        bubble1 = findViewById(R.id.bubble1)
        bubble2 = findViewById(R.id.bubble2)
        bubble3 = findViewById(R.id.bubble3)

        Log.i("init", "create")

        currX = xBorderLeft.toDouble()
        currY = yBorderBottom.toDouble()
    }

    override fun onStart() {
        super.onStart()
        Log.i("init", "start")
        serviceIntent = intent?.extras?.getParcelable("Intent")!!
        applicationContext.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(serviceIntent)
    }

    private fun start() {
        initializeUIResources()
        animateLeaves()
        detectScreenChange()
        holdBreathGesture.detect()
    }

    private fun initializeUIResources() {
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
        coordinatesBubble1 = Pair(bubble1.left, bubble1.right)
        coordinatesBubble2 = Pair(bubble2.left, bubble2.right)
        coordinatesBubble3 = Pair(bubble3.left, bubble3.right)
    }

    private fun animateLeaves() {
        thread(start = true, isDaemon = true) {
            val currVal = breathingUtils.smoothValue()
            prevAbdo = currVal.first
            prevThor = currVal.second
            while (!stop) {
                Log.i("coordinates", "$coordinatesBubble1, $coordinatesBubble2, $coordinatesBubble3")
                val currValue = breathingUtils.smoothValue()
                val combinedValue = breathingUtils.calcCombinedValue(currValue.first, currValue.second)

                currX = (combinedValue).times(Calibrator.flowFactorX).plus(xBorderLeft)
                currY = (combinedValue).times(Calibrator.flowFactorY).plus(yBorderBottom)

                lifecycleScope.launch {
                    detectSelection()
                    moveLeaves(currX, currY, particlesMain)
                    moveLeaves(currX, currY, particlesSupprt)
                }

                val prevValue = breathingUtils.smoothValue()

                prevAbdo = prevValue.first
                prevThor = prevValue.second

                prevX = currX
                prevY = currY
                Thread.sleep(2)
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

    private fun detectSelection() {
        selectionDetected =
            inBubble(coordinatesBubble1) || inBubble(coordinatesBubble2) || inBubble(coordinatesBubble3)
        if (selectionDetected) {
            holdBreathGesture.resumeDetection()
            if (inBubble(coordinatesBubble1)) {

                holdBreathGesture.borderAbdo = Calibrator.holdBreathBufferOutAbdo
                holdBreathGesture.borderThor = Calibrator.holdBreathBufferOutThor

                markSelection(bubble1, 1.0f)

            }
            if (inBubble(coordinatesBubble2)) {

                holdBreathGesture.borderAbdo = Calibrator.holdBreathBufferInAbdo
                holdBreathGesture.borderThor = Calibrator.holdBreathBufferInThor

                markSelection(bubble2, 1.0f)
            }
            if (inBubble(coordinatesBubble3)) {
                holdBreathGesture.borderAbdo = Calibrator.holdBreathBufferInAbdo
                holdBreathGesture.borderThor = Calibrator.holdBreathBufferInThor

                markSelection(bubble3, 1.0f)
            }
        } else {
            holdBreathGesture.stopDetection()

            markSelection(bubble1, 0.7f)
            markSelection(bubble2, 0.7f)
            markSelection(bubble3, 0.7f)
        }
    }

    private fun stopActivity() {
        stop = true
        holdBreathGesture.stopDetection()
        stopLeaves()
    }


    //TODO bugfix in screen change
    private fun detectScreenChange() {
        thread(start = true, isDaemon = true) {
            while (!holdBreathGesture.hold)
                continue
            if (bubble1Selected)
                changeToAboutScreen()
            else if (bubble2Selected)
                changeToCalibrationScreen()
            else changeToGameScreen()
        }
    }

    private fun changeToAboutScreen() {
        stopActivity()
        bubble1Selected = false
        Intent(this, AboutScreen::class.java).also { intent ->
            intent.putExtra("Intent", serviceIntent)
            startActivity(intent)
            overridePendingTransition(
                R.anim.slide_down_top, R.anim.slide_down_bottom
            )
        }
    }

    private fun changeToGameScreen() {
        stopActivity()
        bubble3Selected = false
        Intent(this, GameScreen::class.java).also { intent ->
            intent.putExtra("Intent", serviceIntent)
            startActivity(intent)
        }
    }

    private fun changeToCalibrationScreen() {
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

    private fun inBubble(coordinatesBubble: Pair<Int, Int>): Boolean {
        return currX in coordinatesBubble.first.toDouble()..coordinatesBubble.second.toDouble()
    }

    private fun markSelection(imageView: ImageView, alpha: Float) {
        bubble1Selected = imageView.tag.equals("bubble1")
        bubble2Selected = imageView.tag.equals("bubble2")
        bubble3Selected = imageView.tag.equals("bubble3")

        if (bubble1Selected && bubble2Selected && bubble3Selected) {
            bubble1Selected = false
            bubble2Selected = false
            bubble3Selected = false
        }

        runOnUiThread {
            imageView.alpha = alpha
        }
    }
}