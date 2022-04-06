package com.example.breathingmeditationandroid

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class StartupActivity : AppCompatActivity() {

    private val mDevices = ArrayList<BluetoothDevice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        //Instance State
        super.onCreate(savedInstanceState)

        //View erstellen
        setContentView(R.layout.activity_device_list)
        val _recyclerView: RecyclerView = findViewById(R.id.my_recycler_view)
        _recyclerView.setHasFixedSize(true)
        val _layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        _recyclerView.layoutManager = _layoutManager

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH), 0)

        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices

        //TODO: change the null check
        if (pairedDevices != null) {
            if (pairedDevices.isNotEmpty()) {
                mDevices.addAll(pairedDevices)
            }
        }

        val _adapter = DevicesAdapter()
        _recyclerView.adapter = _adapter

    }

    private inner class DevicesAdapter : RecyclerView.Adapter<DevicesAdapter.DeviceViewHolder>() {

        override fun getItemCount(): Int {
            return mDevices.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DevicesAdapter.DeviceViewHolder {
            val v: View = LayoutInflater.from(parent.context).inflate(R.layout.row_item, parent, false)
            return DeviceViewHolder(v)
        }

        override fun onBindViewHolder(holder: DevicesAdapter.DeviceViewHolder, position: Int) {
            val device: BluetoothDevice = mDevices[position]
            holder._textView.text = device.name
            holder._device = device
        }

        inner class DeviceViewHolder : RecyclerView.ViewHolder {

            lateinit var _device: BluetoothDevice
            var _textView: TextView

            constructor(v: View) : super(v) {
                _textView = v.findViewById(R.id.row_item) as TextView
                val container = v.findViewById<LinearLayout>(R.id.container)
                container.setOnClickListener {
                    Intent(applicationContext, TestView::class.java).also { intent ->
                        intent.putExtra("Device", _device)
                        startActivity(intent)
                    }
                }
            }
        }
    }
}


