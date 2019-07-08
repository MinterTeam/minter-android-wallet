package network.minter.bipwallet.tests.rules;

import android.view.WindowManager;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 18-Apr-19
 */
public class UnlockScreenRule<A extends AppCompatActivity> implements TestRule {

    ActivityTestRule<A> activityRule;

    public UnlockScreenRule(ActivityTestRule<A> activityRule) {
        this.activityRule = activityRule;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                activityRule.runOnUiThread(() -> activityRule
                        .getActivity()
                        .getWindow()
                        .addFlags(
                                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                                        | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                        ));
                base.evaluate();
            }
        };
    }
}
