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

package network.minter.bipwallet.internal.helpers;

import android.content.res.Resources;

import org.joda.time.Duration;

import network.minter.bipwallet.R;
import network.minter.core.MinterSDK;

/**
 * Minter. 2018
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class Plurals {
    private static String[] seconds;
    private static String secondsShort;
    private static String[] minutes;
    private static String minutesShort;
    private static String[] hours;
    private static String hoursShort;
    private static String[] days;
    private static String daysShort;
    private static String[] bips;

    public static void init(Resources res) {
        seconds = new String[]{
                res.getString(R.string.plurals_seconds_1),
                res.getString(R.string.plurals_seconds_2_4),
                res.getString(R.string.plurals_seconds_others)
        };
        secondsShort = res.getString(R.string.plurals_seconds_short);
        minutes = new String[]{
                res.getString(R.string.plurals_minutes_1),
                res.getString(R.string.plurals_minutes_2_4),
                res.getString(R.string.plurals_minutes_others)
        };
        minutesShort = res.getString(R.string.plurals_minutes_short);
        hours = new String[]{
                res.getString(R.string.plurals_hours_1),
                res.getString(R.string.plurals_hours_2_4),
                res.getString(R.string.plurals_hours_others)
        };
        hoursShort = res.getString(R.string.plurals_hours_short);
        days = new String[]{
                res.getString(R.string.plurals_days_1),
                res.getString(R.string.plurals_days_2_4),
                res.getString(R.string.plurals_days_others)
        };
        daysShort = res.getString(R.string.plurals_days_short);
        bips = new String[]{"bip", "bip", "bip"};

    }


    public static String seconds(Long n) {
        return plurals(n, seconds);
    }

    public static String minutes(Long n) {
        return plurals(n, minutes);
    }

    public static String hours(Long n) {
        return plurals(n, hours);
    }

    public static String days(Long n) {
        return plurals(n, days);
    }

    public static String countdown(Long seconds) {
        return countdown(new Duration(seconds * 1000L /*accepts milliseconds only*/));
    }

    public static String countdown(Duration duration) {
        StringBuilder out = new StringBuilder();

        if (duration.getStandardHours() > 0) {
            out.append(String.format("%d %s", duration.getStandardHours(), hours(duration.getStandardHours())));
        }

        if (duration.getStandardMinutes() > 0) {
            out.append(String.format("%d %s", duration.getStandardMinutes(), minutes(duration.getStandardMinutes())));
        }

        if (duration.getStandardSeconds() > 0) {
            out.append(String.format("%d %s", duration.getStandardSeconds(), seconds(duration.getStandardSeconds())));
        } else {
            out.append("0 ").append(seconds(0L));
        }

        return out.toString();
    }

    @Deprecated
    public static String bips(Long n) {
        if (MinterSDK.DEFAULT_COIN.toUpperCase().equals(MinterSDK.DEFAULT_COIN)) {
            return MinterSDK.DEFAULT_COIN.toUpperCase();
        }
        return plurals(n, bips);
    }

    public static String usd(Long n) {
        return String.format("$%d", n);
    }

    public static String usd(String n) {
        return String.format("$%s", n);
    }

    public static String timeUnitShort(Long seconds) {
        if (seconds < 60) {
            return secondsShort;
        } else if (seconds < 3600) {
            return minutesShort;
        } else if (seconds < 86400) {
            return hoursShort;
        } else {
            return daysShort;
        }
    }

    public static String timeUnit(Long seconds) {
        if (seconds < 60) {
            return seconds(seconds);
        } else if (seconds < 3600) {
            return minutes(seconds / 60);
        } else if (seconds < 86400) {
            return hours(seconds / 60 / 60);
        } else {
            return days(seconds / 60 / 60 / 24);
        }
    }

    public static Long timeValue(Long seconds) {
        if (seconds < 60) {
            return seconds;
        } else if (seconds < 3600) {
            return seconds / 60;
        } else if (seconds < 86400) {
            return seconds / 60 / 60;
        } else {
            return seconds / 60 / 60 / 24;
        }
    }


    private static String plurals(Long n, String[] words) {
        if (n == null) {
            return words[2];
        }

        n = n % 100;
        if (n > 19) {
            n = n % 10;
        }

        if (n == 1) {
            return words[0];
        } else if (n == 2L || n == 3L || n == 4L) {
            return words[1];
        } else {
            return words[2];
        }
    }
}
