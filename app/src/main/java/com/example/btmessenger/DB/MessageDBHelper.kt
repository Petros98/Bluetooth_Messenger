package com.example.btmessenger.DB

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.btmessenger.chat.Message
import java.lang.Exception
import java.util.*

class MessageDBHelper(context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION){
    companion object {
        // If you change the database schema, you must increment the database version.
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "MessagesDb.db"

        private val SQL_CREATE_ENTRIES =
                "CREATE TABLE " + DBContract.TABLE_NAME + " (" +
                        DBContract.COLUMN_MSG_ID + " TEXT," +
                        DBContract.COLUMN_USER_ID + " TEXT," +
                        DBContract.COLUMN_USER_NAME + " TEXT," +
                        DBContract.COLUMN_MSG + " TEXT," +
                        DBContract.COLUMN_TYPE + " TEXT)"

        private val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + DBContract.TABLE_NAME
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(SQL_CREATE_ENTRIES)

    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }


    @Throws(SQLiteConstraintException::class)
    fun insertMessage(message: Message): Boolean {
        // Gets the data repository in write mode
        val db = writableDatabase

        // Create a new map of values, where column names are the keys
        val values = ContentValues()
        values.put(DBContract.COLUMN_MSG_ID, message.msgId)
        values.put(DBContract.COLUMN_USER_ID, message.userId)
        values.put(DBContract.COLUMN_USER_NAME, message.userName)
        values.put(DBContract.COLUMN_MSG, message.msg)
        values.put(DBContract.COLUMN_TYPE, message.type)

        // Insert the new row, returning the primary key value of the new row
        val newRowId = db.insert(DBContract.TABLE_NAME, null, values)

        return true
    }
    @Throws(SQLiteConstraintException::class)
    fun deleteMessages(userId: String){
        val db = writableDatabase
        db.delete(DBContract.TABLE_NAME,
                DBContract.COLUMN_USER_ID + "='" + userId + "'",
                null)
    }

    @Throws(SQLiteConstraintException::class)
    fun deleteMessage(msgId: String): Boolean {
        // Gets the data repository in write mode
        val db = writableDatabase
        // Define 'where' part of query.
        val selection = DBContract.COLUMN_MSG_ID + " LIKE ?"
        // Specify arguments in placeholder order.
        val selectionArgs = arrayOf(msgId)
        // Issue SQL statement.
        db.delete(DBContract.TABLE_NAME, selection, selectionArgs)

        return true
    }

    @SuppressLint("Recycle")
    fun readMessages(userId: String): ArrayList<Message> {
        val messages = ArrayList<Message>()
        val db = writableDatabase
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery(
                    "select * from " +
                            DBContract.TABLE_NAME +
                            " WHERE " + DBContract.COLUMN_USER_ID +
                            "='" + userId + "'",
                    null
            )
        } catch (e: SQLiteException) {
            // if table not yet present, create it
            db.execSQL(SQL_CREATE_ENTRIES)
            return ArrayList()
        }

        var userName: String
        var msg: String
        var msgId: String
        var type: String
        if (cursor!!.moveToFirst()) {
            while (!cursor.isAfterLast) {
                userName = cursor.getString(cursor.getColumnIndex(DBContract.COLUMN_USER_NAME))
                msg = cursor.getString(cursor.getColumnIndex(DBContract.COLUMN_MSG))
                msgId = cursor.getString(cursor.getColumnIndex(DBContract.COLUMN_MSG_ID))
                type = cursor.getString(cursor.getColumnIndex(DBContract.COLUMN_TYPE))
                messages.add(Message(msgId, userId, userName, msg, type))
                cursor.moveToNext()
            }
        }
        db.close()
        return messages
    }

    fun getLastMessage(userId: String) : Message {
        var message = Message("_","_","_","_","_")
        val db = writableDatabase
        var cursor : Cursor? = null
        try {
            cursor = db.rawQuery(
                    "SELECT * FROM " +
                            DBContract.TABLE_NAME +
                            " WHERE " +
                            DBContract.COLUMN_USER_ID + "='" + userId + "'",
                    null
            )
        } catch (e: SQLiteException) {
            // if table not yet present, create it
            db.execSQL(SQL_CREATE_ENTRIES)

        }

        if (cursor != null) {
            cursor.moveToLast()
            val msgId = cursor.getString(cursor.getColumnIndex(DBContract.COLUMN_MSG_ID))
            val userName = cursor.getString(cursor.getColumnIndex(DBContract.COLUMN_USER_NAME))
            val msg = cursor.getString(cursor.getColumnIndex(DBContract.COLUMN_MSG))
            val type = cursor.getString(cursor.getColumnIndex(DBContract.COLUMN_TYPE))
            message = Message(msgId, userId, userName, msg, type)
        }

        db.close()

        return message
    }

//    fun getMessagesCount(): Int {
//        val countQuery = "SELECT  * FROM ${DBContract.UserEntry.TABLE_NAME}"
//        val db = readableDatabase
//        val cursor = db.rawQuery(countQuery, null)
//        val count = cursor.count
//        cursor.close()
//        return count
//    }

//    fun readAllMessages(): ArrayList<Message> {
//        val messages = ArrayList<Message>()
//        val db = writableDatabase
//        var cursor: Cursor? = null
//        try {
//            cursor = db.rawQuery("select * from " + DBContract.UserEntry.TABLE_NAME, null)
//        } catch (e: SQLiteException) {
//            db.execSQL(SQL_CREATE_ENTRIES)
//            return ArrayList()
//        }
//
//        var userName: String
//        var userId: String
//        var msg: String
//        if (cursor!!.moveToFirst()) {
//            while (cursor.isAfterLast == false) {
//                userName = cursor.getString(cursor.getColumnIndex(DBContract.UserEntry.COLUMN_USER_NAME))
//                userId = cursor.getString(cursor.getColumnIndex(DBContract.UserEntry.COLUMN_USER_ID))
//                msg = cursor.getString(cursor.getColumnIndex(DBContract.UserEntry.COLUMN_MSG))
//
//                messages.add(Message(userid, name, age))
//                cursor.moveToNext()
//            }
//        }
//        return users
//    }

}