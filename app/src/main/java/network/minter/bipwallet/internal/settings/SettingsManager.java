package network.minter.bipwallet.internal.settings;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/**
 * minter-android-wallet. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
@SuppressLint("CommitPrefEdits")
public class SettingsManager {
    public static final Key<Boolean> EnableLiveNotifications = new Key<>("enable_live_notifications", true);
    public static final Key<Integer> CurrentBalanceCursor = new Key<>("current_balance_cursor", 0);
    private final static String sPrefsPrefix = "minter_wallet_";
    private final SharedPreferences mPrefs;

    public SettingsManager(SharedPreferences prefs) {
        mPrefs = prefs;
    }

    public boolean getBool(Key<Boolean> key) {
        return mPrefs.getBoolean(key.getName(), key.getDefaultValue());
    }

    public int getInt(Key<Integer> key) {
        return mPrefs.getInt(key.getName(), key.getDefaultValue());
    }

    public float getFloat(Key<Float> key) {
        return mPrefs.getFloat(key.getName(), key.getDefaultValue());
    }

    public long getLong(Key<Long> key) {
        return mPrefs.getLong(key.getName(), key.getDefaultValue());
    }

    public String getString(Key<String> key) {
        return mPrefs.getString(key.getName(), key.getDefaultValue());
    }

    public void putFloat(Key<Float> key, float value, boolean sync) {
        put(mPrefs.edit().putFloat(key.getName(), value), sync);
    }

    public void putFloat(Key<Float> key, float value) {
        put(mPrefs.edit().putFloat(key.getName(), value), false);
    }

    public void putLong(Key<Long> key, long value, boolean sync) {
        put(mPrefs.edit().putLong(key.getName(), value), sync);
    }

    public void putLong(Key<Long> key, long value) {
        put(mPrefs.edit().putLong(key.getName(), value), false);
    }

    public void putInt(Key<Integer> key, int value, boolean sync) {
        put(mPrefs.edit().putInt(key.getName(), value), sync);
    }

    public void putInt(Key<Integer> key, int value) {
        put(mPrefs.edit().putInt(key.getName(), value), false);
    }

    public void putString(Key<String> key, String value, boolean sync) {
        put(mPrefs.edit().putString(key.getName(), value), sync);
    }

    public void putString(Key<String> key, String value) {
        put(mPrefs.edit().putString(key.getName(), value), false);
    }

    public void putBool(Key<Boolean> key, boolean value, boolean sync) {
        put(mPrefs.edit().putBoolean(key.getName(), value), sync);
    }

    public void putBool(Key<Boolean> key, boolean value) {
        put(mPrefs.edit().putBoolean(key.getName(), value), false);
    }

    private void put(SharedPreferences.Editor editor, boolean immediately) {
        if (immediately) {
            editor.commit();
            return;
        }

        editor.apply();


    }

    public static final class Key<T> {
        private final String mName;
        private final T mDefaultValue;

        Key(String name, T defaultValue) {
            mName = sPrefsPrefix + name;
            mDefaultValue = defaultValue;
        }

        public String getName() {
            return mName;
        }

        public T getDefaultValue() {
            return mDefaultValue;
        }

        @NonNull
        @Override
        public String toString() {
            return getName();
        }
    }
}
