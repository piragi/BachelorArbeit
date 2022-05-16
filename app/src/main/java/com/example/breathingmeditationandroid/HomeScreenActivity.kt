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
import com.example.breathingmeditationandroid.gestures.HoldBreathGesture
import com.plattysoft.leonids.ParticleSystem
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.floor

class HomeScreenActivity : ComponentActivity() {

    private lateinit var container: ViewGroup
    private lateinit var particlesMain: ParticleSystem
    private lateinit var particlesSupprt: ParticleSystem
    private var mDevice: BluetoothDevice? = null
    private lateinit var mService: BluetoothConnection
    private var mBound = false
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

    private var stop = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothConnection.LocalBinder
            mService = binder.getService()
            breathingUtils = BreathingUtils(mService)
            mBound = true
            holdBreathGesture = HoldBreathGesture(mService)
            Log.i("Calibration", "start")
            Calibrator.calibrate(mService)
            bubble2.alpha = 1.0f
            initializeParticleSystems()
            animateLeaves()
            holdBreathGesture.detect()

        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService.stopService(intent)
            mBound = false
            return
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stop = false
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
        Log.i("init:", "particles initialized")
    }

    private fun animateLeaves() {
        //TODO weg aus der methode
        val coordinatesBubble1 = Pair(bubble1.left, bubble1.right)
        val coordinatesBubble2 = Pair(bubble2.left, bubble2.right)
        val coordinatesBubble3 = Pair(bubble3.left, bubble3.right)
        detectSelections(coordinatesBubble1, coordinatesBubble2, coordinatesBubble3)
        val currVal = breathingUtils.smoothValue()
        prevAbdo = currVal.first
        prevThor = currVal.second
        thread(start = true, isDaemon = true) {
            breathingUtils.startFromBeginning()
            while (!stop) {
                val currValue = breathingUtils.smoothValue()
                val combinedValue = breathingUtils.calcCombinedValue(currValue.first, currValue.second)

                currX = (combinedValue).times(Calibrator.flowFactorXUp).plus(xBorderLeft)
                currY = (combinedValue).times(Calibrator.flowFactorYUp).plus(yBorderBottom)

                moveLeaves(currX, currY, particlesMain)
                moveLeaves(currX, currY, particlesSupprt)

                selectionDetected =
                    inBubble(coordinatesBubble1) || inBubble(coordinatesBubble2) || inBubble(coordinatesBubble3)
                holdBreathGesture.stop = !selectionDetected

                val prevValue = breathingUtils.smoothValue()

                prevAbdo = prevValue.first
                prevThor = prevValue.second

                prevX = currX
                prevY = currY
            }
        }
    }

    private fun moveLeaves(xValue: Double, yValue: Double, particleSystem: ParticleSystem) {
        val x = floor(xValue)
        val y = floor(yValue)
        if (x in xBorderLeft.toDouble()..xBorderRight.toDouble() && y in yBorderTop.toDouble()..yBorderBottom.toDouble()) {
            //particleSystem.updateEmitPoint(prevX.toInt(), prevY.toInt())
            particleSystem.updateEmitPoint((abs(prevX.minus(currX))).toInt(), (abs(prevY.minus(currY))).toInt())
            particleSystem.updateEmitPoint(x.toInt(), y.toInt())
        } else if (y in yBorderTop.toDouble()..yBorderBottom.toDouble()) {
            if (x < xBorderLeft.toDouble()) {
                //particleSystem.updateEmitPoint(xBorderLeft, prevY.toInt())
                particleSystem.updateEmitPoint(xBorderLeft, (abs(prevY.minus(currY))).toInt())
                particleSystem.updateEmitPoint(xBorderLeft, y.toInt())
            }
            if (x > xBorderRight.toDouble()) {
                //particleSystem.updateEmitPoint(xBorderRight, prevY.toInt())
                particleSystem.updateEmitPoint(xBorderRight, (abs(prevY.minus(currY))).toInt())
                particleSystem.updateEmitPoint(xBorderRight, y.toInt())
            }
        } else if (x in xBorderLeft.toDouble()..xBorderRight.toDouble()) {
            if (y > yBorderBottom.toDouble()) {
                //particleSystem.updateEmitPoint(prevX.toInt(), prevY.toInt())
                particleSystem.updateEmitPoint((abs(prevX.minus(currX))).toInt(), yBorderBottom)
                particleSystem.updateEmitPoint(x.toInt(), yBorderBottom)
            }
            if (y < yBorderTop.toDouble()) {
                //particleSystem.updateEmitPoint(prevX.toInt(), prevY.toInt())
                particleSystem.updateEmitPoint((abs(prevX.minus(currX))).toInt(), yBorderTop)
                particleSystem.updateEmitPoint(x.toInt(), yBorderTop)
            }
        }
    }

    private fun detectSelections(
        coordinatesBubble1: Pair<Int, Int>,
        coordinatesBubble2: Pair<Int, Int>,
        coordinatesBubble3: Pair<Int, Int>
    ) {
        thread(start = true, isDaemon = true) {
            while (true) {
                if (selectionDetected) {
                    if (inBubble(coordinatesBubble1)) {
                        setAlpha(bubble1, 1.0f)
                        if (holdBreathGesture.hold) {
                            Intent(this, AboutScreen::class.java).also { intent ->
                                unbindService(connection)
                                startActivity(intent)
                                stop = true
                            }
                        }
                    }
                    if (inBubble(coordinatesBubble2)) {
                        holdBreathGesture.border = 1.0
                        setAlpha(bubble2, 1.0f)
                    }
                    if (inBubble(coordinatesBubble3)) {
                        holdBreathGesture.border = 3.0
                        if (holdBreathGesture.hold) {
                            Intent(this, GameScreen::class.java).also { intent ->
                                unbindService(connection)
                                startActivity(intent)
                                stop = true
                            }
                        }
                        setAlpha(bubble3, 1.0f)
                    }
                } else {
                    holdBreathGesture.stop = true
                    setAlpha(bubble1, 0.7f)
                    setAlpha(bubble2, 0.7f)
                    setAlpha(bubble3, 0.7f)

                }
                Thread.sleep(10)
            }
        }
    }

    private fun inBubble(coordinatesBubble: Pair<Int, Int>): Boolean {
        return currX in coordinatesBubble.first.toDouble()..coordinatesBubble.second.toDouble()
    }

    private fun setAlpha(imageView: ImageView, alpha: Float) {
        runOnUiThread {
            imageView.alpha = alpha
        }
    }
}