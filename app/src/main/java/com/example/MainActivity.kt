package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.executor.AutomationEngine
import com.example.services.AutomationForegroundService
import com.example.services.WindowOverlayManager
import com.example.ui.DashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var overlayManager: WindowOverlayManager? = null
    private var automationEngine: AutomationEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Start Foreground Service
        val serviceIntent = Intent(this, AutomationForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // Initialize and start Automation Engine
        val db = com.example.data.AppDatabase.getDatabase(this)
        val repository = com.example.data.AutomationRepository(db)
        automationEngine = AutomationEngine(this, repository)
        automationEngine?.startEngine()

        // Set up overlay manager if permissions are granted
        overlayManager = WindowOverlayManager(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                overlayManager?.showOverlay()
            }
        } else {
            overlayManager?.showOverlay()
        }

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Just to absorb the insets
                    val pad = innerPadding
                    DashboardScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissions()
        // If overlay permission was just granted, show overlay
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                overlayManager?.showOverlay()
            }
        } else {
            overlayManager?.showOverlay()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        automationEngine?.stopEngine()
        overlayManager?.hideOverlay()
    }
}
