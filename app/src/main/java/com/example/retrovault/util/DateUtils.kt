package com.example.retrovault.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {
    private val utcTimeZone = TimeZone.getTimeZone("UTC")
    
    private val dbFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        timeZone = utcTimeZone
    }
    private val uiFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).apply {
        timeZone = utcTimeZone
    }
    private val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault()).apply {
        timeZone = utcTimeZone
    }

    fun formatDisplayDate(dateString: String): String {
        if (dateString.isEmpty()) return ""
        return try {
            val date = dbFormat.parse(dateString)
            uiFormat.format(date!!)
        } catch (e: Exception) {
            dateString
        }
    }

    fun formatToDb(date: Date): String = dbFormat.format(date)
    
    fun formatToUi(date: Date): String = uiFormat.format(date)

    fun formatTimestamp(timestamp: Long): String {
        return uiFormat.format(Date(timestamp))
    }

    fun parseDate(dateString: String): Date? {
        if (dateString.isEmpty()) return null
        return try {
            dbFormat.parse(dateString)
        } catch (e: Exception) {
            try {
                yearFormat.parse(dateString)
            } catch (e2: Exception) {
                null
            }
        }
    }
}
