package ru.mahnet.buyer

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** Приём уведомлений от сайта (Firebase). Когда приложение закрыто, показывает их сама система. */
class PushService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Push.saveToken(this, token)
    }

    // Вызывается, когда приложение открыто: показываем уведомление сами, чтобы оно было заметно
    override fun onMessageReceived(msg: RemoteMessage) {
        val n = msg.notification ?: return
        Push.ensureChannel(this)
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val b = NotificationCompat.Builder(this, Push.CHANNEL)
            .setSmallIcon(R.drawable.ic_notify)
            .setColor(0xFFFF5B24.toInt())
            .setContentTitle(n.title ?: "mahnet")
            .setContentText(n.body ?: "")
            .setStyle(NotificationCompat.BigTextStyle().bigText(n.body ?: ""))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(open)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), b.build())
    }
}
