package xyz.pokkst.pokket.lite.util

import android.content.Context
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.*


class DateFormatter {
    companion object {
        fun getFormattedDateFromLong(app: Context?, time: Long): String? {
            var formatter = SimpleDateFormat("M/d@ha", Locale.getDefault())
            var is24HoursFormat = false
            if (app != null) {
                is24HoursFormat =
                    DateFormat.is24HourFormat(app.applicationContext)
                if (is24HoursFormat) {
                    formatter = SimpleDateFormat("M/d H", Locale.getDefault())
                }
            }
            val calendar: Calendar = Calendar.getInstance()
            calendar.timeInMillis = time
            var result: String =
                formatter.format(calendar.time).toLowerCase().replace("am", "a").replace("pm", "p")
            if (is24HoursFormat) result += "h"
            return result
        }
    }
}