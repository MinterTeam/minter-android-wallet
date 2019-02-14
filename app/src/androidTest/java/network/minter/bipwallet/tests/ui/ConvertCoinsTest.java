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

import android.support.annotation.IdRes;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.view.View;
import android.view.ViewGroup;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import network.minter.bipwallet.R;
import network.minter.bipwallet.advanced.repo.SecretStorage;
import network.minter.bipwallet.exchange.ui.ConvertCoinActivity;
import network.minter.bipwallet.internal.auth.AuthSession;
import network.minter.bipwallet.settings.repo.MinterBotRepository;
import network.minter.bipwallet.tests.internal.TestWallet;
import network.minter.blockchain.models.BCResult;
import network.minter.blockchain.models.Coin;
import network.minter.blockchain.models.ExchangeBuyValue;
import network.minter.blockchain.models.ExchangeSellValue;
import network.minter.core.MinterSDK;
import network.minter.core.bip39.MnemonicResult;
import network.minter.core.crypto.MinterAddress;
import network.minter.explorer.models.BCExplorerResult;
import network.minter.explorer.models.CoinItem;
import network.minter.explorer.models.ExpResult;
import network.minter.explorer.repo.ExplorerCoinsRepository;
import network.minter.profile.models.User;
import retrofit2.Response;
import timber.log.Timber;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static network.minter.bipwallet.internal.ReactiveAdapter.rxCallBc;
import static network.minter.bipwallet.internal.ReactiveAdapter.rxCallExp;
import static network.minter.bipwallet.internal.helpers.MathHelper.bdHuman;
import static network.minter.bipwallet.tests.internal.MyMatchers.withInputLayoutError;
import static network.minter.bipwallet.tests.internal.MyMatchers.withInputLayoutHint;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertTrue;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
@LargeTest
@RunWith(MockitoJUnitRunner.class)
public class ConvertCoinsTest extends BaseUiTest {

    @Rule
    public ActivityTestRule<ConvertCoinActivity> mActivityTestRule = new ActivityTestRule<>(ConvertCoinActivity.class, true, false);

    private ExplorerCoinsRepository mCoinsRepo;
    private Coin mExchangeCoin;
    private MinterAddress mAddress;

    public ConvertCoinsTest() {
        mCoinsRepo = TestWallet.app().explorerCoinsRepo();
        ExpResult<List<CoinItem>> coins = rxCallExp(mCoinsRepo.getAll())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(AndroidSchedulers.mainThread())
                .blockingFirst();

        if (coins.error != null) {
            throw new RuntimeException(coins.error.getMessage());
        }

        Optional<CoinItem> coin = Stream.of(coins.result)
                .filter(item -> !item.name.toLowerCase().equals(MinterSDK.DEFAULT_COIN.toLowerCase()))
                .sorted((o1, o2) -> {
                    // first - with biggest volume,
                    return o2.volume.compareTo(o1.volume);
                })
                .findFirst();

        if (!coin.isPresent()) {
            throw new RuntimeException("Unable to find coin other than default minter's coin: " + MinterSDK.DEFAULT_COIN);
        }

        BCResult<Coin> coinInfo = rxCallBc(TestWallet.app().coinRepoBlockChain().getCoinInfo(coin.get().symbol)).blockingFirst();

        if (!coinInfo.isOk()) {
            throw new RuntimeException(coinInfo.error.getMessage());
        }

        mExchangeCoin = coinInfo.result;
        Timber.d("Exchange coin: %s", mExchangeCoin.getSymbol());
    }

    private static Matcher<View> childOf(final int position, final Matcher<View> parentMatcher) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText(String.format("child %d of ViewPager", position));
            }

            @Override
            protected boolean matchesSafely(View view) {
                if (!(view.getParent() instanceof ViewGroup)) {
                    return parentMatcher.matches(view.getParent());
                }

                ViewGroup group = (ViewGroup) view.getParent();
                return parentMatcher.matches(view.getParent()) && group.getChildAt(position) != null && group.getChildAt(position).equals(view);
            }
        };
    }

    private static Matcher<View> inViewPager(int position, @IdRes int resId) {
        return isDescendantOfA(childOf(position, withId(resId)));
    }

    @Before
    @Override
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

        TestWallet.app().idlingManager().register();
    }

    @After
    @Override
    public void tearDown() {
        super.tearDown();
        TestWallet.app().idlingManager().unregister();
        mActivityTestRule.finishActivity();
    }

    @Test
    public void testSpendCoin() {
//        final int tabPos = 0;
//        // spend coins
//        mActivityTestRule.getActivity().setCurrentPage(tabPos);
//
//        // wait while balance be a 100
//        waitForBalance(100);
//
//        // wait while balance isn't update
//        waitForBalanceUpdate();
    }

    @Test
    public void testSpendInputs() throws Throwable {

        ExplorerCoinsRepository repo = TestWallet.app().explorerCoinsRepo();

        final int tabPos = 0;
        // spend coins
        mActivityTestRule.runOnUiThread(() -> {
            mActivityTestRule.getActivity().setCurrentPage(tabPos);
        });

        // wait while balance be a 100
        waitForBalance(100);

        // wait while balance isn't update
        waitForBalanceUpdate();

        final String balanceString = String.format("%s (%s)", MinterSDK.DEFAULT_COIN, bdHuman(new BigDecimal(100.d)));


        ViewInteraction actionBtn = onView(allOf(
                withId(R.id.action),
                withText("EXCHANGE"),
                inViewPager(tabPos, R.id.pager)
        ));
        ViewInteraction actionUseMax = onView(allOf(
                withId(R.id.action_maximum),
                inViewPager(tabPos, R.id.pager)
        ));
        ViewInteraction amountInput = onView(allOf(withId(R.id.input_amount), inViewPager(tabPos, R.id.pager)));
        ViewInteraction coinInput = onView(allOf(withId(R.id.input_incoming_coin), inViewPager(tabPos, R.id.pager)));
        ViewInteraction coinOutput = onView(allOf(withId(R.id.input_outgoing_coin), inViewPager(tabPos, R.id.pager)));
        ViewInteraction layoutCalculation = onView(allOf(
                withId(R.id.layout_calculation),
                inViewPager(tabPos, R.id.pager)
        ));
        ViewInteraction calculationSum = onView(allOf(withId(R.id.calculation), inViewPager(tabPos, R.id.pager)));

        coinOutput.check(matches(withText(balanceString)));

        actionUseMax.check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        // submit disabled
        actionBtn.check(matches(not(isEnabled())));

        amountInput.check(matches(withInputLayoutHint(R.string.label_amount)));
        amountInput.check(matches(withInputLayoutError(null)));
        actionBtn.check(matches(not(isEnabled())));

        amountInput.perform(replaceText("0"));
        amountInput.check(matches(withInputLayoutError("Amount must be greater than 0")));
        actionBtn.check(matches(not(isEnabled())));

        // empty string is valid zero
        amountInput.perform(replaceText(""));
        amountInput.check(matches(withInputLayoutError("Amount must be greater than 0")));
        actionBtn.check(matches(not(isEnabled())));

        final String amount = "1";
        // valid value
        amountInput.perform(replaceText(amount));
        amountInput.check(matches(withInputLayoutError(null)));
        actionBtn.check(matches(not(isEnabled())));

        // @TODO potentially, may break test, as coin can be created
        coinInput.perform(replaceText("DOSNTEXIST"));

        coinInput.check(matches(withInputLayoutError("Coin to buy not exists")));

        // we can send transaction even if coin does not exists, because it can be no a true
        actionBtn.check(matches(isEnabled()));
        layoutCalculation.check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));


        coinInput.perform(scrollTo(), replaceText(mExchangeCoin.getSymbol()));
        layoutCalculation.check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        onView(allOf(withId(R.id.calculation_title), inViewPager(tabPos, R.id.pager)))
                .check(matches(withText(R.string.label_you_will_get_approximately)));


        calculationSum.perform(scrollTo());


        Response<BCExplorerResult<ExchangeSellValue>> estimate1 = repo.getCoinExchangeCurrencyToSell(MinterSDK.DEFAULT_COIN, new BigDecimal(amount), mExchangeCoin.getSymbol()).execute();
        assertTrue(estimate1.body().isSuccess());
        // amount without commission
        String expectEstimate3 = bdHuman(estimate1.body().result.getAmount());
        calculationSum.check(matches(withText(String.format("%s %s", expectEstimate3, mExchangeCoin.getSymbol()))));


        // visible "use max" for spend tab
        actionUseMax.check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        actionUseMax.perform(click());
        amountInput.check(matches(withText("100")));

        Response<BCExplorerResult<ExchangeSellValue>> estimate2 = repo.getCoinExchangeCurrencyToSell(MinterSDK.DEFAULT_COIN, new BigDecimal("100"), mExchangeCoin.getSymbol()).execute();
        assertTrue(estimate2.body().isSuccess());
        // amount without commission
        String expectEstimate2 = bdHuman(estimate2.body().result.getAmount());
        calculationSum.check(matches(withText(String.format("%s %s", expectEstimate2, mExchangeCoin.getSymbol()))));
    }

    @Test
    public void testGetInputs() throws Throwable {

        ExplorerCoinsRepository repo = TestWallet.app().explorerCoinsRepo();

        final int tabPos = 1;
        // spend coins
        mActivityTestRule.runOnUiThread(() -> {
            mActivityTestRule.getActivity().setCurrentPage(tabPos);
        });


        // wait while balance be a 100
        waitForBalance(100);

        // wait while balance isn't update
        waitForBalanceUpdate();

        final String balanceString = String.format("%s (%s)", MinterSDK.DEFAULT_COIN, bdHuman(new BigDecimal("100")));


        ViewInteraction actionBtn = onView(allOf(
                withId(R.id.action),
                withText("EXCHANGE"),
                inViewPager(tabPos, R.id.pager)
        ));
        ViewInteraction actionUseMax = onView(allOf(
                withId(R.id.action_maximum),
                inViewPager(tabPos, R.id.pager)
        ));
        ViewInteraction amountInput = onView(allOf(withId(R.id.input_amount), inViewPager(tabPos, R.id.pager)));
        ViewInteraction coinInput = onView(allOf(withId(R.id.input_incoming_coin), inViewPager(tabPos, R.id.pager)));
        ViewInteraction coinOutput = onView(allOf(withId(R.id.input_outgoing_coin), inViewPager(tabPos, R.id.pager)));
        ViewInteraction layoutCalculation = onView(allOf(
                withId(R.id.layout_calculation),
                inViewPager(tabPos, R.id.pager)
        ));
        ViewInteraction calculationSum = onView(allOf(withId(R.id.calculation), inViewPager(tabPos, R.id.pager)));


        // invisible "use max" for spend tab
        actionUseMax.check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));


        coinOutput.check(matches(withText(balanceString)));

        // submit disabled
        actionBtn.check(matches(not(isEnabled())));

        amountInput.check(matches(withInputLayoutHint(R.string.label_amount)));
        amountInput.check(matches(withInputLayoutError(null)));
        actionBtn.check(matches(not(isEnabled())));

        amountInput.perform(replaceText("0"));
        amountInput.check(matches(withInputLayoutError("Amount must be greater than 0")));
        actionBtn.check(matches(not(isEnabled())));

        // empty string is valid zero
        amountInput.perform(replaceText(""));
        amountInput.check(matches(withInputLayoutError("Amount must be greater than 0")));
        actionBtn.check(matches(not(isEnabled())));

        final String amount = "1";
        // valid value
        amountInput.perform(replaceText(amount));
        amountInput.check(matches(withInputLayoutError(null)));
        actionBtn.check(matches(not(isEnabled())));

        // @TODO potentially, may break test, as coin can be created
        coinInput.perform(replaceText("DOSNTEXIST"));

        coinInput.check(matches(withInputLayoutError("Coin to buy not exists")));

        // we can send transaction even if coin does not exists, because it can be no a true
        actionBtn.check(matches(isEnabled()));
        layoutCalculation.check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));


        coinInput.perform(scrollTo(), replaceText(mExchangeCoin.getSymbol()));
        layoutCalculation.check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        onView(allOf(withId(R.id.calculation_title), inViewPager(tabPos, R.id.pager)))
                .check(matches(withText(R.string.label_you_will_pay_approximately)));


        calculationSum.perform(scrollTo());


        Response<BCExplorerResult<ExchangeBuyValue>> estimate1 = repo.getCoinExchangeCurrencyToBuy(MinterSDK.DEFAULT_COIN, new BigDecimal(amount), mExchangeCoin.getSymbol()).execute();
        assertTrue(estimate1.body().isSuccess());
        // amount without commission
        String expectEstimate1 = bdHuman(estimate1.body().result.getAmount());
        calculationSum.check(matches(withText(String.format("%s %s", expectEstimate1, MinterSDK.DEFAULT_COIN))));

    }


}
