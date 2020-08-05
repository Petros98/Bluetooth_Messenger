package com.example.btmessenger.devices

import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.btmessenger.R


class DevicesListAdapter(
    private val devices: ArrayList<BluetoothDevice>,
    private val itemClickListener: OnItemClickListener
) : RecyclerView.Adapter<DevicesListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.device, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int {
        return devices.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(devices[position], itemClickListener)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems(
            device: BluetoothDevice,
            itemClickListener: OnItemClickListener
        ) {
            val deviceName = itemView.findViewById<TextView>(R.id.deviceName)
            val icon = itemView.findViewById<ImageView>(R.id.deviceIcon)
            if (device.bluetoothClass.deviceClass == BluetoothClass.Device.PHONE_SMART)
                icon.visibility = View.VISIBLE
            else
                icon.visibility = View.GONE
            deviceName.text = device.name

            itemView.setOnClickListener {
                itemClickListener.onItemClicked(device)
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClicked(device: BluetoothDevice)
    }

}





