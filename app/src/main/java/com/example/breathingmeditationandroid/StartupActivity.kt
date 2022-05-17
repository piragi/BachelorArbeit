package com.example.breathingmeditationandroid

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class StartupActivity : AppCompatActivity() {

    private val mDevices = ArrayList<BluetoothDevice>()

    @SuppressLint("MissingPermission") //TODO: supressed permissions
    override fun onCreate(savedInstanceState: Bundle?) {
        //Instance State
        super.onCreate(savedInstanceState)

        //View erstellen
        setContentView(R.layout.activity_device_list)
        val recyclerView: RecyclerView = findViewById(R.id.my_recycler_view)
        recyclerView.setHasFixedSize(true)
        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH), 0)

        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices

        //TODO: change the null check
        if (pairedDevices != null) {
            if (pairedDevices.isNotEmpty()) {
                mDevices.addAll(pairedDevices)
            }
        }

        val adapter = DevicesAdapter()
        recyclerView.adapter = adapter

    }

    private inner class DevicesAdapter : RecyclerView.Adapter<DevicesAdapter.DeviceViewHolder>() {

        override fun getItemCount(): Int {
            return mDevices.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DevicesAdapter.DeviceViewHolder {
            val v: View = LayoutInflater.from(parent.context).inflate(R.layout.row_item, parent, false)
            return DeviceViewHolder(v)
        }

        @SuppressLint("MissingPermission") //TODO: Supressed permission
        override fun onBindViewHolder(holder: DevicesAdapter.DeviceViewHolder, position: Int) {
            val device: BluetoothDevice = mDevices[position]
            holder.textView.text = device.name
            holder.device = device
        }

        inner class DeviceViewHolder(v: View) : RecyclerView.ViewHolder(v) {

            lateinit var device: BluetoothDevice
            var textView: TextView = v.findViewById(R.id.row_item) as TextView

            init {
                val container = v.findViewById<LinearLayout>(R.id.container)
                container.setOnClickListener {
                    Intent(applicationContext, HomeScreenActivity::class.java).also { intent ->
                        intent.putExtra("Device", device)
                        startActivity(intent)
                    }
                }
            }
        }
    }
}


