package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "automation_rules")
data class AutomationRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val triggerType: String, // TIME, NOTIFICATION, LOCATION
    val triggerValue: String, // e.g., "08:30", "WhatsApp:Hello", "lat,lng"
    val actionType: String,   // OPEN_APP, SEND_SMS, TTS, TOGGLE_WIFI, TOGGLE_BLUETOOTH
    val actionValue: String,  // e.g., package_name, phone_number:msg, text to speak
    val isActive: Boolean = true,
    val description: String
)
