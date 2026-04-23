package com.seagazer.aiimage.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.seagazer.aiimage.data.local.AppPreferences
import com.seagazer.aiimage.domain.AppLanguageOption
import com.seagazer.aiimage.domain.toLocaleListCompat

object AppLocale {
    fun applyFromPreferences(context: Context) {
        AppCompatDelegate.setApplicationLocales(
            AppPreferences.loadAppLanguage(context).toLocaleListCompat(),
        )
    }

    fun applyOption(option: AppLanguageOption) {
        AppCompatDelegate.setApplicationLocales(option.toLocaleListCompat())
    }
}
