/*
 * Copyright (C) 2018 by MinterTeam
 * @link https://github.com/MinterTeam
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

package network.minter.bipwallet.internal.helpers;

import android.support.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.YearMonth;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import network.minter.bipwallet.internal.Wallet;
import timber.log.Timber;

/**
 * Dogsy. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class DateHelper {

    /**
     * Example: 2017-08-27T17:25:06+0300
     */
    public final static String DATE_FORMAT_ISO_8601 = "yyyy-MM-dd'T'HH:mm:ssZ";
    /**
     * Example: 2017-07-28
     */
    public final static String DATE_FORMAT_SIMPLE = "yyyy-MM-dd";
    /**
     * Example: 2017-07-28 12:00:00+0300
     */
    public final static String DATE_FORMAT_WITH_TIME = "yyyy-MM-dd HH:mm:ssZ";

    /**
     * Example: 2017-07-28 12:00:00
     */
    public final static String DATE_FORMAT_NO_TZ = "yyyy-MM-dd HH:mm:ss";
    /**
     * Example: 28-07-2017
     */
    public final static String DATE_FORMAT_SIMPLE_RU = "dd-MM-yyyy";
    /**
     * Example: 25/12
     */
    public final static String DATE_FORMAT_SHORT = "dd/MM";

    @Nullable
    public static Calendar getCalendar(String format, String date) {
        SimpleDateFormat sdf = new SimpleDateFormat(format, Wallet.LC_EN);
        try {
            sdf.parse(date);
        } catch (ParseException e) {
            Timber.w(e);
            return null;
        }

        return sdf.getCalendar();
    }

    public static DateTime fromYearMonth(YearMonth yearMonth) {
        return new DateTime().withYear(yearMonth.getYear()).withMonthOfYear(yearMonth.getMonthOfYear()).withDayOfMonth(1);
    }

    /**
     * Convert string date from sw format to ne
     *
     * @param date       String date value
     * @param fromFormat
     * @param toFormat
     * @return
     */
    @Nullable
    public static String convertFormat(String date, String fromFormat, String toFormat) {
        if (date == null) return null;
        Calendar cal = getCalendar(fromFormat, date);
        if (cal == null) return null;
        SimpleDateFormat format = new SimpleDateFormat(toFormat, Wallet.LC_EN);
        return format.format(cal.getTime());
    }

    @Nullable
    public static String format(DateTime input, String format) {
        if (input == null || format == null) {
            return null;
        }

        final DateTimeFormatter fmt = DateTimeFormat.forPattern(format);
        return fmt.print(input);
    }

    public static DateTime flatDay(DateTime d) {
        return d.withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfDay(0);
    }

    public static boolean compareFlatDay(DateTime d1, DateTime d2) {
        return flatDay(d1).equals(flatDay(d2));
    }

    public static String toSimpleISODate(DateTime dt) {
        return format(dt, DATE_FORMAT_SIMPLE);
    }


}
