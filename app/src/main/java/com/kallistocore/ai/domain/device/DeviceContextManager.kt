package com.kallistocore.ai.domain.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeviceContextManager(private val context: Context) {

    fun getCurrentDateTimeSummary(): String {
        val now = Date()
        val dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(now)
        val fullDate = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(now)
        val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(now)
        val timeZone = SimpleDateFormat("z", Locale.getDefault()).format(now)

        return "Today is $dayOfWeek, $fullDate. Current time is $time ($timeZone)."
    }

    fun getBatteryStatus(): String {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100
        return "$pct%"
    }

    fun getDeviceModelInfo(): String {
        return "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
    }

    fun openDefaultBrowserSearch(query: String) {
        val encoded = Uri.encode(query)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://duckduckgo.com/?q=$encoded")).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
