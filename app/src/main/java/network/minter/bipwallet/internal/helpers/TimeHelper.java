package network.minter.bipwallet.internal.helpers;

import org.joda.time.DateTime;

import java.util.Calendar;

/**
 * Dogsy. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class TimeHelper {

	public final static long HOUR_SECONDS = 60 * 60;
	public final static long DAY_SECONDS = HOUR_SECONDS * 24;
	public final static long WEEK_SECONDS = DAY_SECONDS * 7;
	public final static long YEAR_SECONDS =
			DAY_SECONDS * Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_YEAR);

	public static long timestamp() {
		return new DateTime().getMillis() / 1000;
	}
}
