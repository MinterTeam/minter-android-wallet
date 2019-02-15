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

package network.minter.bipwallet.internal.system.testing;

import android.support.test.espresso.IdlingRegistry;

import com.annimon.stream.Stream;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

import static network.minter.bipwallet.internal.common.Preconditions.checkNotNull;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public class IdlingManager {
    private boolean mEnabled;
    private Map<String, CallbackIdlingResource> mContent = new HashMap<>();

    public IdlingManager(boolean enabled) {
        mEnabled = enabled;
    }

    public IdlingManager add(String... name) {
        for (String n : name) {
            add(n);
        }
        return this;
    }

    public IdlingManager add(String name, CallbackIdlingResource resource) {
        if (!mEnabled) {
            return this;
        }

        if (mContent.containsKey(resource.getName())) {
            Timber.w("Idling resource with name %s already exists. Skip adding...", resource.getName());
            return this;

        }

        mContent.put(name, resource);
        return this;
    }

    public IdlingManager add(String name) {
        return add(name, new CallbackIdlingResource(name));
    }

    public IdlingManager add(CallbackIdlingResource resource) {
        return add(resource.getName(), resource);
    }

    public void setNeedsWait(String name, boolean active) {
        if (!mContent.containsKey(name)) {
            Timber.e("Idling resource with name %s does not exists", name);
            return;
        }

        if (active) {
            Timber.d("Waiting for %s...", name);
        } else {
            Timber.d("Complete %s!", name);
        }

        checkNotNull(mContent.get(name), String.format("Idling resource with name %s is null", name))
                .setIdleState(!active);
    }

    public void register() {
        Stream.of(mContent)
                .forEach(item -> IdlingRegistry.getInstance().register(item.getValue()));
    }

    public void unregister() {
        Stream.of(mContent)
                .forEach(item -> IdlingRegistry.getInstance().unregister(item.getValue()));
    }
}
