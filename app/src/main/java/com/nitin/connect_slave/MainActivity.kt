package com.nitin.connect_slave

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*

const val WRITE_SETTINGS_PERMISSION_CODE = 124
const val REQ_PICK_SOUNDFILE_CODE = 125

class MainActivity : AppCompatActivity() {
    private lateinit var db: DatabaseReference
    private lateinit var context: Context
    private var mediaPlayer: MediaPlayer? = null
    private var playbackState: String? = null
    private var currVolume: Int? = null

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

        choose_song_btn.setOnClickListener { openChooser() }
    }

    override fun onDestroy() {
        super.onDestroy()
        db.removeEventListener(settingsListener)
        mediaPlayer?.release()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_PICK_SOUNDFILE_CODE && resultCode == Activity.RESULT_OK) {
            val songUri = data?.data
            Log.d("Nitin", songUri?.toString())
            mediaPlayer = MediaPlayer.create(context, songUri)
            playbackState?.let { updatePlaybackState(it) }
            currVolume?.let { updateVolume(it) }
        }

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
                updatePlaybackState(it.state)
                updateVolume(it.volume)
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.d("Nitin", "failed to read value", error.toException())
        }
    }

    private fun openChooser() {
        val songChooserintent = Intent()
        songChooserintent.action = Intent.ACTION_GET_CONTENT
        songChooserintent.type = "audio/*"
        startActivityForResult(Intent.createChooser(songChooserintent, "Choose mp3 song"), REQ_PICK_SOUNDFILE_CODE)
    }

    fun updateBrightness(level: Int) {
        android.provider.Settings.System.putInt(
            this@MainActivity.contentResolver,
            android.provider.Settings.System.SCREEN_BRIGHTNESS,
            level
        )
    }

    fun updatePlaybackState(state: String) {
        playbackState = state
        mediaPlayer?.let {
            when (state) {
                "play" ->
                    if (!it.isPlaying)
                        it.start()
                "pause" ->
                    if (it.isPlaying)
                        it.pause()
            }
        }
    }

    fun updateVolume(volume: Int) {
        currVolume = volume
        val maxVolume = 101
        val vol = (Math.log(volume.toDouble()) / Math.log(maxVolume.toDouble())).toFloat()
        Log.d("Nitin", "volume=$vol")
        mediaPlayer?.setVolume(vol, vol)
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
