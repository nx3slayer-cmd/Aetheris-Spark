package com.kallistocore.ai.data.manager

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

enum class AppIconStyle(
    val aliasName: String,
    val displayName: String,
    val isProOnly: Boolean,
    val previewDrawableName: String
) {
    FREE("MainActivityAliasFree", "Obsidian Indigo", false, "ic_launcher_free"),
    PRO_PLATINUM("MainActivityAliasProPlatinum", "Holographic Platinum (Pro)", true, "ic_launcher_pro_platinum"),
    PRO_GOLD("MainActivityAliasProGold", "24K Obsidian Gold (Pro)", true, "ic_launcher_pro_gold"),
    PRO_AMETHYST("MainActivityAliasProAmethyst", "Royal Amethyst (Pro)", true, "ic_launcher_pro_amethyst"),
    PRO_EMERALD("MainActivityAliasProEmerald", "Quantum Emerald (Pro)", true, "ic_launcher_pro_emerald")
}

object AppIconManager {

    private const val PACKAGE_NAME = "com.kallistocore.ai"

    /**
     * Dynamically switches the active Android launcher icon on the user's home screen.
     */
    fun setAppIcon(context: Context, selectedStyle: AppIconStyle) {
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
    }

    /**
     * Determines which launcher icon is currently active.
     */
    fun getActiveAppIcon(context: Context): AppIconStyle {
        val packageManager = context.packageManager
        for (style in AppIconStyle.values()) {
            val componentName = ComponentName(PACKAGE_NAME, "$PACKAGE_NAME.${style.aliasName}")
            val state = packageManager.getComponentEnabledSetting(componentName)
            if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                return style
            }
        }
        return AppIconStyle.FREE
    }
}
