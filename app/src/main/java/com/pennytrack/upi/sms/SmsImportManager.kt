package com.pennytrack.upi.sms

import android.content.Context
import android.provider.Telephony

data class SmsMessageSnapshot(
    val body: String,
    val dateMillis: Long,
    val address: String?
)

object SmsImportManager {
    fun readInbox(context: Context, maxMessages: Int): List<SmsMessageSnapshot> {
        val messages = mutableListOf<SmsMessageSnapshot>()
        val projection = arrayOf(
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.ADDRESS
        )

        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            null,
            null,
            "${Telephony.Sms.DATE} DESC"
        )?.use { cursor ->
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)

            while (cursor.moveToNext() && messages.size < maxMessages) {
                messages += SmsMessageSnapshot(
                    body = cursor.getString(bodyIndex).orEmpty(),
                    dateMillis = cursor.getLong(dateIndex),
                    address = cursor.getString(addressIndex)
                )
            }
        }

        return messages
    }
}
