package com.crescenzi.remagic.system

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import androidx.annotation.MainThread
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import com.crescenzi.remagic.MainActivity
import com.crescenzi.remagic.R
import com.crescenzi.remagic.core.AndroidVersionManager.isAndroid14OrAbove
import com.crescenzi.remagic.core.LOG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// == HANDLE NOTIFICATIONS == //
class NotificationManager {


    private fun createNotificationChannel(context: Context) {
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                context.getString(R.string.notification_channel_id),
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                description =
                    context.getString(R.string.notification_channel_description)
            })
    }


    @MainThread
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun newNotification(
        ctx: Context,
        title: String, body: String
    ) {
        try {
            if (ContextCompat.checkSelfPermission(
                    ctx,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                LOG("Insufficient Notification permission"); return;
            }

            createNotificationChannel(ctx)


            // == DO NOT CHANGE THE ACTIVITY::CLASS , BECAUSE USER HAS TO PASS BY BIOMETRIC AUTH (ONLY PRESENT INTO StartActivity) == //
            val intent = Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                ctx, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(
                ctx,
                ctx.getString(R.string.notification_channel_id)
            )
                .setSmallIcon(R.drawable.magician_head)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(1000, 1000, 1000, 1000))
                .setContentIntent(pendingIntent)
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(body)
                )  // Long Message Beautification //

            isAndroid14OrAbove {
                val drawable = ContextCompat.getDrawable(ctx, R.drawable.magician_head)
                val bitmap = (drawable as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    val scaled = bitmap.scale(128, 128, false)
                    builder.setLargeIcon(Icon.createWithBitmap(scaled))
                }
            }

            val notification = builder.build()

            NotificationManagerCompat.from(ctx).notify(1001, notification)
            LOG("Successo invio Notifica")
        } catch (e: Exception) {
            LOG("Errore Invio notifica => ${e.message}")
        }
    }

}