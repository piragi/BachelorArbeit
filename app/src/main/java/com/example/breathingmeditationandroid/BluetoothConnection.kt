package com.example.breathingmeditationandroid

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.*
import android.util.Log
import android.widget.Toast
import com.hexoskin.hsapi_android.*
import com.hexoskin.resp_drift_correction.Corrector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*
import kotlin.concurrent.thread

//inspired by: https://developer.android.com/guide/components/services
class BluetoothConnection : Service(), HexoskinDataListener, HexoskinLogListener, HexoskinCommandWriter {

    //Binder for clients
    private val binder: LocalBinder = LocalBinder()
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    //Bluetooth
    private var mDevice: BluetoothDevice? = null
    private var mSocket: BluetoothSocket? = null
    //Corrector
    private var mCorrector: Corrector = Corrector()
    //Hexoskin specifics
    private var mKeepAliveTimer: Timer? = null
    private var mHexoskinAPI: HexoskinAPI? = null
    //Values
    private var mThorRaw: Int = 0
    private var mAbdoRaw: Int = 0
    var mAbdoCorrected: Double = 0.0
    var mThorCorrected: Double = 0.0

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothConnection = this@BluetoothConnection
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }


    override fun onCreate() {
        super.onCreate()
        disconnected()
        mCorrector.uninit()
    }

    @SuppressLint("MissingPermission") //TODO: why did this come up? was no problem before ??
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //TODO: remove helperThis
        //TODO: optimize coroutine
        val helperThis = this
            GlobalScope.launch(Dispatchers.Default) {
                //connect to the Hexoskin
                try {
                    mDevice = intent?.extras?.getParcelable("Device")
                    mCorrector.init()
                    disconnected()

                    mSocket = mDevice?.createRfcommSocketToServiceRecord(uuid)
                    mSocket?.connect()

                    listenBluetoothIncomingData()

                    mHexoskinAPI = HexoskinAPI(helperThis, helperThis, helperThis)
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
                    Log.i("crash:", "bluetoothconnection")
                }
            }
        // If we get killed, after returning from here, restart
        return START_STICKY
    }

    private fun listenBluetoothIncomingData() {

        //TODO: thread crashes when getting to landscape, nullpointerException
        thread(start = true, name = "listenBluetoothIncomingData") {
            val buffer = ByteArray(512)
            while (!Thread.interrupted()) {
                try {
                    val length = mSocket!!.inputStream!!.read(buffer, 0, 512)
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
            mSocket?.inputStream?.close()
            mSocket?.outputStream?.close()
            mKeepAliveTimer?.cancel()
            mKeepAliveTimer?.purge()
        } catch (e1: IOException) {
            e1.printStackTrace()
        }
        mHexoskinAPI?.let {
            mHexoskinAPI!!.Uninit()
            mHexoskinAPI = null
        }
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
                        mAbdoCorrected = correction.abdominal
                        mThorCorrected = correction.thorasic
                    }
                }
                mThorRaw = values[0][0]
                mAbdoRaw = values[1][0]
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
            mSocket!!.outputStream!!.write(cmd)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnected()
        // Corrector has to be uninitialized
        mCorrector.uninit()
        Toast.makeText(this, "disconnected", Toast.LENGTH_SHORT).show()

    }

}

