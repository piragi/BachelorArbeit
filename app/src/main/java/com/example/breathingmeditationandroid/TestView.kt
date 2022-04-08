package com.example.breathingmeditationandroid

import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import kotlin.concurrent.thread

class TestView : ComponentActivity() {

    //View
    private lateinit var textSteps: TextView
    private lateinit var textAbdominalCorrected: TextView
    private lateinit var textThorasicRaw: TextView


    //Bluetooth Connection
    private var mDevice: BluetoothDevice? = null

    //Binding service
    private lateinit var mService: BluetoothConnection
    private var mBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothConnection.LocalBinder
            mService = binder.getService()
            mBound = true

            updateView()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            return
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //setup View
        setContentView(R.layout.activity_device)
        textSteps = findViewById<View>(R.id.textViewStepValue) as TextView
        textAbdominalCorrected = findViewById<View>(R.id.textViewAbdoRawValue) as TextView
        textThorasicRaw = findViewById<View>(R.id.textViewThorRawValue) as TextView

        //setup and start bluetooth service
        mDevice = intent?.extras?.getParcelable("Device")
        Intent(applicationContext, BluetoothConnection::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
            intent.putExtra("Device", mDevice)
            startService(intent)
        }
    }

    fun preview() {

    }

    fun updateView() {
        thread(start = true) {

            while (true) {
                Thread.sleep(75)
                runOnUiThread {
                    with(mService) {
                        textSteps.text = mSteps
                        textAbdominalCorrected.text = mAbdoCorrected
                        textThorasicRaw.text = mThorRaw
                    }
                }
            }
        }
    }

}