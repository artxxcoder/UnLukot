package com.unlukot.app.data

import android.content.Context
import com.unlukot.app.ai.FlattenMode

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

enum class ResultTab(val label: String) { COMPONENTS("Components"), TEXT("Text"), JSON("JSON") }

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val flatten: FlattenMode = FlattenMode.AUTO,
    val saveHistory: Boolean = true,
    val showBoxes: Boolean = true,
    val haptics: Boolean = true,
    val defaultTab: ResultTab = ResultTab.COMPONENTS
)

/** Small key-value store for user preferences. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        theme = enumOr(prefs.getString(KEY_THEME, null), ThemeMode.SYSTEM),
        flatten = enumOr(prefs.getString(KEY_FLATTEN, null), FlattenMode.AUTO),
        saveHistory = prefs.getBoolean(KEY_SAVE, true),
        showBoxes = prefs.getBoolean(KEY_BOXES, true),
        haptics = prefs.getBoolean(KEY_HAPTICS, true),
        defaultTab = enumOr(prefs.getString(KEY_TAB, null), ResultTab.COMPONENTS)
    )

    fun save(s: AppSettings) {
        prefs.edit()
            .putString(KEY_THEME, s.theme.name)
            .putString(KEY_FLATTEN, s.flatten.name)
            .putBoolean(KEY_SAVE, s.saveHistory)
            .putBoolean(KEY_BOXES, s.showBoxes)
            .putBoolean(KEY_HAPTICS, s.haptics)
            .putString(KEY_TAB, s.defaultTab.name)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        name?.let { n -> enumValues<T>().firstOrNull { it.name == n } } ?: default

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_FLATTEN = "flatten"
        const val KEY_SAVE = "save_history"
        const val KEY_BOXES = "show_boxes"
        const val KEY_HAPTICS = "haptics"
        const val KEY_TAB = "default_tab"
    }
}
