package com.petitbear.catuplayer.models

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

/**
 * 网络歌曲搜索结果模型
 */
data class NetSongResponse(
    @SerializedName("result")
    val result: Result
)

data class Result(
    @SerializedName("songs")
    val songs: List<NetSong>
)

data class NetSong(
    @SerializedName("name")
    val name: String,

    @SerializedName("id")
    val id: Long,

    @SerializedName("ar")
    val artists: List<Artist>,

    @SerializedName("al")
    val album: Album?,

    @SerializedName("dt")
    val duration: Long,

    @SerializedName("pop")
    val popularity: Int,

    @SerializedName("fee")
    val feeType: Int,

    @SerializedName("publishTime")
    val publishTime: Long,

    @SerializedName("privilege")
    val privilege: Privilege?  // 添加 privilege 字段
)

data class Artist(
    @SerializedName("id")
    val id: Long,

    @SerializedName("name")
    val name: String
)

data class Album(
    @SerializedName("id")
    val id: Long,

    @SerializedName("name")
    val name: String,

    @SerializedName("picUrl")
    val picUrl: String?
)
data class Privilege(
    @SerializedName("id")
    val id: Long,

    @SerializedName("maxbr")
    val maxBitrate: Int,

    @SerializedName("fee")
    val fee: Int,

    @SerializedName("payed")
    val payed: Int,

    @SerializedName("st")
    val status: Int,

    @SerializedName("pl")
    val playBitrate: Int,

    @SerializedName("dl")
    val downloadBitrate: Int,

    @SerializedName("sp")
    val sp: Int,

    @SerializedName("cp")
    val cp: Int,

    @SerializedName("subp")
    val subp: Int,

    @SerializedName("cs")
    val cs: Boolean,

    @SerializedName("toast")
    val toast: Boolean,

    @SerializedName("flag")
    val flag: Int,

    @SerializedName("preSell")
    val preSell: Boolean,

    @SerializedName("playMaxbr")
    val playMaxBitrate: Int,

    @SerializedName("downloadMaxbr")
    val downloadMaxBitrate: Int,

    @SerializedName("maxBrLevel")
    val maxBitrateLevel: String,

    @SerializedName("playMaxBrLevel")
    val playMaxBitrateLevel: String,

    @SerializedName("downloadMaxBrLevel")
    val downloadMaxBitrateLevel: String,

    @SerializedName("plLevel")
    val playLevel: String,

    @SerializedName("dlLevel")
    val downloadLevel: String,

    @SerializedName("flLevel")
    val freeLevel: String,

    @SerializedName("rightSource")
    val rightSource: Int,

    @SerializedName("code")
    val code: Int,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("chargeInfoList")
    val chargeInfoList: List<ChargeInfo> = emptyList(),

    @SerializedName("freeTrialPrivilege")
    val freeTrialPrivilege: FreeTrialPrivilege? = null
)

data class ChargeInfo(
    @SerializedName("rate")
    val rate: Int,

    @SerializedName("chargeUrl")
    val chargeUrl: String? = null,

    @SerializedName("chargeMessage")
    val chargeMessage: String? = null,

    @SerializedName("chargeType")
    val chargeType: Int
)

// 添加 FreeTrialPrivilege 数据类
data class FreeTrialPrivilege(
    @SerializedName("resConsumable")
    val resConsumable: Boolean,

    @SerializedName("userConsumable")
    val userConsumable: Boolean,

    @SerializedName("listenType")
    val listenType: Int,

    @SerializedName("cannotListenReason")
    val cannotListenReason: Int? = null,

    @SerializedName("playReason")
    val playReason: String? = null
)

data class LyricResponse(
    @SerializedName("sgc")
    val sgc: Boolean,

    @SerializedName("sfy")
    val sfy: Boolean,

    @SerializedName("qfy")
    val qfy: Boolean,

    @SerializedName("code")
    val code: Int,

    @SerializedName("lrc")
    val lrc: LyricContent? = null
)

data class LyricContent(
    @SerializedName("version")
    val version: Int,

    @SerializedName("lyric")
    val lyric: String
)