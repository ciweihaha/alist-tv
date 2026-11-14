package com.ciweihaha.alisttv.api

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiException(message: String) : IOException(message)

/** 服务器返回 401：token 过期或无效。 */
class AuthException(message: String = "登录已过期") : IOException(message)

/**
 * Alist / OpenList HTTP API 客户端（OkHttp + Gson）。
 *
 * 接口约定（与 OpenList 实测一致）：
 * - 登录：POST /api/auth/login，body {"username","password"}，
 *   返回 {"code":200,"data":{"token":"..."}}。
 * - 列目录：POST /api/fs/list，body {"path","page","per_page","refresh"}，
 *   请求头 Authorization: <token>（只有 token 本身，不带 Bearer 前缀），
 *   返回 {"code":200,"data":{"content":[{name,size,is_dir,type,thumb,modified…}]}}。
 * - 取直链：POST /api/fs/get，body {"path"}，返回 data.raw_url。
 */
class AlistClient(private val server: String) {

    private val baseUrl = server.trimEnd('/')
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    /** 登录，成功返回 token；失败抛 ApiException。 */
    fun login(username: String, password: String): String {
        val body = post("/api/auth/login", LoginRequest(username, password), null)
        val resp = gson.fromJson(body, LoginResponse::class.java)
        if (resp.code == 200 && resp.data != null && resp.data.token.isNotBlank()) {
            return resp.data.token
        }
        throw ApiException(resp.message ?: "登录失败")
    }

    /** 列出目录的全部内容（自动翻页，直到一页不满 per_page）。401 抛 AuthException。 */
    fun listFiles(token: String, path: String, refresh: Boolean = false): List<FsItem> {
        val result = mutableListOf<FsItem>()
        var page = 1
        while (true) {
            val body = post("/api/fs/list", FsListRequest(path, page, PER_PAGE, refresh), token)
            val resp = gson.fromJson(body, FsListResponse::class.java)
            if (resp.code == 401) throw AuthException()
            if (resp.code != 200 || resp.data == null) {
                throw ApiException(resp.message ?: "读取目录失败")
            }
            val content = resp.data.content ?: emptyList()
            result.addAll(content)
            if (content.size < PER_PAGE) break
            page++
            if (page > MAX_PAGES) break
        }
        return result
    }

    /**
     * 取文件的播放/下载直链。
     * 优先用 data.raw_url；如果为空（个别驱动不返回直链），
     * 回退到本站 /d 直链：{server}/d{path}?sign={sign}。
     */
    fun getRawUrl(token: String, path: String): String {
        val body = post("/api/fs/get", FsGetRequest(path), token)
        val resp = gson.fromJson(body, FsGetResponse::class.java)
        if (resp.code == 401) throw AuthException()
        if (resp.code != 200 || resp.data == null) {
            throw ApiException(resp.message ?: "获取文件失败")
        }
        val data = resp.data
        val raw = data.rawUrl
        if (!raw.isNullOrBlank()) return resolveUrl(raw)
        val sign = data.sign ?: ""
        val suffix = if (sign.isBlank()) "" else "?sign=$sign"
        return baseUrl + "/d" + path + suffix
    }

    /** 下载图片字节流。直链与本站同域时才带 Authorization，避免把 token 发给第三方。 */
    fun fetchBytes(url: String, token: String?): ByteArray {
        val builder = Request.Builder().url(url)
        if (token != null && isSameHost(url)) builder.header("Authorization", token)
        http.newCall(builder.build()).execute().use { resp ->
            if (!resp.isSuccessful) throw ApiException("下载失败: ${resp.code}")
            return resp.body?.bytes() ?: throw ApiException("空响应")
        }
    }

    private fun post(path: String, payload: Any, token: String?): String {
        val json = gson.toJson(payload)
        val requestBody = json.toRequestBody(JSON_MEDIA_TYPE)
        val builder = Request.Builder().url(baseUrl + path).post(requestBody)
        if (token != null) builder.header("Authorization", token)
        http.newCall(builder.build()).execute().use { resp ->
            val text = resp.body?.string() ?: ""
            // 401 由调用方按 JSON 里的 code 判断，不在这里当网络错误抛掉。
            // 空响应体时补一个等价 JSON，避免 Gson 解析空串崩掉。
            if (resp.code == 401) {
                return text.ifBlank { """{"code":401,"message":"未登录或登录已过期"}""" }
            }
            if (!resp.isSuccessful) throw ApiException("网络错误: ${resp.code}")
            return text
        }
    }

    private fun resolveUrl(raw: String): String {
        if (raw.startsWith("http://") || raw.startsWith("https://")) return raw
        if (raw.startsWith("/")) return baseUrl + raw
        return "$baseUrl/$raw"
    }

    private fun isSameHost(url: String): Boolean {
        return try {
            val a = java.net.URL(url).host
            val b = java.net.URL(baseUrl).host
            a.equals(b, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        const val PER_PAGE = 100
        private const val MAX_PAGES = 50
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
