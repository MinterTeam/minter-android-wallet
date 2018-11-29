/*
 * Copyright (C) by MinterTeam. 2018
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

package network.minter.bipwallet.internal.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.support.annotation.RawRes;

import java.util.HashMap;
import java.util.Map;

import network.minter.bipwallet.R;
import network.minter.bipwallet.internal.common.Lazy;

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public final class SoundManager {

    private final Context mContext;
    private final Lazy<Boolean> mEnabled;
    private SoundPool mPool;
    private Map<Integer, Integer> mSoundMap;
    private boolean mInit = false;

    @SuppressLint("UseSparseArrays")
    public SoundManager(Lazy<Boolean> enabled, Context context) {
        mContext = context;
        mEnabled = enabled;
        mPool = new SoundPool(1, AudioManager.STREAM_SYSTEM, 0);
        mSoundMap = new HashMap<>(4);
        loadAll();
    }

    @SuppressWarnings("ConstantConditions")
    public void play(final @RawRes int soundId) {
        if (!mEnabled.get()) {
            return;
        }
        if (!mSoundMap.containsKey(soundId)) {
            mSoundMap.put(soundId, mPool.load(mContext, soundId, 1));
        }

        mPool.play(mSoundMap.get(soundId), 1, 1, 0, 0, 1);
    }

    public void loadAll() {
        if (mInit) {
            return;
        }
        mPool = new SoundPool(1, AudioManager.STREAM_SYSTEM, 0);

        mSoundMap.put(R.raw.bip_beep_digi_octave, mPool.load(mContext, R.raw.bip_beep_digi_octave, 1));
        mSoundMap.put(R.raw.cancel_pop_hi, mPool.load(mContext, R.raw.cancel_pop_hi, 1));
        mSoundMap.put(R.raw.click_pop_zap, mPool.load(mContext, R.raw.click_pop_zap, 1));
        mSoundMap.put(R.raw.refresh_pop_down, mPool.load(mContext, R.raw.refresh_pop_down, 1));
        mInit = true;
    }

    public void releaseAll() {
        if (!mInit) {
            return;
        }

        if (mPool != null) {
            mPool.release();
            mPool = null;
        }

        if (mSoundMap.size() > 0) {
            mSoundMap.clear();
        }
        mInit = false;
    }
}
