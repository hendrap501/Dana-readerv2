package com.danareader.app

import android.app.Notification
import android.content.Context
import android.content.SharedPreferences
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.*

class NotificationListenerService : android.service.notification.NotificationListenerService(),
    TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var prefs: SharedPreferences
    private var isTTSReady = false

    companion object {
        private const val TAG = "DANAReader"

        // Package names untuk aplikasi DANA
        private val DANA_PACKAGES = setOf(
            "id.dana",
            "com.dana.android",
            "id.dana.merchant"
        )

        // Pattern untuk mendeteksi transaksi masuk QRIS
        private val QRIS_KEYWORDS = listOf(
            "pembayaran qris",
            "qris masuk",
            "terima pembayaran",
            "berhasil menerima",
            "payment received",
            "qris diterima",
            "transaksi masuk",
            "menerima uang",
            "uang masuk"
        )

        // Pattern untuk mendeteksi jumlah uang (Rp)
        private val AMOUNT_PATTERNS = listOf(
            Regex("""Rp\.?\s*([\d.,]+)""", RegexOption.IGNORE_CASE),
            Regex("""IDR\s*([\d.,]+)""", RegexOption.IGNORE_CASE),
            Regex("""sejumlah\s+Rp\.?\s*([\d.,]+)""", RegexOption.IGNORE_CASE),
            Regex("""senilai\s+Rp\.?\s*([\d.,]+)""", RegexOption.IGNORE_CASE),
            Regex("""nominal\s+Rp\.?\s*([\d.,]+)""", RegexOption.IGNORE_CASE)
        )

        // Pattern untuk nama pengirim
        private val SENDER_PATTERNS = listOf(
            Regex("""dari\s+([A-Za-z\s]+?)(?:\s+(?:telah|berhasil|senilai|sebesar|Rp)|$)""", RegexOption.IGNORE_CASE),
            Regex("""from\s+([A-Za-z\s]+?)(?:\s+(?:has|successfully|amount|Rp)|$)""", RegexOption.IGNORE_CASE),
            Regex("""pengirim:\s*([A-Za-z\s]+?)(?:\n|$)""", RegexOption.IGNORE_CASE),
            Regex("""sender:\s*([A-Za-z\s]+?)(?:\n|$)""", RegexOption.IGNORE_CASE),
            Regex("""by\s+([A-Za-z\s]+?)(?:\s+(?:via|at|Rp)|$)""", RegexOption.IGNORE_CASE)
        )
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        tts = TextToSpeech(this, this)
        Log.d(TAG, "NotificationListenerService created")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTTSReady = true
            setTTSLanguage()
            Log.d(TAG, "TTS initialized successfully")
        } else {
            Log.e(TAG, "TTS initialization failed")
        }
    }

    private fun setTTSLanguage() {
        val langIndex = prefs.getInt(MainActivity.KEY_LANGUAGE, 0)
        val locale = if (langIndex == 0) Locale("id", "ID") else Locale.US
        val result = tts.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(Locale.US)
            Log.w(TAG, "Bahasa Indonesia tidak tersedia, fallback ke English")
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        // Cek apakah notifikasi dari DANA
        if (!DANA_PACKAGES.contains(sbn.packageName)) return

        // Cek apakah fitur aktif
        if (!prefs.getBoolean(MainActivity.KEY_ENABLED, true)) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getString(Notification.EXTRA_TITLE, "") ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT, "") ?: ""
        val bigText = extras.getString(Notification.EXTRA_BIG_TEXT, "") ?: ""

        val fullText = "$title $text $bigText"

        Log.d(TAG, "Notifikasi DANA diterima: $fullText")

        // Cek apakah ini transaksi masuk
        if (isIncomingTransaction(fullText)) {
            val amount = extractAmount(fullText)
            val sender = extractSender(fullText)

            Log.d(TAG, "Transaksi terdeteksi - Amount: $amount, Sender: $sender")

            if (amount != null) {
                announceTransaction(amount, sender)
                notifyMainActivity(amount, sender)
            }
        }
    }

    private fun isIncomingTransaction(text: String): Boolean {
        val lowerText = text.lowercase()
        return QRIS_KEYWORDS.any { keyword -> lowerText.contains(keyword) }
    }

    private fun extractAmount(text: String): Long? {
        for (pattern in AMOUNT_PATTERNS) {
            val match = pattern.find(text)
            if (match != null) {
                val amountStr = match.groupValues[1]
                    .replace(".", "")
                    .replace(",", "")
                    .trim()
                return amountStr.toLongOrNull()
            }
        }
        return null
    }

    private fun extractSender(text: String): String? {
        for (pattern in SENDER_PATTERNS) {
            val match = pattern.find(text)
            if (match != null) {
                val sender = match.groupValues[1].trim()
                if (sender.isNotEmpty() && sender.length > 2) {
                    return sender
                }
            }
        }
        return null
    }

    private fun announceTransaction(amount: Long, sender: String?) {
        if (!isTTSReady) {
            // Reinitialize TTS if needed
            tts = TextToSpeech(this, this)
            return
        }

        // Refresh settings
        setTTSLanguage()
        val speechRate = when (prefs.getInt(MainActivity.KEY_VOICE_SPEED, 1)) {
            0 -> 0.7f
            1 -> 1.0f
            2 -> 1.3f
            3 -> 1.6f
            else -> 1.0f
        }
        tts.setSpeechRate(speechRate)

        val langIndex = prefs.getInt(MainActivity.KEY_LANGUAGE, 0)
        val text = buildAnnouncementText(amount, sender, langIndex)

        Log.d(TAG, "TTS akan mengucapkan: $text")
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "dana_txn_${System.currentTimeMillis()}")
    }

    private fun buildAnnouncementText(amount: Long, sender: String?, langIndex: Int): String {
        val amountWords = if (langIndex == 0) {
            numberToWords(amount)
        } else {
            formatNumberEnglish(amount)
        }

        return if (langIndex == 0) {
            // Bahasa Indonesia
            if (sender != null) {
                "Pembayaran Q R I S masuk. $amountWords rupiah. Dari $sender."
            } else {
                "Pembayaran Q R I S masuk. $amountWords rupiah."
            }
        } else {
            // English
            if (sender != null) {
                "Q R I S payment received. $amountWords Rupiah. From $sender."
            } else {
                "Q R I S payment received. $amountWords Rupiah."
            }
        }
    }

    private fun notifyMainActivity(amount: Long, sender: String?) {
        // Broadcast ke MainActivity untuk update UI
        val intent = android.content.Intent("com.danareader.TRANSACTION_RECEIVED").apply {
            putExtra("amount", amount)
            putExtra("sender", sender ?: "Tidak diketahui")
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    /**
     * Konversi angka ke kata-kata Bahasa Indonesia
     */
    private fun numberToWords(number: Long): String {
        if (number == 0L) return "nol"
        if (number < 0) return "minus ${numberToWords(-number)}"

        val satuan = arrayOf("", "satu", "dua", "tiga", "empat", "lima",
            "enam", "tujuh", "delapan", "sembilan", "sepuluh", "sebelas")

        return when {
            number < 12 -> satuan[number.toInt()]
            number < 20 -> "${numberToWords(number - 10)} belas"
            number < 100 -> {
                val tens = number / 10
                val ones = number % 10
                val tensWord = when (tens.toInt()) {
                    2 -> "dua puluh"
                    3 -> "tiga puluh"
                    4 -> "empat puluh"
                    5 -> "lima puluh"
                    6 -> "enam puluh"
                    7 -> "tujuh puluh"
                    8 -> "delapan puluh"
                    9 -> "sembilan puluh"
                    else -> ""
                }
                if (ones == 0L) tensWord else "$tensWord ${numberToWords(ones)}"
            }
            number < 200 -> "seratus${if (number % 100 == 0L) "" else " ${numberToWords(number % 100)}"}"
            number < 1000 -> {
                val hundreds = number / 100
                val rest = number % 100
                "${numberToWords(hundreds)} ratus${if (rest == 0L) "" else " ${numberToWords(rest)}"}"
            }
            number < 2000 -> "seribu${if (number % 1000 == 0L) "" else " ${numberToWords(number % 1000)}"}"
            number < 1_000_000 -> {
                val thousands = number / 1000
                val rest = number % 1000
                "${numberToWords(thousands)} ribu${if (rest == 0L) "" else " ${numberToWords(rest)}"}"
            }
            number < 1_000_000_000 -> {
                val millions = number / 1_000_000
                val rest = number % 1_000_000
                "${numberToWords(millions)} juta${if (rest == 0L) "" else " ${numberToWords(rest)}"}"
            }
            else -> {
                val billions = number / 1_000_000_000
                val rest = number % 1_000_000_000
                "${numberToWords(billions)} miliar${if (rest == 0L) "" else " ${numberToWords(rest)}"}"
            }
        }
    }

    private fun formatNumberEnglish(number: Long): String {
        return when {
            number >= 1_000_000_000 -> "${number / 1_000_000_000} billion ${number % 1_000_000_000 / 1_000_000} million"
            number >= 1_000_000 -> "${number / 1_000_000} million"
            number >= 1_000 -> "${number / 1_000} thousand"
            else -> number.toString()
        }
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
