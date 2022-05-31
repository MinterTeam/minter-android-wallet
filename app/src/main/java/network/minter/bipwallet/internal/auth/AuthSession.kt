/*
 * Copyright (C) by MinterTeam. 2022
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
package network.minter.bipwallet.internal.auth

import network.minter.bipwallet.internal.common.Preconditions
import network.minter.bipwallet.internal.storage.KVStorage

/**
 * minter-android-wallet. 2018
 * @author Eduard Maximovich <edward.vstock></edward.vstock>@gmail.com>
 */
class AuthSession(private val storage: KVStorage) {
    private var _isLoggedIn = false
    private var authType: AuthType? = AuthType.None
    var authToken: String? = null
    private val mLogoutListeners: MutableList<LogoutListener> by lazy { mutableListOf() }
    private val mLoginListeners: MutableList<LoginListener> by lazy { mutableListOf() }

    enum class AuthType {
        None, Advanced, Basic, Hardware
    }

    /**
     * Sets bool flag, does not notifies listeners
     */
    fun setIsLoggedIn() {
        isLoggedIn = true
    }

    /**
     * Notifies listeners if not logged out already
     * @see LogoutListener
     *
     * @see .addLogoutListener
     */
    @Synchronized
    fun logout() {
        if (!isLoggedIn) {
            return
        }
        clear()
        authToken = null
        authType = null
        mLogoutListeners.forEach { it.onLogout() }
        _isLoggedIn = false
    }

    /**
     * Check is logged in and auth token exists
     * @return true if has positive flag and auth token is not null
     */
    fun isLoggedIn(tryRestore: Boolean): Boolean {
        if (tryRestore) {
            val logged = _isLoggedIn && authToken != null
            if (!logged) {
                restore()
            }
        }
        return _isLoggedIn && authToken != null
    }
    /**
     * Check is logged in and auth token exists
     * @return true if has positive flag and auth token is not null
     */
    /**
     * Sets bool flag, does not notifies listeners
     * @param b true whether is logged int
     */
    var isLoggedIn: Boolean
        get() = isLoggedIn(true)
        set(b) {
            _isLoggedIn = b
        }

    val isAdvancedUser: Boolean
        get() = role == AuthType.Advanced || role == AuthType.Hardware

    /**
     * User type: advanced or dummy
     * @return enum type
     * @see AuthType
     */
    val role: AuthType
        get() {
            if (authType == null) {
                authType = AuthType.None
            }
            return authType!!
        }

    /**
     * Clean up session payload
     */
    fun clear() {
        if (authToken == null) return
        storage.delete(TOKEN_RESTORATION_KEY)
        storage.delete(TYPE_RESTORATION_KEY)
    }

    /**
     * Add login listener
     * @param l
     * @see LoginListener
     */
    fun addLoginListener(l: LoginListener?): AuthSession {
        if (l == null) return this
        mLoginListeners.add(l)
        return this
    }

    /**
     * Removes login listener by the same object instance
     * @param l
     * @see LoginListener
     */
    fun removeLoginListener(l: LoginListener?) {
        if (l == null) return
        mLoginListeners.remove(l)
    }

    /**
     * Add logout listener
     * @param l Listener single-method interface
     * @see LogoutListener
     */
    fun addLogoutListener(l: LogoutListener?): AuthSession {
        if (l == null) return this
        mLogoutListeners.add(l)
        return this
    }

    /**
     * Remove logout listener
     * @param l Listener single-method interface
     */
    fun removeLogoutListener(l: LogoutListener?) {
        if (l == null) return
        mLogoutListeners.remove(l)
    }

    /**
     * Restore session payload from persistent storage
     * @see android.content.SharedPreferences
     */
    fun restore() {
        if (storage.contains(TOKEN_RESTORATION_KEY)) {
            val token = storage.get<String>(TOKEN_RESTORATION_KEY)
            val type = storage.get<Int>(TYPE_RESTORATION_KEY)
            Preconditions.checkNotNull(type, "Type is null!")
            if (token != null) {
                login(token, AuthType.values()[type!!])
            }
        }
    }

    /**
     * Save session to persistent storage (shared preference)
     * @see android.content.SharedPreferences
     */
    fun save() {
        authToken?.let {
            storage.put(TOKEN_RESTORATION_KEY, it)
        }
        authType?.let {
            storage.put(TYPE_RESTORATION_KEY, it.ordinal)
        }
    }

    /**
     * Notifies listeners
     * @param authToken service auth token
     * @see LoginListener
     *
     * @see .addLoginListener
     */
    @Synchronized
    fun login(authToken: String?, type: AuthType?) {
        authToken ?: throw IllegalStateException("Auth token required")

        _isLoggedIn = true
        this.authToken = authToken
        authType = type
        mLoginListeners.forEach { it.onLogin(authToken) }
        save()
    }

    interface LoginListener {
        fun onLogin(authToken: String?)
    }

    interface LogoutListener {
        fun onLogout()
    }

    companion object {
        const val AUTH_TOKEN_ADVANCED = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFULL"
        const val TOKEN_RESTORATION_KEY = "auth_session_restoration_token"
        const val TYPE_RESTORATION_KEY = "auth_session_type"
        const val AVATAR_RESTORATION_KEY = "auth_session_avatar"
    }
}