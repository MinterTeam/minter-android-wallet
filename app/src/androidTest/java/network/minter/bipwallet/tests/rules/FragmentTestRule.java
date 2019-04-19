package network.minter.bipwallet.tests.rules;

import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.rule.ActivityTestRule;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 18-Apr-19
 */
public class FragmentTestRule<A extends AppCompatActivity, F extends Fragment> implements TestRule {

    private ActivityTestRule<A> activityRule;
    private F fragment;
    private RuleChain ruleChain;

    private RuleChain init(Class<A> activityClass, F fragment) {
        this.fragment = fragment;
        this.activityRule = new ActivityTestRule<A>(activityClass, true, true);
        return RuleChain.outerRule(activityRule).around(new UnlockScreenRule<>(activityRule));
    }

    public FragmentTestRule(Class<A> activityClass, F fragment) {
        ruleChain = init(activityClass, fragment).around(new OpenFragmentRule<>(activityRule, fragment));
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return ruleChain.apply(base, description);
    }
}
