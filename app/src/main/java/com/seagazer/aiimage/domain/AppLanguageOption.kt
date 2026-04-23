package com.seagazer.aiimage.domain

import androidx.core.os.LocaleListCompat

enum class AppLanguageOption {
    /** Follows the device locale; clears the app locale override. */
    System,
    English,
    ChineseSimplified,
}

fun AppLanguageOption.toLocaleListCompat(): LocaleListCompat = when (this) {
    AppLanguageOption.System -> LocaleListCompat.getEmptyLocaleList()
    AppLanguageOption.English -> LocaleListCompat.forLanguageTags("en")
    AppLanguageOption.ChineseSimplified -> LocaleListCompat.forLanguageTags("zh-CN")
}
