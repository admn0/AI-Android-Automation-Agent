package com.example.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.AutomationRepository
import com.example.data.LogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale

class AutomationForegroundService : Service(), TextToSpeech.OnInitListener {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private lateinit var repository: AutomationRepository
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    private val CHANNEL_ID = "ai_automation_channel"
    private val NOTIFICATION_ID = 88

    private val ttsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val text = intent?.getStringExtra("text") ?: return
            speak(text)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getDatabase(this)
        repository = AutomationRepository(db)

        // Initialize TTS
        tts = TextToSpeech(this, this)

        // Create Channel and start foreground
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("محرك الأتمتة والذكاء الاصطناعي نشط في الخلفية."))

        // Register TTS speak receiver
        @Suppress("UnspecifiedRegisterReceiverFlag")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(ttsReceiver, IntentFilter("com.example.TTS_SPEAK"), RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(ttsReceiver, IntentFilter("com.example.TTS_SPEAK"))
        }

        serviceScope.launch {
            repository.insertLog(
                LogEntry(
                    actionName = "ForegroundService",
                    status = "SUCCESS",
                    message = "تم تشغيل الخدمة الخلفية المستمرة بنجاح."
                )
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle trigger processing or voice wake intents
        val textToSpeak = intent?.getStringExtra("speak_text")
        if (textToSpeak != null) {
            speak(textToSpeak)
        }
        return START_STICKY
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("ar"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to English if Arabic is missing
                tts?.language = Locale.US
            }
            isTtsInitialized = true
            Log.d("ForegroundService", "TTS initialized successfully.")
        } else {
            Log.e("ForegroundService", "TTS initialization failed.")
        }
    }

    fun speak(text: String) {
        if (isTtsInitialized && tts != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "fgs_tts_id")
            } else {
                @Suppress("DEPRECATION")
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "مساعد الأتمتة بالذكاء الاصطناعي",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعارات حالة التشغيل المستمر لمساعد الأتمتة بالذكاء الاصطناعي"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("مساعد الأتمتة الذكي")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(ttsReceiver)
        tts?.stop()
        tts?.shutdown()
        serviceScope.cancel()
        Log.d("ForegroundService", "Service destroyed.")
    }
}
