package com.example.breathingmeditationandroid.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.breathingmeditationandroid.*
import com.example.breathingmeditationandroid.gestures.*
import com.example.breathingmeditationandroid.utils.BreathingUtils
import com.example.breathingmeditationandroid.utils.SelectionUtils
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
    private lateinit var holdBreathGesture: HoldBreathGesture
    private lateinit var deepBreathLevel: DeepBreathLevel
    private lateinit var birdsEmergingLevel: BirdsEmerging
    private lateinit var pause: GamePause

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
            holdBreathGesture = HoldBreathGesture(mService, 5000.0)
            deepBreathLevel = DeepBreathLevel(snow, this@GameScreen)
            birdsEmergingLevel = BirdsEmerging(this@GameScreen)
            // birdsEmergingLevel.animationStart()
            startLevel()

        }
    }

    private fun startLevel() {
        thread(start = true, isDaemon = true) {
            try {
                lifecycleScope.launch {
                    // deepBreathLevel.animationStart()
                    listenForPause()
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


    //TODO oefter als 1x
    private fun listenForPause() {
        thread(start = true, isDaemon = true) {
            holdBreathGesture.detect()
            var launched = false
            while (true) {
                if (holdBreathGesture.hold && !launched) {
                    Log.i("pause", "paused")
                    launched = true
                    lifecycleScope.launch {
                        pauseGame()
                    }
                }
                if (launched && this@GameScreen::pause.isInitialized) {
                    if (pause.end) {
                        Log.i("pause", "ended")
                        changeToHomeScreen()
                    } else if (pause.resume) {
                        Log.i("pause", "resumed")
                        resumeGame()
                        launched = false
                    }
                }
            }
        }
    }

    private fun changeToHomeScreen() {
        pause.stopAll()
        holdBreathGesture.stopDetection()
        Intent(this, HomeScreenActivity::class.java).also { intent ->
            intent.putExtra("Intent", serviceIntent)
            startActivity(intent)
            overridePendingTransition(R.anim.fadeout, R.anim.fadein)
            finish()
        }
    }

    private fun pauseGame() {
        pause = GamePause(this@GameScreen, HoldBreathGesture(mService, 5000.0), breathingUtils)
        pause.pauseGame()
    }

    private fun resumeGame() {
        holdBreathGesture = HoldBreathGesture(mService, 5000.0)
        holdBreathGesture.detect()
        pause.resumeGame()
    }
}