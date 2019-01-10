/*
 * Copyright (C) by MinterTeam. 2019
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

package network.minter.bipwallet.tests.ui;

import android.support.test.espresso.IdlingRegistry;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.intent.Intents;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;

import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.models.SecretData;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.home.ui.HomeActivity;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.internal.system.testing.CallbackIdlingResource;
import network.minter.bipwallet.sending.ui.SendTabFragment;
import network.minter.bipwallet.settings.repo.MinterBotRepository;
import network.minter.bipwallet.tests.internal.TestWallet;
import network.minter.blockchain.models.operational.OperationType;
import network.minter.core.MinterSDK;
import network.minter.core.bip39.MnemonicResult;
import network.minter.core.crypto.MinterAddress;
import network.minter.profile.models.User;
import timber.log.Timber;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withSubstring;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdHuman;
import static network.minter.bipwallet.tests.internal.MyMatchers.withInputLayoutError;
import static network.minter.bipwallet.tests.internal.MyMatchers.withInputLayoutHint;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.not;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
@LargeTest
@RunWith(MockitoJUnitRunner.class)
public class SendCoinsTabTest extends BaseUiTest {

    @Rule
    public ActivityTestRule<HomeActivity> mActivityTestRule = new ActivityTestRule<>(HomeActivity.class, true, false);
    private MinterAddress mAddress;

    @Before
    public void setUp() {
        super.setUp();
        final SecretStorage ss = TestWallet.app().secretStorage();
        ss.destroy();

        MnemonicResult mnemonicResult = generateMnemonic();
        mAddress = ss.add(mnemonicResult);

        TestWallet.app().session().login(
                AuthSession.AUTH_TOKEN_ADVANCED,
                new User(AuthSession.AUTH_TOKEN_ADVANCED),
                AuthSession.AuthType.Advanced
        );

        MinterBotRepository botRepo = new MinterBotRepository();
        MinterBotRepository.MinterBotResult botResult = botRepo.requestFreeCoins(mAddress).blockingFirst();
        if (!botResult.isOk()) {
            throw new RuntimeException("Unable to get free coins, which is required for testing!");
        }
        TestWallet.app().accountStorageCache().update(true);
        mActivityTestRule.launchActivity(null);

        Timber.d("Secret storage by address %s: %s", mAddress, TestWallet.app().secretStorage().getSecret(mAddress) != null ? "true" : "false");
        Intents.init();
    }

    @After
    public void tearDown() {
        super.tearDown();
        Intents.release();
        mActivityTestRule.finishActivity();
    }

    @Test
    public void testSendCoinsInsufficient() {
        selectTab(1);

        // wait while balance be a 100
        waitForBalance(100);
        // wait while balance isn't update
        waitForBalanceUpdate();


        BigDecimal amountToSend = new BigDecimal(10000);

        // register idlings
        SendTabFragment fragment = ((SendTabFragment) mActivityTestRule.getActivity().getActiveTabs().get(1));
        CallbackIdlingResource confirmIdling = new CallbackIdlingResource();
        CallbackIdlingResource completeIdling = new CallbackIdlingResource();

        IdlingRegistry.getInstance().register(confirmIdling);
        IdlingRegistry.getInstance().register(completeIdling);

        fragment.registerIdlings(confirmIdling, completeIdling);

        SecretData sd = SecretStorage.generateAddress();
        final String address = sd.getMinterAddress().toString();

        // recipient
        ViewInteraction recipient = onView(withId(R.id.input_recipient));
        recipient.perform(replaceText(address));
        recipient.check(matches(withText(address)));

        // amount
        ViewInteraction amount = onView(withId(R.id.input_amount));
        amount.perform(replaceText(amountToSend.toString()));

        // submit
        ViewInteraction submit = onView(allOf(withId(R.id.action), withText(R.string.btn_send)));
        submit.check(matches((isEnabled())));

        submit.perform(click());


        ViewInteraction confirmTitle = onView(allOf(isDescendantOfA(withId(android.R.id.content)), withId(R.id.title)));
        confirmTitle.check(matches(withText("You're sending")));

        onView(withId(R.id.dialog_amount)).check(matches(withText(String.format("%s %s", bdHuman(amountToSend), MinterSDK.DEFAULT_COIN))));
        onView(withId(R.id.tx_recipient_name)).check(matches(withText(address)));


        ViewInteraction confirm = onView(allOf(withId(R.id.action_confirm), withText(R.string.btn_send)));
        confirm.perform(click());

        onView(allOf(isDescendantOfA(withId(android.R.id.content)), withId(R.id.title)))
                .check(matches(
                        withText("Unable to send transaction"))
                );

        onView(allOf(isDescendantOfA(withId(android.R.id.content)), withId(R.id.dialog_text)))
                .check(matches(
                        allOf(
                                withText(containsString("Insufficient")),
                                withText(containsString(mAddress.toString()))
                        )
                ));

        // close dialog to prevent leaking
        onView(allOf(withText("Close"), withId(R.id.action_confirm))).perform(click());


        IdlingRegistry.getInstance().unregister(confirmIdling);
        IdlingRegistry.getInstance().unregister(completeIdling);

        // as before
        waitForBalance(100);
    }

    @Test
    public void testSendCoinsSuccessfully() {
        selectTab(1);

        waitForBalance(100d);
        waitForBalanceUpdate();

        // register idlings
        SendTabFragment fragment = ((SendTabFragment) mActivityTestRule.getActivity().getActiveTabs().get(1));
        CallbackIdlingResource confirmIdling = new CallbackIdlingResource();
        CallbackIdlingResource completeIdling = new CallbackIdlingResource();

        IdlingRegistry.getInstance().register(confirmIdling);
        IdlingRegistry.getInstance().register(completeIdling);

        fragment.registerIdlings(confirmIdling, completeIdling);

        SecretData sd = SecretStorage.generateAddress();
        final String address = sd.getMinterAddress().toString();

        // recipient
        ViewInteraction recipient = onView(withId(R.id.input_recipient));
        recipient.perform(replaceText(address));
        recipient.check(matches(withText(address)));

        // amount
        ViewInteraction amount = onView(withId(R.id.input_amount));
        amount.perform(replaceText("1"));

        // submit
        ViewInteraction submit = onView(allOf(withId(R.id.action), withText(R.string.btn_send)));
        submit.check(matches((isEnabled())));

        submit.perform(click());


        ViewInteraction confirmTitle = onView(allOf(isDescendantOfA(withId(android.R.id.content)), withId(R.id.title)));
        confirmTitle.check(matches(withText("You're sending")));

        onView(withId(R.id.dialog_amount)).check(matches(withText(String.format("%s %s", bdHuman(1d), MinterSDK.DEFAULT_COIN))));
        onView(withId(R.id.tx_recipient_name)).check(matches(withText(address)));


        ViewInteraction confirm = onView(allOf(withId(R.id.action_confirm), withText(R.string.btn_send)));
        confirm.perform(click());

        onView(withId(R.id.tx_description)).check(matches(withText(R.string.tx_send_success_dialog_description)));
        onView(withId(R.id.tx_recipient_name)).check(matches(withText(address)));
        onView(withId(R.id.action_view_tx))
                .check(matches(isDisplayed()))
                .check(matches(withText("VIEW TRANSACTION")));

        // close dialog to prevent leaking
        onView(allOf(withText("Close"), withId(R.id.action_close))).perform(click());

        IdlingRegistry.getInstance().unregister(confirmIdling);
        IdlingRegistry.getInstance().unregister(completeIdling);

        // 100 - 1 - 0.01 fee  - 98.99
        waitForBalance(new BigDecimal(99).subtract(OperationType.SendCoin.getFee()));

    }

    @Test
    public void testAccountSelector() {
        selectTab(1);

        waitForBalance(100d);
        final String balanceString = String.format("%s (%s)", MinterSDK.DEFAULT_COIN, bdHuman(new BigDecimal(100.d)));

        ViewInteraction submit = onView(allOf(withId(R.id.action), withText(R.string.btn_send)));
        submit.check(matches(not(isEnabled())));

        ViewInteraction accountInput = onView(withId(R.id.input_coin));
        accountInput.check(matches(withSubstring(MinterSDK.DEFAULT_COIN)));
        accountInput.check(matches(withText(balanceString)));

        accountInput.perform(click());
        ViewInteraction accountDialogTitle = onView(withText(R.string.title_select_account));
        accountDialogTitle.check(matches(isDisplayed()));
        ViewInteraction accountItem = onView(allOf(withId(R.id.item_title), withText(balanceString)));
        accountItem.perform(click());

        // check submit still not enabled
        submit.check(matches(not(isEnabled())));


        ViewInteraction recipient = onView(withId(R.id.input_recipient));

        recipient.check(matches(not(withText("aaa"))));
        recipient.check(matches(withInputLayoutHint(R.string.tx_send_recipient_hint)));
        // set something strange
        {
            recipient.perform(replaceText("aaa"));
            recipient.check(matches(withInputLayoutError("Incorrect recipient format")));
            // check submit still not enabled
            submit.check(matches(not(isEnabled())));
        }

        // set something illegal
        {
            recipient.perform(replaceText("whywhywhye"));
            recipient.check(matches(withInputLayoutError("Incorrect recipient format")));
            // check submit still not enabled
            submit.check(matches(not(isEnabled())));
        }

        // set too short username, minimum 5 chars
        {
            recipient.perform(replaceText("@aabb"));
            recipient.check(matches(withInputLayoutError("Incorrect recipient format")));
            // check submit still not enabled
            submit.check(matches(not(isEnabled())));
        }

        // set too long username, max 16 chars
        {
            recipient.perform(replaceText("@abcdefghijklmnopqrtstuwxyz"));
            recipient.check(matches(withInputLayoutError("Incorrect recipient format")));
            // check submit still not enabled
            submit.check(matches(not(isEnabled())));
        }

        // set invalid email #1
        {
            recipient.perform(replaceText("test@fly"));
            recipient.check(matches(withInputLayoutError("Incorrect recipient format")));
            // check submit still not enabled
            submit.check(matches(not(isEnabled())));
        }

        // set invalid email #2
        {
            recipient.perform(replaceText("test.fly.com"));
            recipient.check(matches(withInputLayoutError("Incorrect recipient format")));
            // check submit still not enabled
            submit.check(matches(not(isEnabled())));
        }

        // set invalid email #2 (but this will parsed as invalid username)
        {
            recipient.perform(replaceText("@gmail.com"));
            recipient.check(matches(withInputLayoutError("Incorrect recipient format")));
            // check submit still not enabled
            submit.check(matches(not(isEnabled())));
        }
        // set valid username, minimum 5 chars
        {
            recipient.perform(replaceText("@aabbc"));
            recipient.check(matches(withInputLayoutError(null)));
            // check submit still not enabled
            submit.check(matches((isEnabled())));
        }

        // set valid username, minimum 5 chars
        {
            recipient.perform(replaceText("@12345"));
            recipient.check(matches(withInputLayoutError(null)));
            // check submit still not enabled
            submit.check(matches((isEnabled())));
        }

        ViewInteraction amount = onView(withId(R.id.input_amount));
        amount.check(matches(withText("")));
        amount.check(matches(withInputLayoutHint(R.string.label_amount)));

        // set 0
        {
            amount.perform(replaceText("0"));
            amount.check(matches(withInputLayoutError("Amount must be greater than 0")));
            // check submit still not enabled
            submit.check(matches(not(isEnabled())));
        }

        // set .
        {
            amount.perform(replaceText("."));
            amount.check(matches(withInputLayoutError("Amount must be greater than 0")));
            // check submit still not enabled
            submit.check(matches(not(isEnabled())));
        }

        // set .1
        {
            amount.perform(replaceText(".1"));
            amount.check(matches(withInputLayoutError(null)));
            // check submit is enabled
            submit.check(matches((isEnabled())));
        }

        // maximum is 10^-18
        {
            amount.perform(replaceText("0.102030405060708090"));
            amount.check(matches(withInputLayoutError(null)));
            // check submit is enabled
        }

        ViewInteraction useMaxButton = onView(withId(R.id.action_maximum));
        useMaxButton.check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        useMaxButton.perform(click());

        amount.check(matches(withText("100")));

        ViewInteraction feeLabel = onView(withId(R.id.fee_label));
        feeLabel.check(matches(withText(R.string.tx_send_fee_hint)));
        ViewInteraction feeText = onView(withId(R.id.fee_value));
        feeText.check(matches(withText(
                String.format("%s %s", bdHuman(OperationType.SendCoin.getFee()), MinterSDK.DEFAULT_COIN)
        )));

    }
}
