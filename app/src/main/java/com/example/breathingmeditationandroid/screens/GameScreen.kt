package com.example.breathingmeditationandroid.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
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
import kotlinx.coroutines.delay
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
            holdBreathGesture = HoldBreathGesture(mService, 3000.0)
            deepBreathLevel =
                DeepBreathLevel(findViewById<View>(R.id.snow) as ImageView, this@GameScreen)
            birdsEmergingLevel = BirdsEmerging(this@GameScreen)
            feedbackTrees = FeedbackTrees(this@GameScreen, mService)
            rocketTakeOff = RocketTakeOff(background.height, this@GameScreen)
            cloudsFadeOut = CloudsFadeOut(this@GameScreen)
            startLevel()
        }
    }

    private fun startLevel() {
        thread(start = true, isDaemon = true) {
            try {
                lifecycleScope.launch {
                    listenForPause()
                    GlobalScope.launch {
                        detectBirdsEmerging()
                    }
                    GlobalScope.launch {
                        detectDeepBreathLevel()
                    }
                    GlobalScope.launch {
                        detectCloudsLevel()
                    }
                    GlobalScope.launch {
                        detectRocketLevel()
                    }
//                    if ( detectedStaccatoBreathGesture.await()) {
//                        birdsEmergingLevel.animationStart()
//                    } else if (detectedAbdoBreathGesture.await()) {
//                        deepBreathLevel.animationStart()
//                    } else if (detectedThorBreathGesture.await()) {
//                        //fadeout clouds
//                        cloudsFadeOut.animationStart()
//                    } else if (detectedSighBreathGesture.await()) {
//                        rocketTakeOff.startAnimation()
//                    }

                }
            } catch (consumed: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    private suspend fun detectBirdsEmerging() {
        val detectedStaccatoBreathGesture = staccatoBreathGesture.detect()
        if (detectedStaccatoBreathGesture.await()) {
            birdsEmergingLevel.animationStart()
            detectBirdsEmerging()
        }
    }

    //TODO fix animations
    private suspend fun detectDeepBreathLevel() {
        val detectedAbdoBreathGesture = deepAbdoBreathGesture.detect()
        if (detectedAbdoBreathGesture.await()) {
            lifecycleScope.launch {
                deepBreathLevel.animationStart()
            }
            delay(3000)
            lifecycleScope.launch {
                deepBreathLevel.resetView()
            }.join()
            detectDeepBreathLevel()
        }
    }

    private suspend fun detectCloudsLevel() {
        val detectedThorBreathGesture = deepThorBreathGesture.detect()
        if (detectedThorBreathGesture.await()) {
            //fadeout clouds
            cloudsFadeOut.animationStart()
            cloudsFadeOut.resetView()
            detectCloudsLevel()
        }
    }

    private suspend fun detectRocketLevel() {
        val detectedSighBreathGesture = sighBreathGesture.detect()
        if (detectedSighBreathGesture.await()) {
            rocketTakeOff.startAnimation()
            detectRocketLevel()
        }
    }

    private suspend fun listenForInspiration() {
        val inspirationDetected = breathingUtils.detectInspiration()
        if (inspirationDetected.await()) {
            holdBreathGesture.borderAbdo = Calibrator.holdBreathBufferInAbdo
            holdBreathGesture.borderThor = Calibrator.holdBreathBufferInThor
            listenForInspiration()
        }
    }

    private suspend fun listenForExpiration() {
        val expirationDetected = breathingUtils.detectExpiration()
        if (expirationDetected.await()) {
            holdBreathGesture.borderAbdo = Calibrator.holdBreathBufferOutAbdo
            holdBreathGesture.borderThor = Calibrator.holdBreathBufferOutThor
            listenForExpiration()
        }
    }


    //TODO continue here
    private fun listenForPause() {
        GlobalScope.launch {
            actOnPause()
        }
        GlobalScope.launch {
            listenForExpiration()
        }
        GlobalScope.launch {
            listenForInspiration()
        }
    }

    private suspend fun actOnPause() {
        val holdBreathDetected = holdBreathGesture.detect()
        if (holdBreathDetected.await()) {
            lifecycleScope.launch {
                pauseGame()
            }.join()
            val gameResumed = pause.gameResumedAsync()
            changeToHomeScreen()
            if (gameResumed.await()) {
                resumeGame()
                actOnPause()
            }
        }
    }

    private fun changeToHomeScreen() {
        GlobalScope.launch {
            val gameEnded = pause.gameEndedAsync()
            if(gameEnded.await()) {
                Intent(this@GameScreen, HomeScreenActivity::class.java).also { intent ->
                    intent.putExtra("Intent", serviceIntent)
                    startActivity(intent)
                    overridePendingTransition(R.anim.fadein_fast_full, R.anim.fadeout_fast_full)
                    finish()
                }
            }
        }
    }

    private fun pauseGame() {

        deepAbdoBreathGesture.stopDetection()
        deepThorBreathGesture.stopDetection()
        staccatoBreathGesture.stopDetection()
        sighBreathGesture.stopDetection()

        pause = GamePause(this, HoldBreathGesture(mService), breathingUtils)
        pause.pauseGame()
    }

    private fun resumeGame() {
        deepAbdoBreathGesture.resumeDetection()
        deepThorBreathGesture.resumeDetection()
        staccatoBreathGesture.resumeDetection()
        sighBreathGesture.resumeDetection()
        holdBreathGesture = HoldBreathGesture(mService, 3000.0)
    }
}