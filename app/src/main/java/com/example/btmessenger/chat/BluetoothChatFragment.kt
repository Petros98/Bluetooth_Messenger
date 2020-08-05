package com.example.btmessenger.chat

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Base64.DEFAULT
import android.util.Base64.encodeToString
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationManagerCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothchat.chat.ChatAdapter
import com.example.btmessenger.DB.MessageDBHelper
import com.example.btmessenger.R
import com.example.btmessenger.devices.DeviceListActivity
import com.example.btmessenger.service.BluetoothChatService
import com.example.btmessenger.service.Constants
import com.example.btmessenger.service.intToByteArray
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.fragment_bluetooth_chat.*
import kotlinx.android.synthetic.main.toolbar.*
import java.util.*
import kotlin.collections.ArrayList

/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
class BluetoothChatFragment : Fragment() , NavigationView.OnNavigationItemSelectedListener, ChatAdapter.OnItemLongClickListener {

    private lateinit var messageRecyclerView: RecyclerView
    private lateinit var chatAdapter :  ChatAdapter
    private lateinit var messages: ArrayList<Message>
    private lateinit var mOutEditText: TextInputEditText
    private var mSendButton: Button? = null
    private var status: TextView? = null
    private lateinit var mDrawer: DrawerLayout
    private lateinit var mToolbar: Toolbar
    private lateinit var mDrawerToggle: ActionBarDrawerToggle
    private lateinit var mNavigationView: NavigationView
    private lateinit var db: MessageDBHelper

    private lateinit var btnTakePhoto: Button
    private lateinit var btnChoosePhoto: Button

    private var mConnectedDeviceName: String = ""
    private lateinit var mConnectedDeviceAddress: String



    private var mOutStringBuffer: StringBuffer? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null

    var fileUri : Uri? = null
    private lateinit var imageView: ImageView
    private lateinit var btnImgSend: Button
    private val encodeDecode = EncodeDecode()

    private var mChatService: BluetoothChatService? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            val activity = activity
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show()
            activity!!.finish()
        }
        createNotificationChannel(context!!,
                NotificationManagerCompat.IMPORTANCE_DEFAULT, false,
                getString(R.string.app_name), "App notification channel.")
    }

    override fun onStart() {
        super.onStart()
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter!!.isEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mChatService != null) {
            mChatService!!.stop()
        }
    }

    override fun onResume() {
        super.onResume()

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService!!.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService!!.start()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_bluetooth_chat, container, false)
        initializeViews(view)
        mDrawerToggle = ActionBarDrawerToggle(
                activity, mDrawer, R.string.open, R.string.close)
        mNavigationView.setNavigationItemSelectedListener(this)
        mDrawerToggle.syncState()

        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        layoutManager.stackFromEnd = true
        messageRecyclerView.layoutManager = layoutManager
        messages = ArrayList()
        chatAdapter = ChatAdapter(messages, this)
        messageRecyclerView.adapter = chatAdapter
        messageRecyclerView.viewTreeObserver.addOnGlobalLayoutListener { scrollToEnd() }

        btnTakePhoto.setOnClickListener { launchCamera() }

        btnChoosePhoto.setOnClickListener { choosePhoto() }

        return view
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.info_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (mDrawerToggle.onOptionsItemSelected(item)) return true
        when (item.itemId){
            R.id.info -> {
                if (mConnectedDeviceName.isNotEmpty())
                    showDialog(mConnectedDeviceName)
            }
        }
        return false
    }

    private fun initializeViews(view: View){
        mToolbar = view.findViewById<View>(R.id.toolbar) as Toolbar
        val activity: AppCompatActivity? = activity as AppCompatActivity?
        activity?.setSupportActionBar(mToolbar)

        mDrawer = view.findViewById(R.id.drawer_layout)
        mNavigationView = view.findViewById(R.id.navigation_view)
        db = MessageDBHelper(context)

        messageRecyclerView = view.findViewById(R.id.messageView)
        mOutEditText = view.findViewById(R.id.edit_text_out)
        mSendButton = view.findViewById(R.id.button_send)
        status = view.findViewById(R.id.status)
        btnTakePhoto = view.findViewById(R.id.btn_take_photo)
        btnChoosePhoto = view.findViewById(R.id.btn_select_photo)
    }
    private fun launchCamera() {
        val values = ContentValues(1)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
        fileUri = activity?.contentResolver
                ?.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if(intent.resolveActivity(activity!!.packageManager) != null) {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
            intent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            startActivityForResult(intent, TAKE_PHOTO_REQUEST)
        }
    }

    private fun choosePhoto(){
//        val intent = Intent(activity, GaleryActivity::class.java)
//        startActivityForResult(intent, REQUEST_CHOOSE_PHOTO)
        val pickImageIntent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(pickImageIntent, PICK_PHOTO_REQUEST)
    }

    private fun showDialog(title: String) {
        val dialog = Dialog(context!!)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.fragment_info)
        val deviceName = dialog.findViewById<TextView>(R.id.deviceName)
        deviceName.text = title
        val btnClearChat = dialog.findViewById<Button>(R.id.clear_chat)
        btnClearChat.setOnClickListener {
            val builder = AlertDialog.Builder(context!!)

            builder.setTitle("Clear")

            builder.setMessage("Are you want to clear all messages ?")

            builder.setPositiveButton("YES"){dialog, which ->
                db.deleteMessages(mConnectedDeviceAddress)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    messages.removeIf { it.userId == mConnectedDeviceAddress }
                }
                chatAdapter.notifyDataSetChanged()
                Toast.makeText(context, "Cleared", Toast.LENGTH_SHORT).show()
            }
            builder.setNegativeButton("No"){dialog,which ->
                Toast.makeText(context, "Canceled", Toast.LENGTH_SHORT).show()
            }
            val alertDialog: AlertDialog = builder.create()

            // Display the alert dialog on app interface
            alertDialog.show()
        }
        dialog .show()

    }

    private fun showImageDialog(uri: Uri){
        val imageDialog = Dialog(context!!)
        imageDialog.also {
            it.requestWindowFeature(Window.FEATURE_LEFT_ICON)
            it.requestWindowFeature(Window.FEATURE_NO_TITLE)
            it.setCancelable(true)
            it.setContentView(R.layout.image_dialog)
        }
        val imageView = imageDialog.findViewById<ImageView>(R.id.imageView)
        imageView.setImageURI(uri)
        val btnImgSend = imageDialog.findViewById<Button>(R.id.btn_img_send)
        btnImgSend.setOnClickListener {
            sendMessage(
                    encodeDecode.encode( encodeDecode.getPath( context!!, uri ) )
            )
            btnImgSend.visibility = View.GONE
            imageView.visibility = View.GONE
            imageDialog.dismiss()
        }
        imageDialog.show()
    }
    private fun scrollToEnd() = (chatAdapter.itemCount - 1).takeIf { it > 0 }?.let(messageRecyclerView::smoothScrollToPosition)

    /**
     * Set up the UI and background operations for chat.
     */
    private fun setupChat() {

        mOutEditText.setOnEditorActionListener(mWriteListener)

        mSendButton!!.setOnClickListener {
//                val textView = view.findViewById<EditText>(R.id.edit_text_out) as TextView
                val message = mOutEditText.text.toString()
                sendMessage(message)
        }

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = BluetoothChatService(activity, mHandler)

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = StringBuffer("")
    }

    /**
     * Makes this device discoverable for 300 seconds (5 minutes).
     */
    private fun ensureDiscoverable() {
        if (mBluetoothAdapter!!.scanMode !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            startActivity(discoverableIntent)
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private fun sendMessage(message: String) {
        // Check that we're actually connected before trying anything
        if (mChatService?.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(activity, R.string.not_connected, Toast.LENGTH_SHORT).show()
            return
        }

        // Check that there's actually something to send
        if (message.isNotEmpty()) {
            // Get the message bytes and tell the BluetoothChatService to write

            val typeByte : Byte = Constants.TYPE_TEXT //type text
            val send = message.toByteArray()
            val messageSize = (send.size + 1).intToByteArray()
            Log.i("info", send.size.toString())
            val bytes = ByteArray(send.size + 5)
            for (i in 0..3){
                bytes[i] = messageSize[i]
            }
            for (i in 4 until bytes.size-1){
                bytes[i] = send[i-4]
            }
            bytes[bytes.size-1] = typeByte
            mChatService!!.write(bytes)

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer!!.setLength(0)
            mOutEditText.setText(mOutStringBuffer)
        }
    }
    private fun sendMessage(fileBytes: ByteArray) {
        // Check that we're actually connected before trying anything
        if (mChatService?.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(activity, R.string.not_connected, Toast.LENGTH_SHORT).show()
            return
        }

        // Check that there's actually something to send
        if (fileBytes.isNotEmpty()) {
            // Get the message bytes and tell the BluetoothChatService to write
            val typeFile : Byte = Constants.TYPE_FILE // Type file
            val messageSize = fileBytes.size.intToByteArray()
            val bytes = ByteArray(fileBytes.size + 5)
            for (i in 0..3){
                bytes[i] = messageSize[i]
            }
            for (i in 4 until bytes.size-1){
                bytes[i] = fileBytes[i-4]
            }
            bytes[bytes.size-1] = typeFile
            mChatService!!.write(bytes)

            // Reset out string buffer to zero and clear the edit text field
//            mOutStringBuffer!!.setLength(0)
//            mOutEditText.setText(mOutStringBuffer)
        }
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private val mWriteListener = OnEditorActionListener { view, actionId, event -> // If the action is a key-up event on the return key, send the message
        if (actionId == EditorInfo.IME_NULL && event.action == KeyEvent.ACTION_UP) {
            val message = view.text.toString()
            sendMessage(message)
        }
        true
    }

    private fun setStatus(resId: Int) {
        when(resId){
            R.string.title_connected -> status?.setTextColor(Color.GREEN)
            R.string.title_connecting -> status?.setTextColor(Color.WHITE)
            R.string.title_not_connected -> status?.setTextColor(Color.RED)
        }
        status!!.setText(resId)
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private val mHandler: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: android.os.Message) {
            val activity = activity
            when (msg.what) {
                Constants.MESSAGE_STATE_CHANGE -> when (msg.arg1) {
                    BluetoothChatService.STATE_CONNECTED -> {
                        setStatus(R.string.title_connected)
                    }
                    BluetoothChatService.STATE_CONNECTING -> setStatus(R.string.title_connecting)
                    BluetoothChatService.STATE_LISTEN, BluetoothChatService.STATE_NONE -> setStatus(R.string.title_not_connected)
                }
                Constants.MESSAGE_WRITE -> {
                    val writeBuf = msg.obj as ByteArray
                    // construct a string from the buffer
                    val type = writeBuf.last()
                    if (type == Constants.TYPE_FILE){
                        val fileString = encodeToString(writeBuf.copyOfRange(4,writeBuf.size-1), DEFAULT)
                        db.insertMessage(
                                Message(
                                        msgId = UUID.randomUUID().toString(),
                                        userId = mConnectedDeviceAddress,
                                        userName = "Me",
                                        msg =  fileString,
                                        type = "file"
                                )
                        )
                    }else{
                        val writeMessage = String(writeBuf.copyOf(writeBuf.size-1))
                        Log.i("info", "write message $writeMessage")
                        db.insertMessage(
                                Message(
                                        msgId = UUID.randomUUID().toString(),
                                        userId = mConnectedDeviceAddress,
                                        userName = "Me",
                                        msg =  writeMessage,
                                        type = "text"
                                )
                        )
                    }


                    messages.add(db.getLastMessage(mConnectedDeviceAddress))
                    chatAdapter.notifyDataSetChanged()
                }
                Constants.MESSAGE_READ -> {
                    val readBuf = msg.obj as ByteArray
                    // construct a string from the buffer
                    val type = readBuf.last()
                    Toast.makeText(context, "size = ${readBuf.size} last = $type", Toast.LENGTH_LONG).show()
                    if (type == Constants.TYPE_FILE){
                        val fileString = encodeToString(readBuf.copyOf(readBuf.size-1), DEFAULT)
                        db.insertMessage(
                                Message(
                                        msgId = UUID.randomUUID().toString(),
                                        userId = mConnectedDeviceAddress,
                                        userName = mConnectedDeviceName,
                                        msg =  fileString,
                                        type = "file"
                                )
                        )
                    } else {
                        val readMessage = String(readBuf.copyOf(readBuf.size-1))
                        Log.i("info", "readMessage $readMessage")
                        db.insertMessage(
                                Message(
                                        msgId = UUID.randomUUID().toString(),
                                        userId = mConnectedDeviceAddress,
                                        userName = mConnectedDeviceName,
                                        msg =  readMessage,
                                        type = "text"
                                )
                        )
                    }

                    messages.add(db.getLastMessage(mConnectedDeviceAddress))
                    chatAdapter.notifyDataSetChanged()
                }
                Constants.MESSAGE_DEVICE_NAME -> {
                    // save the connected device's name

                    mConnectedDeviceName = msg.data.getString(Constants.DEVICE_NAME).toString()
                    mConnectedDeviceAddress = msg.data.getString(Constants.DEVICE_ADDRESS).toString()
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show()
                        messages.clear()
                        messages.addAll(db.readMessages(mConnectedDeviceAddress))
                        chatAdapter.notifyDataSetChanged()
                    }
                }
                Constants.MESSAGE_TOAST -> if (null != activity) {
                    Toast.makeText(activity, msg.data.getString(Constants.TOAST),
                            Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            when (requestCode) {
                REQUEST_CONNECT_DEVICE_SECURE ->                 // When DeviceListActivity returns with a device to connect
                    if (resultCode == Activity.RESULT_OK) {
                        connectDevice(data, true)
                    }
                REQUEST_CONNECT_DEVICE_INSECURE ->                 // When DeviceListActivity returns with a device to connect
                    if (resultCode == Activity.RESULT_OK) {
                        connectDevice(data, false)
                    }
                REQUEST_ENABLE_BT ->                 // When the request to enable Bluetooth returns
                    if (resultCode == Activity.RESULT_OK) {
                        // Bluetooth is now enabled, so set up a chat session
                        setupChat()
                    } else {
                        // User did not enable Bluetooth or an error occurred
                        Toast.makeText(activity, R.string.bt_not_enabled_leaving,
                                Toast.LENGTH_SHORT).show()
                        activity!!.finish()
                    }
                PICK_PHOTO_REQUEST -> {
                    if (resultCode == Activity.RESULT_OK){
                        fileUri = data?.data
                        showImageDialog(fileUri!!)
                    }

                }
                TAKE_PHOTO_REQUEST -> {
                    if (resultCode == Activity.RESULT_OK)
                        showImageDialog(fileUri!!)
                    }

            }
    }


    private fun connectDevice(data: Intent?, secure: Boolean) {
        // Get the device MAC address
        val address = data!!.extras
                ?.getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS)
        mConnectedDeviceName = data.extras?.getString(DeviceListActivity.EXTRA_DEVICE_NAME).toString()
        toolbar.subtitle = mConnectedDeviceName
        if (address != null) {
            mConnectedDeviceAddress = address
            messages.clear()
            messages.addAll(db.readMessages(mConnectedDeviceAddress))
            chatAdapter.notifyDataSetChanged()
        }
        // Get the BluetoothDevice object
        val device = mBluetoothAdapter!!.getRemoteDevice(address)
        // Attempt to connect to the device
        mChatService!!.connect(device, secure)
    }



    companion object {
        // Intent request codes
        private const val REQUEST_CONNECT_DEVICE_SECURE = 1
        private const val REQUEST_CONNECT_DEVICE_INSECURE = 2
        private const val REQUEST_ENABLE_BT = 3
        private const val TAKE_PHOTO_REQUEST: Int = 4
        private const val PICK_PHOTO_REQUEST: Int = 5
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.secure_connect_scan -> {
                mDrawer.closeDrawers()
                // Launch the DeviceListActivity to see devices and do scan
                val serverIntent = Intent(activity, DeviceListActivity::class.java)
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE)
                return true
            }
            R.id.insecure_connect_scan -> {
                mDrawer.closeDrawers()

                // Launch the DeviceListActivity to see devices and do scan
                val serverIntent = Intent(activity, DeviceListActivity::class.java)
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE)
                return true
            }
            R.id.discoverable -> {
                mDrawer.closeDrawers()

                // Ensure this device is discoverable by others
                ensureDiscoverable()
                return true
            }
        }
        return false
    }

    override fun onItemLongClicked(message: Message): Boolean {
        TODO("Not yet implemented")
    }

}
