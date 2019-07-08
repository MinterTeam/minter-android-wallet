package network.minter.bipwallet.tests;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

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
