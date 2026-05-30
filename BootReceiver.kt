package com.danareader.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("DANAReader", "Boot completed - service will auto-start via NotificationListenerService")
            // NotificationListenerService otomatis restart oleh sistem Android
            // jika sudah diberi izin sebelumnya
        }
    }
}
