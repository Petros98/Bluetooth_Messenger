package com.example.btmessenger.chat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.util.Base64.DEFAULT
import java.io.ByteArrayOutputStream

class EncodeDecode {
    fun encode(path: String?): ByteArray {
        //encode image to base64 string
        val baos = ByteArrayOutputStream()
        val bitmap = BitmapFactory.decodeFile(path)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        return baos.toByteArray()
    }

    fun decode(imageString: String): Bitmap {
        //decode base64 string to image
//        val imageString = encodeToString(baos, DEFAULT)
        val imageBytes = Base64.decode(imageString, DEFAULT)
        return BitmapFactory.decodeByteArray(imageBytes,0,imageBytes.size)
    }

    fun getPath(context: Context, uri: Uri?): String {
        var result: String? = null
        val pro = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = context.contentResolver.query(
            uri!!,
            pro,
            null,
            null,
            null
        )
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(pro[0])
                result = cursor.getString(columnIndex)
            }
            cursor.close()
        }
        if (result == null) {
            result = "Not found"
        }
        return result
    }

}