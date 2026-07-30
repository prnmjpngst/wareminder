package com.dishub.lumajang.wareminder.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import java.util.concurrent.atomic.AtomicBoolean

class WaAutoSendService : AccessibilityService() {

    companion object {
        private var instance: WaAutoSendService? = null
        private val isProcessing = AtomicBoolean(false)
        private var pending: ((AccessibilityNodeInfo) -> Unit)? = null
        private var retries = 0
        private const val MAX_RETRIES = 10

        fun hasInstance(): Boolean = instance != null

        fun sendWithAutoClick(onReady: (AccessibilityNodeInfo) -> Unit) {
            pending = onReady
            isProcessing.set(true)
            retries = 0
            if (instance == null) return
            instance!!.performGlobalAction(GLOBAL_ACTION_RECENTS)
            instance!!.performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    override fun onServiceConnected() {
        instance = this
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!isProcessing.get()) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val currentPackage = event.packageName?.toString() ?: ""
                if (currentPackage.contains("whatsapp", ignoreCase = true)) {
                    retries = 0
                    findAndClickSend()
                } else {
                    retries++
                    if (retries > MAX_RETRIES) {
                        isProcessing.set(false)
                        pending = null
                    }
                }
            }

            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                // Check if the send button was clicked
                val clickedPackage = event.packageName?.toString() ?: ""
                if (clickedPackage.contains("whatsapp", ignoreCase = true)) {
                    val node = event.source
                    if (node != null && isSendButton(node)) {
                        isProcessing.set(false)
                        pending = null
                        node.recycle()
                    }
                }
            }
        }
    }

    private fun findAndClickSend() {
        val root = rootInActiveWindow ?: return
        try {
            val sendButtons = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send")
            if (sendButtons != null && !sendButtons.isEmpty()) {
                val sendButton = sendButtons[0]
                if (sendButton.isEnabled) {
                    sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                sendButton.recycle()
                isProcessing.set(false)
                pending = null
                return
            }

            // Fallback: search by text
            val textNodes = root.findAccessibilityNodeInfosByText("Send")
            if (textNodes != null) {
                for (node in textNodes) {
                    if (isSendButton(node)) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        isProcessing.set(false)
                        pending = null
                        node.recycle()
                        return
                    }
                    node.recycle()
                }
            }

            // Last resort: search with button class name
            searchRecursive(root) { node ->
                if (isSendButton(node)) {
                    if (node.isEnabled) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        isProcessing.set(false)
                        pending = null
                        return@searchRecursive true
                    }
                }
                false
            }
        } finally {
            root.recycle()
        }
    }

    private fun isSendButton(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()?.lowercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        val className = node.className?.toString()?.lowercase() ?: ""
        val isClickable = node.isClickable

        return (text == "send" || contentDesc == "send" || text == "kirim" || contentDesc == "kirim") &&
                isClickable && className.contains("button")
    }

    private fun searchRecursive(node: AccessibilityNodeInfo, f: (AccessibilityNodeInfo) -> Boolean): Boolean {
        if (f(node)) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                if (searchRecursive(child, f)) {
                    child.recycle()
                    return true
                }
            } finally {
                child.recycle()
            }
        }
        return false
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
}
