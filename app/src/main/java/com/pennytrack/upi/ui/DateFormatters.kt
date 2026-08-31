package com.pennytrack.upi.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateFormatters {
    private val zone = ZoneId.systemDefault()
    private val dayFormatter = DateTimeFormatter.ofPattern("dd MMM")
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    private val monthFormatter = DateTimeFormatter.ofPattern("MMM yyyy")

    fun dayTime(millis: Long): String {
        val localDateTime = Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime()
        return "${localDateTime.format(dayFormatter)}, ${localDateTime.format(timeFormatter)}"
    }

    fun monthLabel(millis: Long = System.currentTimeMillis()): String {
        return Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().format(monthFormatter)
    }
}
