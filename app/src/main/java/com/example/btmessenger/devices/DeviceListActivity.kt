package com.example.btmessenger.devices

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.btmessenger.R

/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */
class DeviceListActivity : AppCompatActivity(), DevicesListAdapter.OnItemClickListener {
    /**
     * Member fields
     */
    private lateinit var mBtAdapter: BluetoothAdapter
    private lateinit var deviceRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var mToolbar: Toolbar
    private var devices = ArrayList<BluetoothDevice>()

    /**
     * Newly discovered devices
     */
    private var mNewDevicesArrayAdapter: ArrayAdapter<String>? = null
    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup the window
        setContentView(R.layout.activity_device_list)
        mToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(mToolbar)
        progressBar = findViewById(R.id.progressBar)
        deviceRecyclerView = findViewById(R.id.pairedRecyclerView)
        deviceRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        val devicesListAdapter = DevicesListAdapter(devices, this)
        deviceRecyclerView.adapter = devicesListAdapter

        // Initialize the button to perform device discovery

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED)

        // Register for broadcasts when a device is discovered
        var filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        this.registerReceiver(mReceiver, filter)

        // Register for broadcasts when discovery has finished
        filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        this.registerReceiver(mReceiver, filter)

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter()

        // Get a set of currently paired devices
        val pairedDevices = mBtAdapter.bondedDevices

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size > 0) {
            deviceRecyclerView.visibility = View.VISIBLE
            pairedDevices.forEach {
                if (it.bluetoothClass.deviceClass == BluetoothClass.Device.PHONE_SMART)
                    devices.add(0,it)
                else
                    devices.add(it)
            }
        } else {
            Toast.makeText(applicationContext, "No paired devices", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.device, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
       when(item.itemId){
           R.id.Scan ->{
               doDiscovery()
               return true
           }
       }
        return false
    }
    override fun onDestroy() {
        super.onDestroy()

        // Make sure we're not doing discovery anymore
        mBtAdapter.cancelDiscovery()

        // Unregister broadcast listeners
        unregisterReceiver(mReceiver)
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private fun doDiscovery() {
//        Log.d(TAG, "doDiscovery()");

        // Indicate scanning in the title
        deviceRecyclerView.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
//        setTitle(R.string.scanning)

        // Turn on sub-title for new devices
//        findViewById<View>(R.id.title_new_devices).visibility = View.VISIBLE

        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering) {
            mBtAdapter.cancelDiscovery()
        }

        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery()
    }

    /**
     * The BroadcastReceiver that listens for discovered devices and changes the title when
     * discovery is finished
     */
    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val newDevices = ArrayList<BluetoothDevice>()
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND == action) {
                // Get the BluetoothDevice object from the Intent
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                // If it's already paired, skip it, because it's been listed already
                if (device.bondState != BluetoothDevice.BOND_BONDED) {
                    if (!devices.contains(device))
                        devices.add(0, device)
                    newDevices.add(device)
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                if (newDevices.size == 0) {
                    newDevices.clear()
                    progressBar.visibility = View.GONE
                    deviceRecyclerView.visibility = View.VISIBLE
                    Toast.makeText(context, getString(R.string.none_found), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
         /**
         * Return Intent extra
         */
        var EXTRA_DEVICE_ADDRESS = "device_address"
        var EXTRA_DEVICE_NAME = "device_name"
    }

    override fun onItemClicked(device: BluetoothDevice) {
        progressBar.visibility = View.VISIBLE
        mBtAdapter.cancelDiscovery()

        val address = device.address
        val deviceName = device.name
        // Create the result Intent and include the MAC address
        val intent = Intent()
        intent.putExtra(EXTRA_DEVICE_ADDRESS, address)
        intent.putExtra(EXTRA_DEVICE_NAME, deviceName)

        // Set result and finish this Activity
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}