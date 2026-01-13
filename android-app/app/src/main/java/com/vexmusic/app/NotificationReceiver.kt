package com.vexmusic.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.getStringExtra("action")
        Log.d("NotificationReceiver", "Received action: $action")
        
        // Handle notification actions if needed
        when(action) {
            "PLAY_PAUSE" -> {
                // Send event to WebView to handle play/pause
                // This would communicate back to the web app
            }
            "NEXT" -> {
                // Send event to WebView to handle next track
            }
            "PREVIOUS" -> {
                // Send event to WebView to handle previous track
            }
        }
    }
}