package com.ciweihaha.alisttv.api

import com.google.gson.annotations.SerializedName

// ---------- 登录 ----------

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val code: Int,
    val message: String?,
    val data: TokenData?
)

data class TokenData(
    val token: String
)

// ---------- 列目录 ----------

data class FsListRequest(
    val path: String,
    val page: Int,
    @SerializedName("per_page") val perPage: Int,
    val refresh: Boolean
)

data class FsListResponse(
    val code: Int,
    val message: String?,
    val data: FsListData?
)

data class FsListData(
    val content: List<FsItem>?,
    val total: Int,
    val readme: String?,
    val header: String?,
    val write: Boolean
)

/**
 * Alist / OpenList 的文件对象。
 * type 为对象类型（0 未知、1 文件夹、2 视频、3 音频、4 图片、5 文本），
 * 本应用主要用 is_dir 判断文件夹，用文件后缀判断视频/图片。
 */
data class FsItem(
    val name: String,
    val size: Long,
    @SerializedName("is_dir") val isDir: Boolean,
    val modified: String?,
    val sign: String?,
    val thumb: String?,
    val type: Int,
    val created: String?,
    val hashinfo: String?
)

// ---------- 取文件信息（直链） ----------

data class FsGetRequest(
    val path: String
)

data class FsGetResponse(
    val code: Int,
    val message: String?,
    val data: FsGetData?
)

data class FsGetData(
    val name: String,
    val size: Long,
    @SerializedName("is_dir") val isDir: Boolean,
    val modified: String?,
    val sign: String?,
    val thumb: String?,
    val type: Int,
    @SerializedName("raw_url") val rawUrl: String?,
    val readme: String?,
    val provider: String?,
    val created: String?,
    val hashinfo: String?
)
