package com.kallistocore.ai.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.kallistocore.ai.data.manager.AppIconStyle
import com.kallistocore.ai.ui.theme.AppThemeSetting

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("kallisto_master_settings", Context.MODE_PRIVATE)

    var theme: AppThemeSetting
        get() {
            val name = prefs.getString("app_theme", AppThemeSetting.MIDNIGHT_DARK.name)
            return try { AppThemeSetting.valueOf(name ?: AppThemeSetting.MIDNIGHT_DARK.name) } catch (_: Exception) { AppThemeSetting.MIDNIGHT_DARK }
        }
        set(value) = prefs.edit().putString("app_theme", value.name).apply()

    var activeIcon: AppIconStyle
        get() {
            val name = prefs.getString("app_icon", AppIconStyle.FREE.name)
            return try { AppIconStyle.valueOf(name ?: AppIconStyle.FREE.name) } catch (_: Exception) { AppIconStyle.FREE }
        }
        set(value) = prefs.edit().putString("app_icon", value.name).apply()

    var memoryAllocationMB: Int
        get() = prefs.getInt("memory_mb", 1024)
        set(value) = prefs.edit().putInt("memory_mb", value).apply()

    var contextWindowSize: Int
        get() = prefs.getInt("context_window", 4096)
        set(value) = prefs.edit().putInt("context_window", value).apply()

    var cpuThreads: Int
        get() = prefs.getInt("cpu_threads", 6)
        set(value) = prefs.edit().putInt("cpu_threads", value).apply()

    var systemPrompt: String
        get() = prefs.getString("system_prompt", "You are Kallisto, a sovereign, local, and deeply intelligent AI companion.") ?: ""
        set(value) = prefs.edit().putString("system_prompt", value).apply()

    var voiceProfile: String
        get() = prefs.getString("voice_profile", "af_nicole (Soft Narrative)") ?: "af_nicole (Soft Narrative)"
        set(value) = prefs.edit().putString("voice_profile", value).apply()

    var voiceSpeed: Float
        get() = prefs.getFloat("voice_speed", 1.0f)
        set(value) = prefs.edit().putFloat("voice_speed", value).apply()

    var voicePitch: Float
        get() = prefs.getFloat("voice_pitch", 1.0f)
        set(value) = prefs.edit().putFloat("voice_pitch", value).apply()

    var isVoiceAutoSpeak: Boolean
        get() = prefs.getBoolean("voice_auto_speak", true)
        set(value) = prefs.edit().putBoolean("voice_auto_speak", value).apply()
}
