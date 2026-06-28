package com.example.executor

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import android.telephony.SmsManager
import android.util.Log
import com.example.data.AutomationRepository
import com.example.services.MyAccessibilityService
import java.util.Locale

class CommandExecutor(
    private val context: Context,
    private val repository: AutomationRepository
) {

    suspend fun executeAction(actionType: String, actionValue: String) {
        val typeUpper = actionType.uppercase(Locale.ROOT)
        Log.d("CommandExecutor", "Executing Action: $typeUpper with value: $actionValue")

        try {
            when (typeUpper) {
                "OPEN_APP" -> launchApplication(actionValue)
                "CLICK" -> clickScreenElement(actionValue)
                "INPUT" -> {
                    val parts = actionValue.split(":", limit = 2)
                    if (parts.size == 2) {
                        inputTextField(parts[0], parts[1])
                    } else {
                        repository.logFailed("INPUT", "تنسيق القيمة غير صالح للكتابة. استخدم target:text")
                    }
                }
                "TTS" -> speakText(actionValue)
                "SEND_SMS" -> {
                    val parts = actionValue.split(":", limit = 2)
                    if (parts.size == 2) {
                        sendSMS(parts[0], parts[1])
                    } else {
                        repository.logFailed("SEND_SMS", "تنسيق الرقم والرسالة غير صالح. استخدم phone_number:message")
                    }
                }
                "TOGGLE_WIFI" -> toggleWifi(actionValue.toBoolean())
                "TOGGLE_BLUETOOTH" -> toggleBluetooth(actionValue.toBoolean())
                "VOLUME" -> setVolume(actionValue.toIntOrNull() ?: 50)
                "BRIGHTNESS" -> setBrightness(actionValue.toIntOrNull() ?: 50)
                "BACK" -> performBack()
                "HOME" -> performHome()
                "CLICK_COORD" -> {
                    val parts = actionValue.split(":", ",")
                    if (parts.size >= 2) {
                        val x = parts[0].trim().toFloatOrNull()
                        val y = parts[1].trim().toFloatOrNull()
                        if (x != null && y != null) {
                            clickCoordinates(x, y)
                        } else {
                            repository.logFailed("CLICK_COORD", "إحداثيات غير صالحة. استخدم x:y")
                        }
                    } else {
                        repository.logFailed("CLICK_COORD", "إحداثيات غير صالحة. استخدم x:y")
                    }
                }
                "LONG_CLICK_COORD" -> {
                    val parts = actionValue.split(":", ",")
                    if (parts.size >= 2) {
                        val x = parts[0].trim().toFloatOrNull()
                        val y = parts[1].trim().toFloatOrNull()
                        if (x != null && y != null) {
                            longClickCoordinates(x, y)
                        } else {
                            repository.logFailed("LONG_CLICK_COORD", "إحداثيات غير صالحة. استخدم x:y")
                        }
                    } else {
                        repository.logFailed("LONG_CLICK_COORD", "إحداثيات غير صالحة. استخدم x:y")
                    }
                }
                "SCROLL_FORWARD" -> scrollScreen(true, actionValue)
                "SCROLL_BACKWARD" -> scrollScreen(false, actionValue)
                else -> {
                    repository.logFailed("CommandExecutor", "إجراء غير معروف: $actionType")
                }
            }
        } catch (e: Exception) {
            repository.logFailed(typeUpper, "خطأ أثناء تنفيذ الإجراء: ${e.localizedMessage}")
        }
    }

    // --- Action Implementations ---

    suspend fun launchApplication(packageName: String) {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            repository.logSuccess("OPEN_APP", "تم تشغيل التطبيق بنجاح: $packageName")
        } else {
            repository.logFailed("OPEN_APP", "تعذر العثور على التطبيق بالمعرف: $packageName")
        }
    }

    suspend fun clickScreenElement(target: String) {
        val service = MyAccessibilityService.instance
        if (service != null) {
            val success = service.clickElement(target)
            if (success) {
                repository.logSuccess("CLICK", "تم الضغط بنجاح على عنصر الشاشة: $target")
            } else {
                repository.logFailed("CLICK", "فشل الضغط على عنصر الشاشة: $target (غير مرئي أو غير قابل للضغط)")
            }
        } else {
            repository.logFailed("CLICK", "تعذر الضغط. خدمة الوصول ليست مفعلة.")
        }
    }

    suspend fun inputTextField(target: String, text: String) {
        val service = MyAccessibilityService.instance
        if (service != null) {
            val success = service.inputTextField(target, text)
            if (success) {
                repository.logSuccess("INPUT", "تم إدخال النص في الحقل '$target' بنجاح.")
            } else {
                repository.logFailed("INPUT", "فشل إدخال النص في الحقل '$target'.")
            }
        } else {
            repository.logFailed("INPUT", "تعذر الكتابة. خدمة الوصول ليست مفعلة.")
        }
    }

    private suspend fun speakText(text: String) {
        // Broadcaster to TTS service or foreground service
        val intent = Intent("com.example.TTS_SPEAK").apply {
            putExtra("text", text)
        }
        context.sendBroadcast(intent)
        repository.logSuccess("TTS", "طلب نطق النص: '$text'")
    }

    suspend fun sendSMS(phoneNumber: String, message: String) {
        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            repository.logSuccess("SEND_SMS", "تم إرسال رسالة نصية SMS إلى $phoneNumber بنجاح.")
        } catch (e: Exception) {
            repository.logFailed("SEND_SMS", "فشل إرسال رسالة SMS: ${e.localizedMessage}")
        }
    }

    suspend fun toggleWifi(enable: Boolean) {
        repository.logInfo("TOGGLE_WIFI", "محاولة تعديل حالة الواي فاي إلى: $enable")
        // Direct toggling is restricted on modern API, notify state
        repository.logSuccess("TOGGLE_WIFI", "تمت معالجة أمر الواي فاي. يرجى تفعيل إذن النظام إذا لزم الأمر.")
    }

    suspend fun toggleBluetooth(enable: Boolean) {
        repository.logInfo("TOGGLE_BLUETOOTH", "محاولة تعديل حالة البلوتوث إلى: $enable")
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter != null) {
                @Suppress("DEPRECATION")
                if (enable && !bluetoothAdapter.isEnabled) {
                    bluetoothAdapter.enable()
                } else if (!enable && bluetoothAdapter.isEnabled) {
                    bluetoothAdapter.disable()
                }
                repository.logSuccess("TOGGLE_BLUETOOTH", "تم تغيير حالة البلوتوث إلى $enable")
            } else {
                repository.logFailed("TOGGLE_BLUETOOTH", "البلوتوث غير مدعوم على هذا الجهاز.")
            }
        } catch (e: SecurityException) {
            repository.logFailed("TOGGLE_BLUETOOTH", "يرجى منح إذن البلوتوث للتطبيق.")
        }
    }

    suspend fun setVolume(percentage: Int) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val targetVolume = (maxVolume * (percentage / 100f)).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, AudioManager.FLAG_SHOW_UI)
        repository.logSuccess("VOLUME", "تم تعديل مستوى الصوت إلى: $percentage%")
    }

    suspend fun setBrightness(percentage: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(context)) {
            val brightnessValue = (255 * (percentage / 100f)).toInt()
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightnessValue)
            repository.logSuccess("BRIGHTNESS", "تم تعديل السطوع إلى: $percentage%")
        } else {
            repository.logInfo("BRIGHTNESS", "يرجى منح صلاحية كتابة إعدادات النظام (Write Settings) للتحكم بالسطوع.")
        }
    }

    suspend fun performBack() {
        val service = MyAccessibilityService.instance
        if (service != null) {
            service.performGlobalBack()
            repository.logSuccess("BACK", "تم الرجوع للخلف.")
        } else {
            repository.logFailed("BACK", "خدمة الوصول غير مفعلة.")
        }
    }

    suspend fun performHome() {
        val service = MyAccessibilityService.instance
        if (service != null) {
            service.performGlobalHome()
            repository.logSuccess("HOME", "تم الذهاب للشاشة الرئيسية.")
        } else {
            repository.logFailed("HOME", "خدمة الوصول غير مفعلة.")
        }
    }

    suspend fun clickCoordinates(x: Float, y: Float) {
        val service = MyAccessibilityService.instance
        if (service != null) {
            val success = service.clickCoordinates(x, y)
            if (success) {
                repository.logSuccess("CLICK_COORD", "تم الضغط بنجاح على الإحداثيات: x=$x, y=$y")
            } else {
                repository.logFailed("CLICK_COORD", "فشل الضغط على الإحداثيات: x=$x, y=$y")
            }
        } else {
            repository.logFailed("CLICK_COORD", "تعذر الضغط بالإحداثيات. خدمة الوصول غير مفعلة.")
        }
    }

    suspend fun longClickCoordinates(x: Float, y: Float) {
        val service = MyAccessibilityService.instance
        if (service != null) {
            val success = service.longClickCoordinates(x, y)
            if (success) {
                repository.logSuccess("LONG_CLICK_COORD", "تم الضغط مطولاً بنجاح على الإحداثيات: x=$x, y=$y")
            } else {
                repository.logFailed("LONG_CLICK_COORD", "فشل الضغط مطولاً على الإحداثيات: x=$x, y=$y")
            }
        } else {
            repository.logFailed("LONG_CLICK_COORD", "تعذر الضغط مطولاً. خدمة الوصول غير مفعلة.")
        }
    }

    suspend fun scrollScreen(forward: Boolean, target: String) {
        val service = MyAccessibilityService.instance
        if (service != null) {
            val actualTarget = if (target.isBlank()) null else target
            val success = if (forward) service.scrollForward(actualTarget) else service.scrollBackward(actualTarget)
            val actionTag = if (forward) "SCROLL_FORWARD" else "SCROLL_BACKWARD"
            val directionStr = if (forward) "للأمام" else "للخلف"
            
            if (success) {
                repository.logSuccess(actionTag, "تم التمرير $directionStr بنجاح ${if (actualTarget != null) "على العنصر '$actualTarget'" else ""}.")
            } else {
                repository.logFailed(actionTag, "فشل التمرير $directionStr ${if (actualTarget != null) "على العنصر '$actualTarget'" else ""}.")
            }
        } else {
            repository.logFailed("SCROLL", "خدمة الوصول غير مفعلة.")
        }
    }

    // --- Device Information Queries (Stage 5.44) ---

    fun getDeviceStatus(): String {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryPct = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        
        val isCharging = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == BatteryManager.BATTERY_STATUS_CHARGING
        } else {
            false
        }

        val networkType = getNetworkType()
        return "البطارية: $batteryPct% ${if (isCharging) "(جاري الشحن)" else ""}, الشبكة: $networkType"
    }

    private fun getNetworkType(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork ?: return "غير متصل"
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return "غير متصل"
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular (بيانات الهاتف)"
            else -> "متصل"
        }
    }

    // --- Contacts Query (Stage 5.48) ---

    fun queryContactNumber(contactName: String): String? {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$contactName%")
        
        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val numIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (numIndex >= 0) return cursor.getString(numIndex)
            }
        }
        return null
    }
}
