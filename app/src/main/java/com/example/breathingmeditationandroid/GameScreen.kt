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
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
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
    private lateinit var selectionUtils: SelectionUtils

    private lateinit var whiteBox: ImageView
    private lateinit var resumeBubble: ImageView
    private lateinit var endBubble: ImageView
    private lateinit var resumeText: TextView
    private lateinit var endText: TextView

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
            birdsEmergingLevel.animationStart()
            getUiResources()
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

    private fun listenForPause() {
        pauseGame()
        resumeGame()
    }

    private fun pauseGame() {
        runOnUiThread {
            whiteBox.alpha = 0.5f
            resumeBubble.alpha = 0.7f
            endBubble.alpha = 0.7f
            resumeText.alpha = 1.0f
            endText.alpha = 1.0f
        }
        pause = GamePause(this@GameScreen, breathingUtils, holdBreathGesture, prepareSelection())
        pause.pauseGame()
    }

    private fun resumeGame() {
        thread(start = true, isDaemon = true) {
            while (!pause.end && !pause.resume) {
                continue
            }
            runOnUiThread {
                whiteBox.alpha = 1.0f
                resumeBubble.alpha = 0.0F
                endBubble.alpha = 0.0F
                resumeText.alpha = 0.0F
                endText.alpha = 0.0F
            }
            if (pause.end)
                Intent(this, HomeScreenActivity::class.java).also { intent ->
                    intent.putExtra("Intent", serviceIntent)
                    startActivity(intent)
                }
        }
    }

    private fun prepareSelection(): SelectionUtils {
        val bubbles = arrayOf(
            Pair(endBubble, Pair(endBubble.left, endBubble.right)),
            Pair(resumeBubble, Pair(resumeBubble.left, resumeBubble.right))
        )
        return SelectionUtils(this@GameScreen, breathingUtils, holdBreathGesture, bubbles)
    }

    private fun getUiResources() {
        whiteBox = findViewById(R.id.white_box)
        resumeBubble = findViewById(R.id.resumeBubble)
        endBubble = findViewById(R.id.endBubble)
        resumeText = findViewById(R.id.resume)
        endText = findViewById(R.id.end)
    }
}