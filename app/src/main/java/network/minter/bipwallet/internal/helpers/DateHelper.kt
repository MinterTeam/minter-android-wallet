/*
 * Copyright (C) by MinterTeam. 2021
 * @link <a href="https://github.com/MinterTeam">Org Github</a>
 * @link <a href="https://github.com/edwardstock">Maintainer Github</a>
 *
 * The MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package network.minter.bipwallet.internal.helpers

import android.os.Build
import network.minter.bipwallet.internal.Wallet
import org.joda.time.DateTime
import org.joda.time.Days
import org.joda.time.YearMonth
import org.joda.time.format.DateTimeFormat
import timber.log.Timber
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

/**
 * minter-android-wallet. 2018
 *
 * @author Eduard Maximovich <edward.vstock></edward.vstock>@gmail.com>
 */
object DateHelper {
    /**
     * Example: 2017-08-27T17:25:06+0300
     */
    const val DATE_FORMAT_ISO_8601 = "yyyy-MM-dd'T'HH:mm:ssZ"

    /**
     * Example: 2017-07-28
     */
    const val DATE_FORMAT_SIMPLE = "yyyy-MM-dd"

    /**
     * Example: 2017-07-28 12:00:00+0300
     */
    const val DATE_FORMAT_WITH_TIME = "yyyy-MM-dd HH:mm:ssZ"

    /**
     * Example: 2017-07-28 12:00:00 (GMT +03:00)
     */
    const val DATE_FORMAT_WITH_TZ = "yyyy-MM-dd HH:mm:ss ('GMT' ZZ)"

    /**
     * Example: 2017-07-28 12:00:00
     */
    const val DATE_FORMAT_NO_TZ = "yyyy-MM-dd HH:mm:ss"

    /**
     * Example: 28-07-2017
     */
    const val DATE_FORMAT_SIMPLE_RU = "dd-MM-yyyy"

    /**
     * Example: 25/12
     */
    const val DATE_FORMAT_SHORT = "dd/MM"

    @JvmStatic
    fun getCalendar(format: String, date: String): Calendar? {
        val sdf = SimpleDateFormat(format, Wallet.LC_EN)
        try {
            sdf.parse(date)
        } catch (e: ParseException) {
            Timber.w(e)
            return null
        }
        return sdf.calendar
    }

    @JvmStatic
    fun fromYearMonth(yearMonth: YearMonth): DateTime {
        return DateTime().withYear(yearMonth.year).withMonthOfYear(yearMonth.monthOfYear).withDayOfMonth(1)
    }

    /**
     * Convert string date from sw format to ne
     *
     * @param date       String date value
     * @param fromFormat
     * @param toFormat
     * @return
     */
    @JvmStatic
    fun convertFormat(date: String, fromFormat: String, toFormat: String): String? {
        val cal = getCalendar(fromFormat, date) ?: return null
        val format = SimpleDateFormat(toFormat, Wallet.LC_EN)
        return format.format(cal.time)
    }

    @JvmStatic
    fun format(input: DateTime, format: String): String {
        val fmt = DateTimeFormat.forPattern(format)
        return fmt.print(input)
    }

    fun DateTime.fmt(format: String): String {
        val fmt = DateTimeFormat.forPattern(format)
        return fmt.print(this)
    }

    @JvmStatic
    fun flatDay(d: DateTime): DateTime {
        return d.withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfDay(0)
    }

    @JvmStatic
    fun compareFlatDay(d1: DateTime, d2: DateTime): Boolean {
        return flatDay(d1) == flatDay(d2)
    }

    @JvmStatic
    fun toSimpleISODate(dt: DateTime): String {
        return format(dt, DATE_FORMAT_SIMPLE)
    }

    @JvmStatic
    fun toDateMonthOptYear(d: Date): String {
        val c = Calendar.getInstance()
        c.time = d
        return if (Calendar.getInstance()[Calendar.YEAR] == c[Calendar.YEAR]) {
            String.format(Locale.getDefault(), "%02d.%02d", c[Calendar.DAY_OF_MONTH], c[Calendar.MONTH] + 1)
        } else {
            String.format(Locale.getDefault(), "%02d.%02d.%04d", c[Calendar.DAY_OF_MONTH], c[Calendar.MONTH] + 1, c[Calendar.YEAR])
        }
    }

    fun Int.day(): Days {
        return this.days()
    }

    fun Int.days(): Days {
        return Days.days(this)
    }

    fun Date.formatDateLong(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val df = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
            df.format(toInstant())
        } else {
            val sdf = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
            sdf.format(this)
        }
    }
}