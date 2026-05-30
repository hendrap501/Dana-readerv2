package com.danareader.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TransactionReceiver(private val activity: MainActivity) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.danareader.TRANSACTION_RECEIVED") {
            val amount = intent.getLongExtra("amount", 0L)
            val sender = intent.getStringExtra("sender") ?: "Tidak diketahui"
            val amountFormatted = "Rp${String.format("%,d", amount).replace(",", ".")}"
            activity.addLogEntry("QRIS", amountFormatted, sender)
        }
    }
}
