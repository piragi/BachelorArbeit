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
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.breathingmeditationandroid.*
import com.example.breathingmeditationandroid.gestures.*
import com.example.breathingmeditationandroid.utils.BreathingUtils
import com.example.breathingmeditationandroid.gestures.DeepAbdoBreathGesture
import com.example.breathingmeditationandroid.gestures.DeepThorBreathGesture
import com.example.breathingmeditationandroid.gestures.SighBreathGesture
import com.example.breathingmeditationandroid.gestures.StaccatoBreathGesture
import com.example.breathingmeditationandroid.levels.*
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
    private lateinit var feedbackTrees: FeedbackTrees
    private lateinit var rocketTakeOff: RocketTakeOff
    private lateinit var cloudsFadeOut: CloudsFadeOut

    private lateinit var background: ImageView

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
        background = findViewById<View>(R.id.background) as ImageView

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
            holdBreathGesture = HoldBreathGesture(mService)
            deepBreathLevel = DeepBreathLevel(findViewById<View>(R.id.snow) as ImageView, this@GameScreen)
            birdsEmergingLevel = BirdsEmerging(this@GameScreen)
            birdsEmergingLevel.animationStart()
            feedbackTrees = FeedbackTrees(this@GameScreen, mService)
            rocketTakeOff = RocketTakeOff(background.height, this@GameScreen)
            cloudsFadeOut = CloudsFadeOut(this@GameScreen)
            deepBreathLevel.animationStart()
            startLevel()
        }
    }

    private fun startLevel() {
        thread(start = true, isDaemon = true) {
            try {
                lifecycleScope.launch {
                    listenForPause()
                    val detectedThorBreathGesture = deepThorBreathGesture.detected()
                    val detectedAbdoBreathGesture = deepAbdoBreathGesture.detected()
                    val detectedStaccatoBreathGesture = staccatoBreathGesture.detected()
                    val detectedSighBreathGesture = sighBreathGesture.detected()

                    if ( detectedStaccatoBreathGesture.await()) {
                        birdsEmergingLevel.animationStart()
                    } else if (detectedAbdoBreathGesture.await()) {
                        deepBreathLevel.animationStart()
                    } else if (detectedThorBreathGesture.await()) {
                        //fadeout clouds
                        cloudsFadeOut.animationStart()
                    } else if (detectedSighBreathGesture.await()) {
                        rocketTakeOff.startAnimation()
                    }
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
                if (mService.mExpiration == 0) {
                    holdBreathGesture.borderAbdo = Calibrator.holdBreathBufferInAbdo
                    holdBreathGesture.borderThor = Calibrator.holdBreathBufferInThor
                }
                if (mService.mInspiration == 0) {
                    holdBreathGesture.borderAbdo = Calibrator.holdBreathBufferOutAbdo
                    holdBreathGesture.borderThor = Calibrator.holdBreathBufferOutThor
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
            overridePendingTransition(R.anim.fadein_fast_full, R.anim.fadeout_fast_full)
            finish()
        }
    }

    private fun pauseGame() {
        pause = GamePause(this@GameScreen, HoldBreathGesture(mService), breathingUtils)
        pause.pauseGame()
    }

    private fun resumeGame() {
        holdBreathGesture = HoldBreathGesture(mService)
        holdBreathGesture.detect()
        pause.resumeGame()
    }
}