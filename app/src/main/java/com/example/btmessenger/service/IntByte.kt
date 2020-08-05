package com.example.btmessenger.service


fun Int.intToByteArray() : ByteArray {
    var intStr = this.toString()
    val len = intStr.length
    val str = "0000000$intStr"
    intStr = str.substring(len-1)
    val arr = IntArray(4)
    arr[0] = intStr.substring(0,2).toInt()
    arr[1] = intStr.substring(2,4).toInt()
    arr[2] = intStr.substring(4,6).toInt()
    arr[3] = intStr.substring(6).toInt()
    val bytes = ByteArray(arr.size)
    for (i in bytes.indices){
        bytes[i] = arr[i].toByte()
    }
    return bytes
}

fun ByteArray.toInt() : Int {
    var len = 0
    len += this[0].toInt()*1000000
    len += this[1].toInt()*10000
    len += this[2].toInt()*100
    len += this[3].toInt()
    return len
}