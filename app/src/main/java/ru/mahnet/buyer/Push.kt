package ru.mahnet.buyer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.webkit.CookieManager
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/** Общее для уведомлений: канал, сохранение кода устройства, передача его на сайт. */
object Push {
    const val CHANNEL = "orders"
    private const val PREFS = "push"

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL) != null) return
        val ch = NotificationChannel(CHANNEL, "Заказы и сообщения", NotificationManager.IMPORTANCE_HIGH)
        ch.description = "Статус заказа, ответы продавца, возвраты"
        nm.createNotificationChannel(ch)
    }

    fun saveToken(ctx: Context, token: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("token", token).putString("sent", "").apply()
    }

    fun token(ctx: Context): String = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("token", "") ?: ""

    /**
     * Сообщает сайту: «это устройство принадлежит этому покупателю». Вошедший покупатель определяется по cookie сессии,
     * гость — по случайному коду, который сайт хранит у себя. Повторно отправляется только если что-то поменялось.
     */
    fun sync(ctx: Context, gid: String) {
        val token = token(ctx)
        if (token.isEmpty()) return
        val cookie = CookieManager.getInstance().getCookie(MainActivity.SITE_URL) ?: ""
        val session = cookie.split(";").map { it.trim() }.firstOrNull { it.startsWith("MKTBUYER=") } ?: ""
        if (session.isEmpty() && gid.isEmpty()) return   // гость без заказов: сообщать пока нечего
        val key = session + "|" + gid
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString("sent", "") == key) return
        thread {
            try {
                val c = URL(MainActivity.SITE_URL + "/buyer.php?a=push_reg").openConnection() as HttpURLConnection
                c.requestMethod = "POST"
                c.connectTimeout = 8000
                c.readTimeout = 8000
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                if (cookie.isNotEmpty()) c.setRequestProperty("Cookie", cookie)
                val body = JSONObject().put("token", token).put("gid", gid).toString()
                c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                if (c.responseCode == 200) prefs.edit().putString("sent", key).apply()
                c.disconnect()
            } catch (e: Exception) {
                // нет связи — попробуем при следующей проверке
            }
        }
    }
}
