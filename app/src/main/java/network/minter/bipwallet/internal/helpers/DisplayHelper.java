package network.minter.bipwallet.internal.helpers;

import android.content.Context;
import android.support.annotation.DimenRes;
import android.support.annotation.FloatRange;
import android.support.annotation.Px;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.common.annotations.Dp;
import network.minter.bipwallet.internal.common.annotations.Sp;
import network.minter.bipwallet.internal.helpers.data.Vec2;

/**
 * Dogsy. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class DisplayHelper {
	private Context mContext;

	public DisplayHelper(final Context context) {
		mContext = context;
	}

	public int getColumns(@Dp float columnWidthDp) {
		DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
		float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
		return (int) (dpWidth / columnWidthDp);
	}

	@Px
	public int getWidth() {
		DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
		return displayMetrics.widthPixels;
	}

	@Px
	public int getHeight() {
		DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
		return displayMetrics.heightPixels;
	}

	@Px
	public int dpToPx(@Dp int integer) {
		float pixels = getMetrics().density * integer;
		return (int) (pixels + 0.5f);
	}

	public boolean isTablet() {
		return false; //@TODO return mContext.getResources().getBoolean(R.bool.isTablet);
	}

	/**
	 * @param percent From 0.0 to infinite, where 100.0f is 100% of screen width
	 * @return
	 */
	@Px
	public int widthPercent(@FloatRange(from = 0.0f) float percent) {
		return (int) (getWidth() / 100 * percent);
	}

	public Vec2 getWidthAndHeightWithRatio(@DimenRes int fromWidthResId, @DimenRes int ratioWidthId, @DimenRes int ratioHeightId) {
		final int width = px(fromWidthResId);
		final int height = (int)
				(
						width /
								(getDimen(ratioWidthId) / getDimen(ratioHeightId))
				);

		return new Vec2(width, height);
	}

	/**
	 * Как работает:
	 * К примеру у нас есть ширина: 100% экрана, к примеру 1000px
	 * нам нужна пропорциональная высота относительно известного кол-ва пикселей,
	 * Требуемый формат 1000x500
	 * Далее все просто.
	 * Берем 100% ширины делим на пропорцию формата (ширина/высота) и получаем нужную высоту=500px
	 * 1000 / (1000/500=2.0) = 500
	 * <p>
	 * если экран в ширину 800 пикселей, то получим следующую формулу:
	 * 800 / (1000/500=2.0) = 400
	 *
	 * @param widthPercent  Ширина от которой отталкиваемся
	 * @param ratioWidthId  id ширины
	 * @param ratioHeightId id высоты
	 * @return
	 * @see #getWidthAndHeightWithRatio(int, int, int)
	 */
	public Vec2 getWidthAndHeightWithRatio(
			@FloatRange(from = 0.0f) float widthPercent,
			@DimenRes int ratioWidthId,
			@DimenRes int ratioHeightId) {
		final int width = widthPercent(widthPercent);
		final int height = (int)
				(
						width /
								(getDimen(ratioWidthId) / getDimen(ratioHeightId))
				);

		return new Vec2(width, height);
	}

	public Vec2 getWidthAndHeightWithRatio(
			@FloatRange(from = 0.0f) float widthPercent,
			float ratioWidth,
			float ratioHeight) {
		final int width = widthPercent(widthPercent);
		final int height = (int)
				(
						width /
								(ratioWidth / ratioHeight)
				);

		return new Vec2(width, height);
	}

	@Px
	public int heightPercent(@FloatRange(from = 0.0f) float percent) {
		return (int) (getHeight() / 100 * percent);
	}

	@Px
	public int dpToPx(@Dp float dps) {
		float pixels = getMetrics().density * dps;
		return (int) (pixels + 0.5f);
	}

	@Dp
	public int pxToDp(@Px int px) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getMetrics());
	}

	@Px
	public int px(@DimenRes int res) {
		return dpToPx(mContext.getResources().getDimension(res));
	}

	public DisplayMetrics getMetrics() {
		return mContext.getResources().getDisplayMetrics();
	}

	@Px
	public int spToPx(@Sp int sp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getMetrics());
	}

	public int getStatusBarHeight() {
		int result = 0;
		int resourceId = mContext.getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = mContext.getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}

	public float getDimen(@DimenRes int resId) {
		return mContext.getResources().getDimension(resId);
	}


}
