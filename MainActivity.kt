package com.danareader.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.text.TextUtils
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var prefs: SharedPreferences
    private lateinit var transactionReceiver: TransactionReceiver
    private lateinit var statusCard: CardView
    private lateinit var statusIcon: ImageView
    private lateinit var statusTitle: TextView
    private lateinit var statusDesc: TextView
    private lateinit var btnPermission: Button
    private lateinit var btnTest: Button
    private lateinit var switchEnabled: Switch
    private lateinit var logContainer: LinearLayout
    private lateinit var scrollLog: ScrollView
    private lateinit var tvTotalToday: TextView
    private lateinit var tvTransactionCount: TextView
    private lateinit var spinnerVoiceSpeed: Spinner
    private lateinit var spinnerVoiceLanguage: Spinner

    private var isTTSReady = false
    private var totalToday = 0L
    private var transactionCount = 0

    companion object {
        const val PREFS_NAME = "DANAReaderPrefs"
        const val KEY_ENABLED = "enabled"
        const val KEY_VOICE_SPEED = "voice_speed"
        const val KEY_LANGUAGE = "language"
        const val KEY_TOTAL_TODAY = "total_today"
        const val KEY_TXN_COUNT = "txn_count"
        const val KEY_LAST_DATE = "last_date"
        const val CHANNEL_ID = "dana_reader_channel"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        createNotificationChannel()
        initViews()
        initTTS()
        resetDailyStatsIfNeeded()
        loadStats()
        setupListeners()
        updateUI()

        // Register broadcast receiver for transaction updates
        transactionReceiver = TransactionReceiver(this)
        val filter = android.content.IntentFilter("com.danareader.TRANSACTION_RECEIVED")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(transactionReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(transactionReceiver, filter)
        }
    }

    private fun initViews() {
        statusCard = findViewById(R.id.statusCard)
        statusIcon = findViewById(R.id.statusIcon)
        statusTitle = findViewById(R.id.statusTitle)
        statusDesc = findViewById(R.id.statusDesc)
        btnPermission = findViewById(R.id.btnPermission)
        btnTest = findViewById(R.id.btnTest)
        switchEnabled = findViewById(R.id.switchEnabled)
        logContainer = findViewById(R.id.logContainer)
        scrollLog = findViewById(R.id.scrollLog)
        tvTotalToday = findViewById(R.id.tvTotalToday)
        tvTransactionCount = findViewById(R.id.tvTransactionCount)
        spinnerVoiceSpeed = findViewById(R.id.spinnerVoiceSpeed)
        spinnerVoiceLanguage = findViewById(R.id.spinnerVoiceLanguage)

        // Setup voice speed spinner
        val speeds = arrayOf("Lambat (0.7x)", "Normal (1.0x)", "Cepat (1.3x)", "Sangat Cepat (1.6x)")
        spinnerVoiceSpeed.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, speeds)
        spinnerVoiceSpeed.setSelection(prefs.getInt(KEY_VOICE_SPEED, 1))

        // Setup language spinner
        val languages = arrayOf("Bahasa Indonesia", "English")
        spinnerVoiceLanguage.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)
        spinnerVoiceLanguage.setSelection(prefs.getInt(KEY_LANGUAGE, 0))

        switchEnabled.isChecked = prefs.getBoolean(KEY_ENABLED, true)
    }

    private fun initTTS() {
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTTSReady = true
            setTTSLanguage()
        }
    }

    private fun setTTSLanguage() {
        if (!isTTSReady) return
        val langIndex = prefs.getInt(KEY_LANGUAGE, 0)
        val locale = if (langIndex == 0) Locale("id", "ID") else Locale.US
        val result = tts.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(Locale.US)
        }
    }

    private fun getVoiceSpeed(): Float {
        return when (prefs.getInt(KEY_VOICE_SPEED, 1)) {
            0 -> 0.7f
            1 -> 1.0f
            2 -> 1.3f
            3 -> 1.6f
            else -> 1.0f
        }
    }

    private fun setupListeners() {
        btnPermission.setOnClickListener {
            openNotificationListenerSettings()
        }

        btnTest.setOnClickListener {
            speakTest()
        }

        switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_ENABLED, isChecked).apply()
        }

        spinnerVoiceSpeed.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit().putInt(KEY_VOICE_SPEED, position).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerVoiceLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit().putInt(KEY_LANGUAGE, position).apply()
                setTTSLanguage()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Clear log button
        findViewById<Button>(R.id.btnClearLog).setOnClickListener {
            clearLog()
        }

        // Reset stats button
        findViewById<Button>(R.id.btnResetStats).setOnClickListener {
            resetStats()
        }
    }

    private fun speakTest() {
        if (!isTTSReady) {
            Toast.makeText(this, "Text-to-Speech belum siap", Toast.LENGTH_SHORT).show()
            return
        }
        val langIndex = prefs.getInt(KEY_LANGUAGE, 0)
        val testText = if (langIndex == 0) {
            "Pembayaran QRIS masuk. Lima puluh ribu rupiah. Dari Budi Santoso."
        } else {
            "QRIS payment received. Fifty thousand Rupiah. From Budi Santoso."
        }
        tts.setSpeechRate(getVoiceSpeed())
        tts.speak(testText, TextToSpeech.QUEUE_FLUSH, null, "test")
        addLogEntry("TEST", "Rp50.000", "Budi Santoso")
    }

    private fun updateUI() {
        val hasPermission = isNotificationListenerEnabled()
        if (hasPermission) {
            statusCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.status_active))
            statusIcon.setImageResource(R.drawable.ic_check_circle)
            statusTitle.text = "Aktif & Terhubung"
            statusDesc.text = "Mendengarkan notifikasi DANA..."
            btnPermission.visibility = View.GONE
            btnTest.isEnabled = true
        } else {
            statusCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.status_inactive))
            statusIcon.setImageResource(R.drawable.ic_warning)
            statusTitle.text = "Izin Diperlukan"
            statusDesc.text = "Berikan izin akses notifikasi untuk memulai"
            btnPermission.visibility = View.VISIBLE
            btnTest.isEnabled = false
        }
    }

    fun addLogEntry(type: String, amount: String, sender: String) {
        runOnUiThread {
            transactionCount++
            val amountValue = amount.replace("[^0-9]".toRegex(), "").toLongOrNull() ?: 0L
            totalToday += amountValue

            prefs.edit()
                .putInt(KEY_TXN_COUNT, transactionCount)
                .putLong(KEY_TOTAL_TODAY, totalToday)
                .apply()

            updateStats()

            val itemView = layoutInflater.inflate(R.layout.item_transaction, logContainer, false)
            itemView.findViewById<TextView>(R.id.tvType).text = type
            itemView.findViewById<TextView>(R.id.tvAmount).text = amount
            itemView.findViewById<TextView>(R.id.tvSender).text = sender
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            itemView.findViewById<TextView>(R.id.tvTime).text = sdf.format(Date())

            logContainer.addView(itemView, 0)

            // Keep max 50 entries
            if (logContainer.childCount > 50) {
                logContainer.removeViewAt(logContainer.childCount - 1)
            }

            scrollLog.post { scrollLog.smoothScrollTo(0, 0) }
        }
    }

    private fun updateStats() {
        val formatted = formatRupiah(totalToday)
        tvTotalToday.text = formatted
        tvTransactionCount.text = "$transactionCount transaksi"
    }

    private fun loadStats() {
        totalToday = prefs.getLong(KEY_TOTAL_TODAY, 0L)
        transactionCount = prefs.getInt(KEY_TXN_COUNT, 0)
        updateStats()
    }

    private fun resetDailyStatsIfNeeded() {
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val lastDate = prefs.getString(KEY_LAST_DATE, "")
        if (lastDate != today) {
            prefs.edit()
                .putString(KEY_LAST_DATE, today)
                .putLong(KEY_TOTAL_TODAY, 0L)
                .putInt(KEY_TXN_COUNT, 0)
                .apply()
        }
    }

    private fun clearLog() {
        logContainer.removeAllViews()
    }

    private fun resetStats() {
        AlertDialog.Builder(this)
            .setTitle("Reset Statistik")
            .setMessage("Hapus semua data statistik hari ini?")
            .setPositiveButton("Reset") { _, _ ->
                totalToday = 0L
                transactionCount = 0
                prefs.edit()
                    .putLong(KEY_TOTAL_TODAY, 0L)
                    .putInt(KEY_TXN_COUNT, 0)
                    .apply()
                updateStats()
                clearLog()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":")
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && TextUtils.equals(pkgName, cn.packageName)) return true
            }
        }
        return false
    }

    private fun openNotificationListenerSettings() {
        try {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, "Buka Pengaturan > Notifikasi > Akses Notifikasi", Toast.LENGTH_LONG).show()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "DANA Reader Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Layanan pembaca transaksi DANA"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun formatRupiah(amount: Long): String {
        val formatted = String.format("%,d", amount).replace(",", ".")
        return "Rp$formatted"
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        try { unregisterReceiver(transactionReceiver) } catch (e: Exception) {}
        super.onDestroy()
    }
}
