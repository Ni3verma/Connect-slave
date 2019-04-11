package com.nitin.connect_slave

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*

const val WRITE_SETTINGS_PERMISSION_CODE = 124
const val REQ_PICK_SOUNDFILE_CODE = 125
class MainActivity : AppCompatActivity() {
    private lateinit var db: DatabaseReference
    private lateinit var context: Context


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        context = this
        FirebaseApp.initializeApp(this)

        db = FirebaseDatabase.getInstance().reference.child("settings")

        if (!hasSettingsWritePermission()) {
            requestWritePermission()
        } else {
            db.addValueEventListener(settingsListener)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        db.removeEventListener(settingsListener)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == WRITE_SETTINGS_PERMISSION_CODE && hasSettingsWritePermission()) {
            db.addValueEventListener(settingsListener)
        }
    }

    private val settingsListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            val settings = dataSnapshot.getValue(Settings::class.java)
            Log.d("Nitin", settings?.toString())
            settings?.let {
                updateBrightness(it.brightness)

            }
        }
        override fun onCancelled(error: DatabaseError) {
            Log.d("Nitin", "failed to read value", error.toException())
        }
    }

    fun updateBrightness(level: Int) {
        android.provider.Settings.System.putInt(
            this@MainActivity.contentResolver,
            android.provider.Settings.System.SCREEN_BRIGHTNESS,
            level
        )
    }

    fun hasSettingsWritePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.provider.Settings.System.canWrite(context)
        } else {
            return (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_SETTINGS
            ) == PackageManager.PERMISSION_GRANTED)
        }
    }

    private fun requestWritePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intent = Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:${context.packageName}")

            this@MainActivity.startActivityForResult(
                intent,
                WRITE_SETTINGS_PERMISSION_CODE
            )
        } else {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.WRITE_SETTINGS),
                WRITE_SETTINGS_PERMISSION_CODE
            )
        }
    }
}
