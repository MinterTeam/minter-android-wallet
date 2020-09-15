/*
 * Copyright (C) by MinterTeam. 2020
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
package network.minter.bipwallet.tests.ui

import network.minter.bipwallet.home.ui.HomeActivity
import network.minter.bipwallet.internal.auth.AuthSession
import network.minter.bipwallet.internal.storage.SecretStorage
import network.minter.bipwallet.tests.LazyActivityScenarioRule
import network.minter.bipwallet.tests.internal.TestWallet
import network.minter.core.crypto.MinterAddress
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
class SendCoinsTabTest : BaseUiTest() {
    @get:Rule
    var activityTestRule = LazyActivityScenarioRule(false, HomeActivity::class.java)

    private var mAddress: MinterAddress? = null

    @Before
    override fun setUp() {
        super.setUp()
        val ss: SecretStorage = TestWallet.app().secretStorage()
        ss.destroy()
        val mnemonicResult = generateMnemonic()
        mAddress = ss.add(mnemonicResult)
        TestWallet.app().session().login(
                AuthSession.AUTH_TOKEN_ADVANCED,
                AuthSession.AuthType.Advanced
        )

        TestWallet.app().accountStorageCache().update(true)


        activityTestRule.launch(null)
//        TestWallet.app().idlingManager().register()
    }

    @After
    override fun tearDown() {
        Timber.d("UnRegister idlings!")
//        TestWallet.app().idlingManager().unregister()
        super.tearDown()

    }

    @Test
    @Throws(InterruptedException::class)
    fun testSendCoinCommission() {
//        val gateMock = ApiMockInterceptor("gate", activityTestRule.activity)
//        gateMock.override("api/v1/min-gas", "/v1/min-gas/min_gas_price_1")
//        MinterExplorerApi.getInstance().getGateApiService().addHttpInterceptor(gateMock)
//        selectTab(1)
//        val sd: SecretData = SecretStorage.generateAddress()
//        val address: String = sd.getMinterAddress().toString()
//        val amountToSend = BigDecimal("1")
//
//        // recipient
//        val recipient = Espresso.onView(ViewMatchers.withId(R.id.input_recipient))
//        recipient.perform(ViewActions.replaceText(address))
//        recipient.check(ViewAssertions.matches(ViewMatchers.withText(address)))
//
//        // amount
//        val amount = Espresso.onView(ViewMatchers.withId(R.id.input_amount))
//        amount.perform(ViewActions.replaceText(amountToSend.toString()))
//
//        // submit
//        val submit = Espresso.onView(CoreMatchers.allOf(ViewMatchers.withId(R.id.action), ViewMatchers.withText(R.string.btn_send)))
//        submit.check(ViewAssertions.matches(ViewMatchers.isEnabled()))
//
//        // fee
//        val feeValue = Espresso.onView(ViewMatchers.withId(R.id.fee_value))
//
//
//        // fee = 0.01
//        feeValue.check(ViewAssertions.matches(ViewMatchers.withText(String.format("%s %s", bdHuman(OperationType.SendCoin.fee.multiply(BigDecimal("1"))), MinterSDK.DEFAULT_COIN))
//        ))
//        Thread.sleep(1000 * 3.toLong())
//
//
//        // fee = 0.02
//        gateMock.override("api/v1/min-gas", "/v1/min-gas/min_gas_price_2")
//        amount.perform(ViewActions.replaceText(amountToSend.toString()))
//        feeValue.check(ViewAssertions.matches(ViewMatchers.withText(String.format("%s %s", bdHuman(OperationType.SendCoin.fee.multiply(BigDecimal("2"))), MinterSDK.DEFAULT_COIN))
//        ))
//        Thread.sleep(1000 * 3.toLong())
//
//        // fee = 0.04
//        gateMock.override("api/v1/min-gas", "/v1/min-gas/min_gas_price_4")
//        amount.perform(ViewActions.replaceText(amountToSend.toString()))
//        feeValue.check(ViewAssertions.matches(ViewMatchers.withText(String.format("%s %s", bdHuman(OperationType.SendCoin.fee.multiply(BigDecimal("4"))), MinterSDK.DEFAULT_COIN))
//        ))
//        Thread.sleep(1000 * 3.toLong())
//        MinterExplorerApi.getInstance().getGateApiService().removeHttpInterceptor(gateMock)
    }

    @Test
    fun testSendCoinsSuccessfully() {
//        selectTab(1)
//        waitForBalance(100.0)
//        waitForBalanceUpdate()
//        val sd: SecretData = SecretStorage.generateAddress()
//        val address: String = sd.getMinterAddress().toString()
//
//        // recipient
//        val recipient = Espresso.onView(ViewMatchers.withId(R.id.input_recipient))
//        recipient.perform(ViewActions.replaceText(address))
//        recipient.check(ViewAssertions.matches(ViewMatchers.withText(address)))
//
//        // amount
//        val amount = Espresso.onView(ViewMatchers.withId(R.id.input_amount))
//        amount.perform(ViewActions.replaceText("1"))
//
//        // submit
//        val submit = Espresso.onView(CoreMatchers.allOf(ViewMatchers.withId(R.id.action), ViewMatchers.withText(R.string.btn_send)))
//        submit.check(ViewAssertions.matches(ViewMatchers.isEnabled()))
//        submit.perform(ViewActions.click())
//        val confirmTitle = Espresso.onView(CoreMatchers.allOf(ViewMatchers.isDescendantOfA(ViewMatchers.withId(android.R.id.content)), ViewMatchers.withId(R.id.title)))
//        confirmTitle.check(ViewAssertions.matches(ViewMatchers.withText("You're sending")))
//        Espresso.onView(withId(R.id.dialog_amount)).check(ViewAssertions.matches(ViewMatchers.withText(String.format("%s %s", bdHuman(1.0), MinterSDK.DEFAULT_COIN))))
//        Espresso.onView(withId(R.id.tx_recipient_name)).check(ViewAssertions.matches(ViewMatchers.withText(address)))
//        val confirm = Espresso.onView(CoreMatchers.allOf(ViewMatchers.withId(R.id.action_confirm), ViewMatchers.withText(R.string.btn_send)))
//        confirm.perform(ViewActions.click())
//        Espresso.onView(ViewMatchers.withId(R.id.tx_description)).check(ViewAssertions.matches(ViewMatchers.withText(R.string.tx_send_success_dialog_description)))
//        Espresso.onView(withId(R.id.tx_recipient_name)).check(ViewAssertions.matches(ViewMatchers.withText(address)))
//        Espresso.onView(ViewMatchers.withId(R.id.action_view_tx))
//                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
//                .check(ViewAssertions.matches(ViewMatchers.withText("VIEW TRANSACTION")))
//
//        // close dialog to prevent leaking
//        Espresso.onView(CoreMatchers.allOf(ViewMatchers.withText("Close"), ViewMatchers.withId(R.id.action_close))).perform(ViewActions.click())
//
//        // 100 - 1 - 0.01 fee  - 98.99
//        waitForBalance(BigDecimal(99).subtract(OperationType.SendCoin.fee))
    }

    @Test
    fun testInputs() {
        selectTab(1)
//        waitForBalance(100.0)
//        val balanceString = String.format("%s (%s)", MinterSDK.DEFAULT_COIN, bdHuman(BigDecimal(100.0)))
//        val submit = Espresso.onView(CoreMatchers.allOf(ViewMatchers.withId(R.id.action), ViewMatchers.withText(R.string.btn_send)))
//        submit.check(ViewAssertions.matches(Matchers.not(ViewMatchers.isEnabled())))
//        val accountInput = Espresso.onView(ViewMatchers.withId(R.id.input_coin))
//        accountInput.check(ViewAssertions.matches(ViewMatchers.withSubstring(MinterSDK.DEFAULT_COIN)))
//        accountInput.check(ViewAssertions.matches(ViewMatchers.withText(balanceString)))
//        accountInput.perform(ViewActions.click())
//        val accountDialogTitle = Espresso.onView(ViewMatchers.withText(R.string.title_select_account))
//        accountDialogTitle.check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
//        val accountItem = Espresso.onView(CoreMatchers.allOf(ViewMatchers.withId(R.id.item_title), ViewMatchers.withText(balanceString)))
//        accountItem.perform(ViewActions.click())
//
//        // check submit still not enabled
//        submit.check(ViewAssertions.matches(Matchers.not(ViewMatchers.isEnabled())))
//        val recipient = Espresso.onView(ViewMatchers.withId(R.id.input_recipient))
//        recipient.check(ViewAssertions.matches(Matchers.not(ViewMatchers.withText("aaa"))))
//        recipient.check(ViewAssertions.matches(MyMatchers.withInputLayoutHint(R.string.tx_send_recipient_hint)))
//        // set something strange
//        run {
//            recipient.perform(ViewActions.replaceText("aaa"))
//            recipient.check(ViewAssertions.matches(MyMatchers.withInputLayoutError("Incorrect recipient format")))
//            // check submit still not enabled
//            submit.check(ViewAssertions.matches(Matchers.not(ViewMatchers.isEnabled())))
//        }
//
//        // set something illegal
//        run {
//            recipient.perform(ViewActions.replaceText("whywhywhye"))
//            recipient.check(ViewAssertions.matches(MyMatchers.withInputLayoutError("Incorrect recipient format")))
//            // check submit still not enabled
//            submit.check(ViewAssertions.matches(Matchers.not(ViewMatchers.isEnabled())))
//        }
//
//        // set too short username, minimum 5 chars
//        run {
//            recipient.perform(ViewActions.replaceText("@aabb"))
//            recipient.check(ViewAssertions.matches(MyMatchers.withInputLayoutError("Incorrect recipient format")))
//            // check submit still not enabled
//            submit.check(ViewAssertions.matches(Matchers.not(ViewMatchers.isEnabled())))
//        }
//
//        // set too long username, max 16 chars
//        run {
//            recipient.perform(ViewActions.replaceText("@abcdefghijklmnopqrtstuwxyz"))
//            recipient.check(ViewAssertions.matches(MyMatchers.withInputLayoutError("Incorrect recipient format")))
//            // check submit still not enabled
//            submit.check(ViewAssertions.matches(Matchers.not(ViewMatchers.isEnabled())))
//        }
//
//        // set invalid email #1
//        run {
//            recipient.perform(ViewActions.replaceText("test@fly"))
//            recipient.check(ViewAssertions.matches(MyMatchers.withInputLayoutError("Incorrect recipient format")))
//            // check submit still not enabled
//            submit.check(ViewAssertions.matches(Matchers.not(ViewMatchers.isEnabled())))
//        }
//
//        // set invalid email #2
//        run {
//            recipient.perform(ViewActions.replaceText("test.fly.com"))
//            recipient.check(ViewAssertions.matches(MyMatchers.withInputLayoutError("Incorrect recipient format")))
//            // check submit still not enabled
//            submit.check(ViewAssertions.matches(Matchers.not(ViewMatchers.isEnabled())))
//        }
//
//        // set invalid email #2 (but this will parsed as invalid username)
//        run {
//            recipient.perform(ViewActions.replaceText("@gmail.com"))
//            recipient.check(ViewAssertions.matches(MyMatchers.withInputLayoutError("Incorrect recipient format")))
//            // check submit still not enabled
//            submit.check(ViewAssertions.matches(Matchers.not(ViewMatchers.isEnabled())))
//        }
//        // set valid username, minimum 5 chars
//        run {
//            recipient.perform(ViewActions.replaceText("@aabbc"))
//            recipient.check(ViewAssertions.matches(MyMatchers.withInputLayoutError(null)))
//            // check submit still not enabled
//            submit.check(ViewAssertions.matches(ViewMatchers.isEnabled()))
//        }
//
//        // set valid username, minimum 5 chars
//        run {
//            recipient.perform(ViewActions.replaceText("@12345"))
//            recipient.check(ViewAssertions.matches(MyMatchers.withInputLayoutError(null)))
//            // check submit still not enabled
//            submit.check(ViewAssertions.matches(ViewMatchers.isEnabled()))
//        }
//        val amount = Espresso.onView(ViewMatchers.withId(R.id.input_amount))
//        amount.check(ViewAssertions.matches(ViewMatchers.withText("")))
//        amount.check(ViewAssertions.matches(MyMatchers.withInputLayoutHint(R.string.label_amount)))
//
//        // set 0
//        run {
//            amount.perform(ViewActions.replaceText("0"))
//            amount.check(ViewAssertions.matches(MyMatchers.withInputLayoutError("Amount must be greater than 0")))
//            // check submit still not enabled
//            submit.check(ViewAssertions.matches(Matchers.not(ViewMatchers.isEnabled())))
//        }
//
//        // set .
//        run {
//            amount.perform(ViewActions.replaceText("."))
//            amount.check(ViewAssertions.matches(MyMatchers.withInputLayoutError("Amount must be greater than 0")))
//            // check submit still not enabled
//            submit.check(ViewAssertions.matches(Matchers.not(ViewMatchers.isEnabled())))
//        }
//
//        // set .1
//        run {
//            amount.perform(ViewActions.replaceText(".1"))
//            amount.check(ViewAssertions.matches(MyMatchers.withInputLayoutError(null)))
//            // check submit is enabled
//            submit.check(ViewAssertions.matches(ViewMatchers.isEnabled()))
//        }
//
//        // maximum is 10^-18
//        run {
//            amount.perform(ViewActions.replaceText("0.102030405060708090"))
//            amount.check(ViewAssertions.matches(MyMatchers.withInputLayoutError(null)))
//        }
//        val useMaxButton = Espresso.onView(withId(R.id.action_maximum))
//        useMaxButton.check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
//        useMaxButton.perform(ViewActions.click())
//        amount.check(ViewAssertions.matches(ViewMatchers.withText("100")))
//        val feeLabel = Espresso.onView(ViewMatchers.withId(R.id.fee_label))
//        feeLabel.check(ViewAssertions.matches(ViewMatchers.withText(R.string.tx_send_fee_hint)))
//        val feeText = Espresso.onView(ViewMatchers.withId(R.id.fee_value))
//        feeText.check(ViewAssertions.matches(ViewMatchers.withText(String.format("%s %s", bdHuman(OperationType.SendCoin.fee), MinterSDK.DEFAULT_COIN))))
    }
}