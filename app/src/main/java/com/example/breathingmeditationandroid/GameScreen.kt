package com.example.breathingmeditationandroid

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
    private lateinit var deepBreathLevel: DeepBreathLevel
    private lateinit var birdsEmergingLevel: BirdsEmerging
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
            deepBreathLevel = DeepBreathLevel(findViewById<View>(R.id.snow) as ImageView, this@GameScreen)
            birdsEmergingLevel = BirdsEmerging(this@GameScreen)
            birdsEmergingLevel.animationStart()
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
}