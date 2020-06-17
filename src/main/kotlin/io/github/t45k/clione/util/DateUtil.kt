package io.github.t45k.clione.util

import java.util.Calendar
import java.util.Date

fun Date.minutesAfter(minutes: Int): Date =
    Calendar.getInstance()
        .also {
            it.time = this
            it.add(Calendar.MINUTE, minutes)
        }
        .time
