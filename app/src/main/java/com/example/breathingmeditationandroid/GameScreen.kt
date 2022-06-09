package com.example.breathingmeditationandroid

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.Image
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.breathingmeditationandroid.gestures.*
import com.plattysoft.leonids.ParticleSystem
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.concurrent.thread


class GameScreen : ComponentActivity() {
    //Binding service
    private lateinit var serviceIntent: Intent
    private lateinit var mService: BluetoothConnection

    private lateinit var breathingUtils: BreathingUtils
    private lateinit var deepAbdoBreathGesture: DeepAbdoBreathGesture
    private lateinit var deepThorBreathGesture: DeepThorBreathGesture
    private lateinit var staccatoBreathGesture: StaccatoBreathGesture
    private lateinit var sighBreathGesture: SighBreathGesture
    private lateinit var breathHoldGesture: HoldBreathGesture
    private lateinit var deepBreathLevel: DeepBreathLevel
    private lateinit var birdsEmergingLevel: BirdsEmerging

    private lateinit var snow: ImageView

    //TODO: Global irgendwo definieren für alle activites?
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothConnection.LocalBinder
            mService = binder.getService()
            startGame()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService.stopService(intent)
            Log.i("BluetoothService", "disconnected")
            return
        }
    }

    //View starten
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("gamescreeen", "started")

        //view
        setContentView(R.layout.game_screen)
        snow = findViewById<View>(R.id.snow) as ImageView

        //bind service to activity
        serviceIntent = intent?.extras?.getParcelable("Intent")!!
        applicationContext.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("onDestroy", "innit")
        stopService(serviceIntent)
    }

    fun startGame() {
        GlobalScope.launch {
            breathingUtils = BreathingUtils(mService)
            deepAbdoBreathGesture = DeepAbdoBreathGesture(mService, breathingUtils)
            deepThorBreathGesture = DeepThorBreathGesture(mService, breathingUtils)
            staccatoBreathGesture = StaccatoBreathGesture(mService, breathingUtils)
            sighBreathGesture = SighBreathGesture(mService, breathingUtils)
            breathHoldGesture = HoldBreathGesture(mService, 5000.0)
            deepBreathLevel = DeepBreathLevel(snow, this@GameScreen)
            birdsEmergingLevel = BirdsEmerging(this@GameScreen)
            birdsEmergingLevel.animationStart()
            startLevel()
        }
    }

    private fun startLevel() {
        thread(start = true, isDaemon = true) {
            try {
                lifecycleScope.launch {
                    deepBreathLevel.animationStart()
                    val detectedBreathHold = breathHoldGesture.detected()
                    if (detectedBreathHold.await()) {
                        onPause()
                    }

//                    val detectedThorBreathGesture = deepThorBreathGesture.detected()
//                    val detectedAbdoBreathGesture = deepAbdoBreathGesture.detected()
//                    val detectedStaccatoBreathGesture = staccatoBreathGesture.detected()
//                    val detectedSighBreathGesture = sighBreathGesture.detected()
//
//                    if ( detectedStaccatoBreathGesture.await()) {
//                        deepBreathLevel.animationStart()
//                    } else if (detectedAbdoBreathGesture.await()) {
//                        deepBreathLevel.animationStart()
//                    } else if (detectedThorBreathGesture.await()) {
//                        deepBreathLevel.animationStart()
//                    } else if (detectedSighBreathGesture.await()) {
//                        deepBreathLevel.animationStart()
//                    }
                }
            } catch (consumed: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    private fun pausGame() {
        val background = findViewById<RelativeLayout>(R.id.game_screen)
        val resumeBubble = findViewById<ImageView>(R.id.resumeBubble)
        val endBubble = findViewById<ImageView>(R.id.endBubble)
        val resumeText = findViewById<TextView>(R.id.resume)
        val endText = findViewById<TextView>(R.id.end)


        //TODO stop detecting other breathing gestures
        runOnUiThread {
            background.alpha = 0.5f
            resumeBubble.alpha = 0.7f
            endBubble.alpha = 0.7f
            resumeText.alpha = 1.0f
            endText.alpha = 1.0f
        }
        lifecycleScope.launch {
            animateLeaves()
        }
    }

    private fun animateLeaves() {
        thread(start = true, isDaemon = true) {
            val particlesMain = initializeParticles()
            val particlesSupport = initializeParticles()
        }
    }
    private fun initializeParticles(): ParticleSystem {
        val particles = ParticleSystem(this, 10, R.drawable.leaf2, 1000)
        particles.setScaleRange(0.7f, 1.3f)
            .setSpeedRange(0.05f, 0.1f)
            .setRotationSpeedRange(50f, 120f)
            .setFadeOut(500, AccelerateInterpolator())
            .emit(ScreenUtils.xBorderLeft, ScreenUtils.yBorderBottom, 10)
        return particles
    }
}