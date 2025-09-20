package com.example.shan_inventory.utils

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

object FirebaseDebug {
    private const val TAG = "FirebaseDebug"
    
    fun testDatabaseConnection() {
        val database = FirebaseDatabase.getInstance("https://shan001-5b11c-default-rtdb.asia-southeast1.firebasedatabase.app/")
        val testRef = database.reference.child("test_connection")
        
        testRef.setValue("test_value")
            .addOnSuccessListener {
                Log.d(TAG, "Database write successful")
                // Clean up test data
                testRef.removeValue()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Database write failed: ${exception.message}")
            }
    }
    
    fun testDatabaseRead() {
        val database = FirebaseDatabase.getInstance("https://shan001-5b11c-default-rtdb.asia-southeast1.firebasedatabase.app/")
        val testRef = database.reference.child("test_read")
        
        testRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                Log.d(TAG, "Database read successful: ${snapshot.value}")
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database read failed: ${error.message}")
            }
        })
    }
}
