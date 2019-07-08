package network.minter.bipwallet.tests.rules;

import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import androidx.appcompat.app.AppCompatActivity;
import androidx.test.espresso.intent.rule.IntentsTestRule;

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 18-Apr-19
 */
public class ActivityTestRule<A extends AppCompatActivity> implements TestRule {

    private final RuleChain ruleChain;

    public ActivityTestRule(Class<A> activityClass) {
        androidx.test.rule.ActivityTestRule<A> activityRule = new IntentsTestRule<>(activityClass, true, true);
        ruleChain = RuleChain.outerRule(activityRule).around(new UnlockScreenRule<>(activityRule));
    }

    @Override
    public Statement apply(Statement statement, Description description) {
        return ruleChain.apply(statement, description);
    }
}
