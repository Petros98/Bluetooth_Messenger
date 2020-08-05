package com.example.btmessenger

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.MultiplePermissionsListener


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askPermissions()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    private fun askPermissions(){
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                ).withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport) {/* ... */
                        if(report.areAllPermissionsGranted()){
                            //once permissions are granted, launch the camera
                            setContentView(R.layout.activity_main)
                        }else{
                            Toast.makeText(this@MainActivity, "All permissions need to be granted", Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                            permissions: MutableList<com.karumi.dexter.listener.PermissionRequest>?,
                            token: PermissionToken?
                    ) {
                        AlertDialog.Builder(this@MainActivity)
                                .setTitle(
                                        "Permissions Error!")
                                .setMessage(
                                        "Please allow permissions to take photo with camera")
                                .setNegativeButton(
                                        android.R.string.cancel
                                ) { dialog, _ ->
                                    dialog.dismiss()
                                    token?.cancelPermissionRequest()
                                }
                                .setPositiveButton(android.R.string.ok
                                ) { dialog, _ ->
                                    dialog.dismiss()
                                    token?.continuePermissionRequest()
                                }
                                .setOnDismissListener {
                                    token?.cancelPermissionRequest() }
                                .show()
                    }

                }).check()

    }
}