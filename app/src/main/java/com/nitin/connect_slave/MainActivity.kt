package com.nitin.connect_slave

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {
    private lateinit var db: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FirebaseApp.initializeApp(this)

        db = FirebaseDatabase.getInstance().reference.child("settings")

        db.addValueEventListener(settingsListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        db.removeEventListener(settingsListener)
    }

    private val settingsListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            val settings = dataSnapshot.getValue(Settings::class.java)
            Log.d("Nitin", settings?.toString())
        }

        override fun onCancelled(error: DatabaseError) {
            Log.d("Nitin", "failed to read value", error.toException())
        }
    }
}
