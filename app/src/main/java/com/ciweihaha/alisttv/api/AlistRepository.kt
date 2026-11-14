package com.ciweihaha.alisttv.api

import android.content.Context
import com.ciweihaha.alisttv.util.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 给界面层用的仓库：读 Prefs 里的服务器地址和账号，
 * 401 时自动用保存的账号重新登录一次再重试。
 */
class AlistRepository(context: Context) {
    private val prefs = Prefs(context)
    private val client get() = AlistClient(prefs.server)

    val server: String get() = prefs.server

    /** 用保存的账号登录，成功返回 token 并保存。 */
    suspend fun login(): String = withContext(Dispatchers.IO) {
        val token = client.login(prefs.username, prefs.password)
        prefs.token = token
        token
    }

    /** 用给定的服务器地址和账号登录，成功后全部保存。 */
    suspend fun login(server: String, username: String, password: String): String =
        withContext(Dispatchers.IO) {
            val token = AlistClient(server).login(username, password)
            prefs.server = server
            prefs.username = username
            prefs.password = password
            prefs.token = token
            token
        }

    /** 列目录；401 时重登一次再重试，仍失败则抛 AuthException。 */
    suspend fun listFiles(path: String): List<FsItem> = withContext(Dispatchers.IO) {
        try {
            client.listFiles(prefs.token, path)
        } catch (e: AuthException) {
            login()
            client.listFiles(prefs.token, path)
        }
    }

    /** 取文件直链；401 时重登一次再重试。 */
    suspend fun getRawUrl(path: String): String = withContext(Dispatchers.IO) {
        try {
            client.getRawUrl(prefs.token, path)
        } catch (e: AuthException) {
            login()
            client.getRawUrl(prefs.token, path)
        }
    }

    /** 下载图片字节流。 */
    suspend fun fetchImageBytes(url: String): ByteArray = withContext(Dispatchers.IO) {
        client.fetchBytes(url, prefs.token)
    }
}
