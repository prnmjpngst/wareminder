package com.dishub.lumajang.wareminder.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

class WaAutoSendService : AccessibilityService() {

    companion object {
        private var instance: WaAutoSendService? = null
        private val isProcessing = AtomicBoolean(false)
        private var pendingMessage: String? = null
        private var retries = 0
        private const val MAX_RETRIES = 30
        private var phase = Phase.WAITING_WINDOW

        fun hasInstance(): Boolean = instance != null

        fun sendWithTyping(message: String) {
            pendingMessage = message
            isProcessing.set(true)
            retries = 0
            phase = Phase.WAITING_WINDOW
        }

        enum class Phase { WAITING_WINDOW, TYPING, CLICKING_SEND, DONE }
    }

    override fun onServiceConnected() {
        instance = this
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 200
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!isProcessing.get()) return
        if (phase == Phase.DONE) return

        val pkg = event.packageName?.toString() ?: ""
        if (!pkg.contains("whatsapp", ignoreCase = true)) return

        when (phase) {
            Phase.WAITING_WINDOW -> handleWindowReady()
            Phase.TYPING -> {} // typing is done on a background thread
            Phase.CLICKING_SEND -> handleClickSend()
            Phase.DONE -> {}
        }
    }

    private fun handleWindowReady() {
        val root = rootInActiveWindow ?: return
        val message = pendingMessage ?: return

        try {
            val inputField = findInputField(root)
            if (inputField != null) {
                phase = Phase.TYPING
                Thread {
                    typeMessage(inputField, message)
                    kotlinx.coroutines.runBlocking { kotlinx.coroutines.delay(500) }
                    phase = Phase.CLICKING_SEND
                    handleClickSend()
                }.start()
                return
            }
        } finally {
            root.recycle()
        }

        retries++
        if (retries > MAX_RETRIES) {
            isProcessing.set(false)
            pendingMessage = null
        }
    }

    private fun findInputField(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val byId = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/entry")
        if (byId != null && byId.size > 0) {
            val field = byId[0]
            for (i in 1 until byId.size) byId[i].recycle()
            return field
        }

        val byId2 = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/conversationEntry")
        if (byId2 != null && byId2.size > 0) {
            val field = byId2[0]
            for (i in 1 until byId2.size) byId2[i].recycle()
            return field
        }

        // Fallback: search for editable text fields
        return findEditableNode(root)
    }

    private fun findEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findEditableNode(child)
            child.recycle()
            if (result != null) return result
        }
        return null
    }

    private fun typeMessage(inputField: AccessibilityNodeInfo, message: String) {
        try {
            inputField.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            Thread.sleep(200)

            // Type character by character
            for (i in message.indices) {
                if (phase == Phase.DONE) break
                val currentText = message.substring(0, i + 1)
                val bundle = Bundle()
                bundle.putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    currentText
                )
                inputField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
                val delay = Random.nextLong(80, 200)
                Thread.sleep(delay)
            }

            // Natural pause after typing complete message
            Thread.sleep(Random.nextLong(500, 1500))

            // Type a few extra chars naturally (simulate editing)
            val extras = listOf(" ", "\n", " ")
            for (extra in extras) {
                val bundle = Bundle()
                bundle.putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    message + extra
                )
                inputField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
                Thread.sleep(Random.nextLong(200, 400))
            }
        } catch (e: Exception) {
            // Continue to send
        } finally {
            inputField.recycle()
        }
    }

    private fun handleClickSend() {
        val root = rootInActiveWindow ?: return
        try {
            val sendButtons = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send")
            if (sendButtons != null && sendButtons.isNotEmpty()) {
                val btn = sendButtons[0]
                if (btn.isEnabled && btn.isClickable) {
                    btn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                btn.recycle()
                for (i in 1 until sendButtons.size) sendButtons[i].recycle()
                finish()
                return
            }

            // Fallback: search by text or content description
            for (i in 0 until root.childCount) {
                val child = root.getChild(i) ?: continue
                if (findAndClickSendRecursive(child)) {
                    child.recycle()
                    finish()
                    return
                }
                child.recycle()
            }
        } finally {
            root.recycle()
        }
    }

    private fun findAndClickSendRecursive(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()?.lowercase() ?: ""
        val cd = node.contentDescription?.toString()?.lowercase() ?: ""
        if ((text == "send" || cd == "send" || text == "kirim" || cd == "kirim") &&
            node.isClickable && node.isEnabled
        ) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickSendRecursive(child)) {
                child.recycle()
                return true
            }
            child.recycle()
        }
        return false
    }

    private fun finish() {
        isProcessing.set(false)
        pendingMessage = null
        phase = Phase.DONE
        Thread { Thread.sleep(2000) }.start()
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
}
