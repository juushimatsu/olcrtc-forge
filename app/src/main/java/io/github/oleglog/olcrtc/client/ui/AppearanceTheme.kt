package io.github.oleglog.olcrtc.client.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

internal object AppearanceTheme {
    fun apply(activity: AppCompatActivity) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }
}
