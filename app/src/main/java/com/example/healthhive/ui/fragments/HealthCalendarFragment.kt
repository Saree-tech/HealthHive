package com.example.healthhive.ui.fragments

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthhive.ui.screens.HealthCalendarScreen
import com.example.healthhive.viewmodel.HealthCalendarViewModel

class HealthCalendarFragment : Fragment() {

    // 1. Initialize the launcher at the class level
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(requireContext(), "Notifications required for reminders", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val sharedPrefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val showProgress = sharedPrefs.getBoolean("show_daily_progress", true)

        // 2. Check for permissions
        checkPermissions()

        return ComposeView(requireContext()).apply {
            setContent {
                val viewModel: HealthCalendarViewModel = viewModel()

                HealthCalendarScreen(
                    onBack = {
                        activity?.onBackPressedDispatcher?.onBackPressed()
                    },
                    viewModel = viewModel,
                    showProgressFromPrefs = showProgress
                )
            }
        }
    }

    private fun checkPermissions() {
        val context = requireContext()

        // Android 13 (API 33) Notification Permission
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Android 14 (API 34) Exact Alarm Permission
        if (Build.VERSION.SDK_INT >= 34) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                startActivity(intent)
            }
        }
    }
}