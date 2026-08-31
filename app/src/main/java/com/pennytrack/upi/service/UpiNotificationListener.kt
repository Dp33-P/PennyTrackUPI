package com.pennytrack.upi.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.pennytrack.upi.PennyTrackApplication
import com.pennytrack.upi.data.model.TransactionSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class UpiNotificationListener : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val raw = listOf(title, text, bigText)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" ")

        if (!looksFinancial(raw)) return

        serviceScope.launch {
            (application as PennyTrackApplication).repository.captureText(
                text = raw,
                source = TransactionSource.NOTIFICATION,
                dateMillis = sbn.postTime
            )
        }
    }

    private fun looksFinancial(raw: String): Boolean {
        val lower = raw.lowercase()
        val hasMoney = lower.contains("rs") || lower.contains("inr") || raw.contains("₹")
        val hasAction = listOf("paid", "debited", "credited", "received", "sent", "refund", "upi")
            .any { lower.contains(it) }
        return hasMoney && hasAction
    }
}
