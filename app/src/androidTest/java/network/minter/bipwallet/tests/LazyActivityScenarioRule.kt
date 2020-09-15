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

package network.minter.bipwallet.tests


import android.app.Activity
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import org.junit.rules.ExternalResource

class LazyActivityScenarioRule<A : Activity> : ExternalResource {

    constructor(launchActivity: Boolean, startActivityIntentSupplier: () -> Intent) {
        this.launchActivity = launchActivity
        scenarioSupplier = { ActivityScenario.launch<A>(startActivityIntentSupplier()) }
    }

    constructor(launchActivity: Boolean, startActivityIntent: Intent) {
        this.launchActivity = launchActivity
        scenarioSupplier = { ActivityScenario.launch<A>(startActivityIntent) }
    }

    constructor(launchActivity: Boolean, startActivityClass: Class<A>) {
        this.launchActivity = launchActivity
        scenarioSupplier = { ActivityScenario.launch<A>(startActivityClass) }
    }

    private var launchActivity: Boolean

    private var scenarioSupplier: () -> ActivityScenario<A>

    private var scenario: ActivityScenario<A>? = null

    private var scenarioLaunched: Boolean = false

    override fun before() {
        if (launchActivity) {
            launch()
        }
    }

    override fun after() {
        scenario?.close()
    }

    fun launch(newIntent: Intent? = null) {
        if (scenarioLaunched) throw IllegalStateException("Scenario has already been launched!")

        newIntent?.let { scenarioSupplier = { ActivityScenario.launch<A>(it) } }

        scenario = scenarioSupplier()
        scenarioLaunched = true
    }

    fun getScenario(): ActivityScenario<A> = checkNotNull(scenario)
}

inline fun <reified A : Activity> lazyActivityScenarioRule(launchActivity: Boolean = true, noinline intentSupplier: () -> Intent): LazyActivityScenarioRule<A> =
        LazyActivityScenarioRule(launchActivity, intentSupplier)

inline fun <reified A : Activity> lazyActivityScenarioRule(launchActivity: Boolean = true, intent: Intent? = null): LazyActivityScenarioRule<A> = if (intent == null) {
    LazyActivityScenarioRule(launchActivity, A::class.java)
} else {
    LazyActivityScenarioRule(launchActivity, intent)
}