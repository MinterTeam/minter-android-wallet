package network.minter.bipwallet.tests;

import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 18-Apr-19
 */
public class ActivityUtils {
    public static void openFragment(AppCompatActivity activity, Fragment newFragment, @IdRes int containerId) {
        activity
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(containerId, newFragment)
                .commitAllowingStateLoss();
    }
}
