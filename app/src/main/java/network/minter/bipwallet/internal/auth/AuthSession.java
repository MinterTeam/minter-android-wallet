/*
 * Copyright (C) 2018 by MinterTeam
 * @link https://github.com/MinterTeam
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

package network.minter.bipwallet.internal.auth;

import android.support.annotation.NonNull;

import com.annimon.stream.Stream;

import java.util.ArrayList;
import java.util.List;

import network.minter.mintercore.internal.common.Lazy;
import network.minter.mintercore.internal.common.LazyMem;
import network.minter.my.models.User;

import static network.minter.bipwallet.internal.auth.AuthSession.AuthType.None;
import static network.minter.bipwallet.internal.common.Preconditions.checkNotNull;

/**
 * Dogsy. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class AuthSession {
    @SuppressWarnings("SpellCheckingInspection")
    public static final String AUTH_TOKEN_ADVANCED = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFULL";
    final static String TOKEN_RESTORATION_KEY = "TOKEN_RESTORATION_KEY";
    final static String TYPE_RESTORATION_KEY = "TYPE_RESTORATION_KEY";
    final static String USER_RESTORATION_KEY = "USER_RESTORATION_KEY";
    final static String AVATAR_RESTORATION_KEY = "AVATAR_RESTORATION_KEY";
    private boolean mIsLoggedIn = false;
    private AuthType mAuthType = None;
    private String mAuthToken;
    private User mUser;
    private Lazy<List<LogoutListener>> mLogoutListeners = LazyMem.memoize(ArrayList::new);
    private Lazy<List<LoginListener>> mLoginListeners = LazyMem.memoize(ArrayList::new);
    private SessionStorage mStorage;

    public enum AuthType {
        None,
        Advanced,
        Basic,
    }

    public AuthSession(SessionStorage storage) {
        mStorage = storage;
    }

    /**
     * Sets bool flag, does not notifies listeners
     */
    public void setIsLoggedIn() {
        setIsLoggedIn(true);
    }

    /**
     * Sets bool flag, does not notifies listeners
     *
     * @param b true whether is logged int
     */
    public void setIsLoggedIn(boolean b) {
        mIsLoggedIn = b;
    }

    /**
     * Notifies listeners if not logged out already
     *
     * @see LogoutListener
     * @see #addLogoutListener(LogoutListener)
     */
    public synchronized void logout() {
        if (!isLoggedIn()) {
            return;
        }

        clear();
        mAuthToken = null;

        Stream.of(mLogoutListeners.get())
                .forEach(LogoutListener::onLogout);

        mIsLoggedIn = false;
    }

    /**
     * Check is logged in and auth token exists
     *
     * @return true if has positive flag and auth token is not null
     */
    public boolean isLoggedIn(boolean tryRestore) {
        boolean logged = mIsLoggedIn && mAuthToken != null;
        if (!logged) {
            restore();
        }

        return mIsLoggedIn && mAuthToken != null;
    }

    /**
     * Check is logged in and auth token exists
     *
     * @return true if has positive flag and auth token is not null
     */
    public boolean isLoggedIn() {
        return mIsLoggedIn && mAuthToken != null;
    }

    /**
     * User type: advanced or dummy
     *
     * @return enum type
     * @see AuthType
     */
    public AuthType getRole() {
        return mAuthType;
    }

    /**
     * Clean up session payload
     */
    public void clear() {
        if (mAuthToken == null) return;
        mStorage.remove(TOKEN_RESTORATION_KEY);
        mStorage.remove(USER_RESTORATION_KEY);
    }

    public String getAuthToken() {
        return mAuthToken;
    }

    public void setAuthToken(final String authToken) {
        mAuthToken = authToken;
    }

    public User getUser() {
        if (mUser == null && isLoggedIn()) {
            restore();
        }
        return mUser;
    }

    public void setUser(User user) {
        mUser = user;
        save();
    }

    /**
     * Add login listener
     *
     * @param l
     * @see LoginListener
     */
    public AuthSession addLoginListener(LoginListener l) {
        if (l == null) return this;
        mLoginListeners.get().add(l);
        return this;
    }

    /**
     * Removes login listener by the same object instance
     *
     * @param l
     * @see LoginListener
     */
    public void removeLoginListener(LoginListener l) {
        if (l == null) return;
        mLoginListeners.get().remove(l);
    }

    /**
     * Add logout listener
     *
     * @param l Listener single-method interface
     * @see LogoutListener
     */
    public AuthSession addLogoutListener(LogoutListener l) {
        if (l == null) return this;
        mLogoutListeners.get().add(l);
        return this;
    }

    /**
     * Remove logout listener
     *
     * @param l Listener single-method interface
     */
    public void removeLogoutListener(LogoutListener l) {
        if (l == null) return;
        mLogoutListeners.get().remove(l);
    }

    /**
     * @return Session storage
     * @see android.content.SharedPreferences
     */
    public SessionStorage getStorage() {
        return mStorage;
    }

    /**
     * Restore session payload from persistent storage
     *
     * @see android.content.SharedPreferences
     */
    public void restore() {
        if (mStorage.has(TOKEN_RESTORATION_KEY) && mStorage.has(USER_RESTORATION_KEY)) {
            final String token = mStorage.get(TOKEN_RESTORATION_KEY, String.class);
            final User user = mStorage.get(USER_RESTORATION_KEY, User.class);
            final Integer type = mStorage.get(TYPE_RESTORATION_KEY, Integer.class);

            checkNotNull(type, "Type is null!");
            checkNotNull(user, "User is null!");

            if (token != null) {
                login(token, user, AuthType.values()[type]);
                user.token.accessToken = getAuthToken();
            }
        }
    }

    /**
     * Save session to persistent storage (shared preference)
     *
     * @see android.content.SharedPreferences
     */
    public void save() {
        if (mAuthToken == null) return;
        mStorage.set(TOKEN_RESTORATION_KEY, mAuthToken);
        mStorage.set(TYPE_RESTORATION_KEY, mAuthType.ordinal());
        mStorage.set(USER_RESTORATION_KEY, mUser);
    }

    /**
     * Notifies listeners
     *
     * @param authToken service auth token
     * @see LoginListener
     * @see #addLoginListener(LoginListener)
     */
    public synchronized void login(@NonNull final String authToken, User user, AuthType type) {
        checkNotNull(authToken, "Auth token required!");
        mIsLoggedIn = true;
        mAuthToken = authToken;
        mUser = user;
        mAuthType = type;
        Stream.of(mLoginListeners.get())
                .forEach(item -> item.onLogin(mAuthToken));

        save();
    }

    public interface LoginListener {
        void onLogin(final String authToken);
    }

    public interface LogoutListener {
        void onLogout();
    }
}
