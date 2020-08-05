package com.example.bluetoothchat.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.btmessenger.chat.Message
import com.example.btmessenger.R
import com.example.btmessenger.chat.EncodeDecode


private const val VIEW_TYPE_MESSAGE_SENT = 1
private const val VIEW_TYPE_MESSAGE_RECEIVED = 2
private const val MESSAGE_TYPE_TEXT = 3
private const val MESSAGE_TYPE_FILE = 4
private lateinit var view: View
private val encodeDecode = EncodeDecode()

class ChatAdapter(
        private val messages: ArrayList<Message>,
        private val onItemLongClickListener : OnItemLongClickListener
) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(parent.context).inflate(R.layout.send_message, parent, false)
            return SentMessageViewHolder(view)
        }
        if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
            view = LayoutInflater.from(parent.context).inflate(R.layout.message, parent, false)
            return ReceivedMessageViewHolder(view)
        }
        return ReceivedMessageViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].userName == "Me")
            VIEW_TYPE_MESSAGE_SENT
        else
            VIEW_TYPE_MESSAGE_RECEIVED
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder.itemViewType) {
            VIEW_TYPE_MESSAGE_SENT -> (holder as SentMessageViewHolder).bindItems(message, onItemLongClickListener)
            VIEW_TYPE_MESSAGE_RECEIVED -> (holder as ReceivedMessageViewHolder).bindItems(message, onItemLongClickListener)
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }


    class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems(message: Message, onItemLongClickListener: OnItemLongClickListener) {
            val imageView = itemView.findViewById<ImageView>(R.id.sentImageView)
            val msg = itemView.findViewById<TextView>(R.id.sendMessage)
            if (message.type == "text"){
                imageView.visibility = View.GONE
                msg.visibility = View.VISIBLE
                msg.text = message.msg
            }else{
                imageView.visibility = View.VISIBLE
                msg.visibility = View.GONE
                val bitmap = encodeDecode.decode(message.msg)
                imageView.setImageBitmap(bitmap)
            }
            itemView.setOnLongClickListener {
                onItemLongClickListener.onItemLongClicked(message)
            }

        }


    }

    class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems(message: Message, onItemLongClickListener: OnItemLongClickListener) {
            val msg = itemView.findViewById<TextView>(R.id.receivedMessage)
            val imageView = itemView.findViewById<ImageView>(R.id.receivedImageView)
            if (message.type == "text"){
                imageView.visibility = View.GONE
                msg.visibility = View.VISIBLE
                msg.text = message.msg
            }else{
                imageView.visibility = View.VISIBLE
                msg.visibility = View.GONE
                val bitmap = encodeDecode.decode(message.msg)
                imageView.setImageBitmap(bitmap)
            }
            imageView.setOnLongClickListener {
                onItemLongClickListener.onItemLongClicked(message)
            }
        }


    }

    interface OnItemLongClickListener{
        fun onItemLongClicked(message: Message) : Boolean
    }


}