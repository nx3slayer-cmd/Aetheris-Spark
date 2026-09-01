package com.kallistocore.ai.data.manager

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

enum class AppIconStyle(
    val aliasName: String,
    val displayName: String,
    val isProOnly: Boolean
) {
    FREE("MainActivityAliasFree", "Obsidian Indigo", false),
    PRO_PLATINUM("MainActivityAliasProPlatinum", "Holographic Platinum (Pro)", true),
    PRO_GOLD("MainActivityAliasProGold", "24K Obsidian Gold (Pro)", true),
    PRO_AMETHYST("MainActivityAliasProAmethyst", "Royal Amethyst (Pro)", true),
    PRO_EMERALD("MainActivityAliasProEmerald", "Quantum Emerald (Pro)", true)
}

object AppIconManager {

    private const val PACKAGE_NAME = "com.kallistocore.ai"

    fun setAppIcon(context: Context, selectedStyle: AppIconStyle) {
        try {
            val packageManager = context.packageManager
            AppIconStyle.values().forEach { style ->
                val componentName = ComponentName(PACKAGE_NAME, "$PACKAGE_NAME.${style.aliasName}")
                val newState = if (style == selectedStyle) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }
                packageManager.setComponentEnabledSetting(
                    componentName,
                    newState,
                    PackageManager.DONT_KILL_APP
                )
            }
        } catch (_: Exception) {}
    }

    fun getActiveAppIcon(context: Context): AppIconStyle {
        try {
            val packageManager = context.packageManager
            for (style in AppIconStyle.values()) {
                val componentName = ComponentName(PACKAGE_NAME, "$PACKAGE_NAME.${style.aliasName}")
                val state = packageManager.getComponentEnabledSetting(componentName)
                if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                    return style
                }
            }
        } catch (_: Exception) {}
        return AppIconStyle.FREE
    }
}
