package com.example.breathingmeditationandroid

import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.widget.ImageView
import androidx.activity.ComponentActivity

class HomeScreenActivity: ComponentActivity() {
    private lateinit var background: ImageView
    //Bluetooth Connection
    private var mDevice: BluetoothDevice? = null
    private var wind = WindDrawable(applicationContext);
    //Binding service
    private lateinit var mService: BluetoothConnection
    private var mBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothConnection.LocalBinder
            mService = binder.getService()
            mBound = true
            setContentView(wind)
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            mService.stopService(intent)
            mBound = false
            return
        }
    }
}