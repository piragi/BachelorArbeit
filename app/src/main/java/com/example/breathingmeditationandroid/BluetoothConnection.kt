package com.example.breathingmeditationandroid

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.*
import android.widget.Toast
import com.hexoskin.hsapi_android.HexoskinAPI
import com.hexoskin.hsapi_android.HexoskinDataListener
import com.hexoskin.hsapi_android.HexoskinDataStatus
import com.hexoskin.hsapi_android.HexoskinDataType
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.concurrent.thread

//inspired by: https://developer.android.com/guide/components/services
class BluetoothConnection : Service(), HexoskinDataListener {

    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    //service
    private var serviceLoop: Looper? = null
    private var serviceHandler: ServiceHandler? = null

    //Bluetooth
    private var mDevice: BluetoothDevice? = null
    private var mSocket: BluetoothSocket? = null
    private var mOutput: OutputStream? = null
    private var mInput: InputStream? = null

    //Reader
    private var mReader: Thread? = null

    //Hexoskin specifics
    private var mKeepAliveTimer: Timer? = null
    private var mHexoskinAPI: HexoskinAPI? = null


    private inner class ServiceHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            //we get a message from the thread
            try {
                //sending the received data to the activity we are viewing rn
                Thread.sleep(100000)
            } catch (e: InterruptedException) {
                //Restore interrupt status
                Thread.currentThread().interrupt()
            }

            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1)
        }

    }

    override fun onCreate() {
        //Start up Thread running the service
        HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND).apply {
            start()

            serviceLoop = looper
            serviceHandler = ServiceHandler(looper)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show()

        //connect to the Hexoskin
        try {
            mSocket = mDevice?.createRfcommSocketToServiceRecord(uuid)
            mSocket?.connect()
            mInput = mSocket?.inputStream
            mOutput = mSocket?.outputStream

            //listenBluetoothIncomingData()

            Toast.makeText(this, "connected", Toast.LENGTH_SHORT).show()

        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Unable to connect, try again", Toast.LENGTH_LONG).show()
        }

        //For each start request send a message to start a job and deliver the start ID
        //so we know which request we're stopping when we finish the job
        serviceHandler?.obtainMessage()?.also { msg ->
            msg.arg1 = startId
            serviceHandler?.sendMessage(msg)
        }

        // If we get killed, after returning from here, restart
        return START_STICKY
    }

    fun listenBluetoothIncomingData() {
        mReader = thread {
            val buffer = ByteArray(512)
            while (!Thread.interrupted()) {
                try {
                    val length = mInput!!.read(buffer, 0, 512)
                    if (length < 1) continue
                    val data = Arrays.copyOfRange(buffer, 0, length)
                    mHexoskinAPI!!.decode(data)
                } catch (e: IOException) {
                    e.printStackTrace()
                    break
                }
            }
        }
    }

    fun disconnected() {
        if (mSocket != null && mSocket!!.isConnected) {
            try {
                mSocket!!.close()
            } catch (e1: IOException) {
                e1.printStackTrace()
            }
        }
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show()
    }

    override fun onData(p0: HexoskinDataType?, p1: Long, p2: Int, p3: EnumSet<HexoskinDataStatus>?) {
        TODO("Not yet implemented")
    }

    override fun onRawData(p0: HexoskinDataType?, p1: Long, p2: Array<out IntArray>?) {
        TODO("Not yet implemented")
    }

}