package com.ciweihaha.alisttv.util

import android.content.Context
import android.content.SharedPreferences

/**
 * 保存服务器地址、账号和登录 token。
 * token 由登录接口返回，之后每次请求都带在 Authorization 请求头里。
 */
class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var server: String
        get() = sp.getString(KEY_SERVER, DEFAULT_SERVER) ?: DEFAULT_SERVER
        set(value) = sp.edit().putString(KEY_SERVER, value).apply()

    var username: String
        get() = sp.getString(KEY_USERNAME, "") ?: ""
        set(value) = sp.edit().putString(KEY_USERNAME, value).apply()

    var password: String
        get() = sp.getString(KEY_PASSWORD, "") ?: ""
        set(value) = sp.edit().putString(KEY_PASSWORD, value).apply()

    var token: String
        get() = sp.getString(KEY_TOKEN, "") ?: ""
        set(value) = sp.edit().putString(KEY_TOKEN, value).apply()

    val isLoggedIn: Boolean get() = token.isNotBlank()

    fun clearAuth() {
        sp.edit().remove(KEY_TOKEN).apply()
    }

    fun clearAll() {
        sp.edit().clear().apply()
    }

    companion object {
        const val PREFS_NAME = "alist_tv_prefs"
        const val DEFAULT_SERVER = "https://openlist.ciweihaha.win"
        private const val KEY_SERVER = "server"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_TOKEN = "token"
    }
}
