package com.seagazer.aiimage.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.seagazer.aiimage.MainActivity
import com.seagazer.aiimage.R

/**
 * Ongoing status notification while a ComfyUI generation is in progress and the app is in the background.
 * Updated from [showOrUpdate]; dismissed via [cancel].
 */
object GenerationProgressNotification {

    const val CHANNEL_ID: String = "generation_progress"
    private const val NOTIFICATION_ID: Int = 41_001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val name = context.getString(R.string.notification_channel_generation_name)
        val ch = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW).apply {
            description = context.getString(R.string.notification_channel_generation_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(ch)
    }

    fun showOrUpdate(context: Context, progressPercent: Int?) {
        val app = context.applicationContext
        ensureChannel(app)
        val open = Intent(app, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            app,
            0,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val indeterminate = progressPercent == null
        val subText = if (indeterminate) {
            app.getString(R.string.notification_generating_waiting_progress)
        } else {
            app.getString(R.string.generating_progress_percent, progressPercent)
        }
        val n = NotificationCompat.Builder(app, CHANNEL_ID)
            .setSmallIcon(R.drawable.icon)
            .setContentTitle(app.getString(R.string.generating_title))
            .setContentText(subText)
            .setSubText(null)
            .setStyle(NotificationCompat.BigTextStyle().bigText(subText))
            .setProgress(100, progressPercent?.coerceIn(0, 100) ?: 0, indeterminate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(pending)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        NotificationManagerCompat.from(app).notify(NOTIFICATION_ID, n)
    }

    fun cancel(context: Context) {
        val app = context.applicationContext
        NotificationManagerCompat.from(app).cancel(NOTIFICATION_ID)
    }
}
