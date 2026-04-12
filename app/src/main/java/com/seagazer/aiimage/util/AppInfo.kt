package com.seagazer.aiimage.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

fun Context.resolveAppVersionName(): String = runCatching {
    val pm = packageManager
    val pkg = packageName
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0)).versionName
    } else {
        @Suppress("DEPRECATION")
        pm.getPackageInfo(pkg, 0).versionName
    }
}.getOrNull().orEmpty()
