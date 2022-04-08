package com.example.breathingmeditationandroid

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.*
import android.util.Log
import android.widget.Toast
import com.hexoskin.hsapi_android.*
import com.hexoskin.resp_drift_correction.Corrector
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.concurrent.thread

//inspired by: https://developer.android.com/guide/components/services
class BluetoothConnection : Service(), HexoskinDataListener, HexoskinLogListener, HexoskinCommandWriter {

    //Binder for clients
    private val binder: LocalBinder = LocalBinder()

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

    //Corrector
    private var mCorrector: Corrector = Corrector()

    //Hexoskin specifics
    private var mKeepAliveTimer: Timer? = null
    private var mHexoskinAPI: HexoskinAPI? = null

    //Values
    var mSteps: String = "0"
    private var mHr: String = "0"
    private var mBr: String = "0"
    private var mCadence: String = "0"
    private var mMv: String = "0"
    private var mAct: String = "0"
    var mThorRaw: String = "0"
    var mAbdoCorrected: String = "0"

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothConnection = this@BluetoothConnection
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }


    private inner class ServiceHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            //we get a message from the thread
            try {
                //sending the received data to the activity we are viewing rn
                //thread has to loop till quit signal and send data to the activity we are in

            } catch (e: InterruptedException) {
                //Restore interrupt status
                Thread.currentThread().interrupt()
            }

            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            //stopSelf(msg.arg1)
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
        //connect to the Hexoskin
        try {
            mDevice = intent?.extras?.getParcelable("Device")
            mCorrector.init()
            disconnected()
            mSocket = mDevice?.createRfcommSocketToServiceRecord(uuid)
            mSocket?.connect()
            mInput = mSocket?.inputStream
            mOutput = mSocket?.outputStream

            listenBluetoothIncomingData()

            mHexoskinAPI = HexoskinAPI(this, this, this)
            mHexoskinAPI!!.Init()

            mHexoskinAPI!!.enableBluetoothTransmission()
            mHexoskinAPI!!.setRealTimeMode(true, false, true, true, true)

            //The hexoskin only keeps bluetooth transmission for 1 minute. After that it disable
            //the bluetooth transmission. This is for Hexoskin device to continue to transmit data
            mKeepAliveTimer = Timer()
            mKeepAliveTimer!!.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    mHexoskinAPI?.let {
                        mHexoskinAPI!!.enableBluetoothTransmission()
                    }
                }
            }, 0, (45 * 1000).toLong())

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

    private fun listenBluetoothIncomingData() {

        //disconnected()
        thread(start = true) {
            val buffer = ByteArray(512)
            while (!Thread.interrupted()) {
                try {
                    val length = mInput!!.read(buffer, 0, 512)
                    if (length < 1) continue
                    val data = buffer.copyOfRange(0, length)
                    mHexoskinAPI!!.decode(data)

                } catch (e: IOException) {
                    e.printStackTrace()
                    break
                }
            }
        }
    }

    private fun disconnected() {

        if (mSocket?.isConnected == true) {
            try {
                mSocket?.close()
            } catch (e1: IOException) {
                e1.printStackTrace()
            }
        }

        try {
            mInput?.close()
            mOutput?.close()
            mKeepAliveTimer?.cancel()
            mKeepAliveTimer?.purge()
        } catch (e1: IOException) {
            e1.printStackTrace()
        }

        mReader?.let {
            mReader!!.interrupt()
            mReader = null
        }
        mHexoskinAPI?.let {
            mHexoskinAPI!!.Uninit()
            mHexoskinAPI = null
        }
    }

    override fun onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show()
    }

    override fun onData(type: HexoskinDataType?, time: Long, value: Int, status: EnumSet<HexoskinDataStatus>?) {
        //Corrector
        type?.let {
            when (type) {
                HexoskinDataType.INSPIRATION -> mCorrector.addInspiration(time, mHexoskinAPI!!.currentSessionStartTime)
                HexoskinDataType.EXPIRATION -> mCorrector.addExpiration(time, mHexoskinAPI!!.currentSessionStartTime)
                HexoskinDataType.RESP_CIRCUIT_TEMPERATURE -> {
                    val converted: Float =
                        mHexoskinAPI!!.hexoskin_sample_conversion(HexoskinDataType.RESP_CIRCUIT_TEMPERATURE, value)
                    mCorrector.addRespTemperature(time, mHexoskinAPI!!.currentSessionStartTime, converted.toDouble())
                }
                HexoskinDataType.STEP -> mSteps = "$value"
                HexoskinDataType.HEART_RATE -> mHr = "$value"
                HexoskinDataType.BREATHING_RATE -> mBr = "$value"
                HexoskinDataType.CADENCE -> mCadence = "$value"
                HexoskinDataType.MINUTE_VENTILATION -> mMv = "$value"
                HexoskinDataType.ACTIVITY -> mAct = "$value"
                else -> return
            }
        }


    }

    override fun onRawData(type: HexoskinDataType?, time: Long, values: Array<out IntArray>?) {

        val dataString = StringBuilder()
        values?.let {
            for (array in values) {
                dataString.append("$time-")
                for (value in array) {
                    dataString.append("$value ")
                }
            }

            if (type == HexoskinDataType.RAW_RESP) {
                for (i in values[0].indices) {
                    val thor = values[0][i]
                    val abdo = values[1][i]

                    //adding respiration values
                    val adjustedTimestamp =
                        mCorrector.addRespiration(time + i * 2, mHexoskinAPI!!.currentSessionStartTime, thor, abdo)

                    if (adjustedTimestamp >= 0) {
                        val correction = mCorrector.getCorrectedRespiration(adjustedTimestamp)
                        mAbdoCorrected = correction.abdominal.toString()
                    }
                }
                mThorRaw = values[0][0].toString()
            }
        }
        return
    }

    override fun onLog(logLevel: Int, logTxt: String) {
        Log.println(logLevel, "HexoskinAPI", logTxt)
    }

    override fun getLogLevel(): Int {
        return Log.DEBUG
    }

    override fun write(cmd: ByteArray?) {
        try {
            mOutput?.write(cmd)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}