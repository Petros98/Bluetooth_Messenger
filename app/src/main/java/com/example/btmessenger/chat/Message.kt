package com.example.btmessenger.chat

data class Message(
        var msgId: String,
        var userId : String = "myMacAddress",
        var userName: String,
        var msg: String,
        var type: String
)