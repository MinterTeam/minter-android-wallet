package network.minter.bipwallet.tests.rules;

import org.awaitility.Awaitility;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import network.minter.bipwallet.R;
import network.minter.bipwallet.tests.ActivityUtils;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 18-Apr-19
 */
public class OpenFragmentRule<A extends AppCompatActivity> implements TestRule {

    private final ActivityTestRule<A> activityRule;
    private final Fragment fragment;
    private final int timeoutSec = 5;

    OpenFragmentRule(ActivityTestRule<A> activityRule, Fragment fragment) {
        this.activityRule = activityRule;
        this.fragment = fragment;
    }

    @Override
    public Statement apply(Statement statement, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                ActivityUtils.openFragment(activityRule.getActivity(), fragment, R.id.container_auth);
                Awaitility.await().atMost(timeoutSec, SECONDS).until(fragment::isResumed);
                statement.evaluate();
            }
        };
    }
}
