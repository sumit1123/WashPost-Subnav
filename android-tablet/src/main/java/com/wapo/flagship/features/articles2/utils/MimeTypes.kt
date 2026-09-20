package com.wapo.flagship.features.articles2.utils

const val BASE_TYPE_AUDIO = "audio"

const val AUDIO_MPEG: String =
    "$BASE_TYPE_AUDIO/mpeg"
const val AUDIO_MP4: String =
    "$BASE_TYPE_AUDIO/mp4"
const val AUDIO_AAC: String =
    "$BASE_TYPE_AUDIO/mp4a-latm"
const val AUDIO_WEBM: String =
    "$BASE_TYPE_AUDIO/webm"
const val AUDIO_MPEG_L1: String =
    "$BASE_TYPE_AUDIO/mpeg-L1"
const val AUDIO_MPEG_L2: String =
    "$BASE_TYPE_AUDIO/mpeg-L2"
const val AUDIO_RAW: String =
    "$BASE_TYPE_AUDIO/raw"
const val AUDIO_ALAW: String =
    "$BASE_TYPE_AUDIO/g711-alaw"
const val AUDIO_MLAW: String =
    "$BASE_TYPE_AUDIO/g711-mlaw"
const val AUDIO_AC3: String =
    "$BASE_TYPE_AUDIO/ac3"
const val AUDIO_E_AC3: String =
    "$BASE_TYPE_AUDIO/eac3"
const val AUDIO_E_AC3_JOC: String =
    "$BASE_TYPE_AUDIO/eac3-joc"
const val AUDIO_AC4: String =
    "$BASE_TYPE_AUDIO/ac4"
const val AUDIO_CUSTOM_EC3: String =
    "$BASE_TYPE_AUDIO/ec3" // AMZN_CHANGE_ONELINE
const val AUDIO_TRUEHD: String =
    "$BASE_TYPE_AUDIO/true-hd"
const val AUDIO_DTS: String =
    "$BASE_TYPE_AUDIO/vnd.dts"
const val AUDIO_DTS_HD: String =
    "$BASE_TYPE_AUDIO/vnd.dts.hd"
const val AUDIO_DTS_EXPRESS: String =
    "$BASE_TYPE_AUDIO/vnd.dts.hd;profile=lbr"
const val AUDIO_VORBIS: String =
    "$BASE_TYPE_AUDIO/vorbis"
const val AUDIO_OPUS: String =
    "$BASE_TYPE_AUDIO/opus"
const val AUDIO_AMR_NB: String =
    "$BASE_TYPE_AUDIO/3gpp"
const val AUDIO_AMR_WB: String =
    "$BASE_TYPE_AUDIO/amr-wb"
const val AUDIO_FLAC: String =
    "$BASE_TYPE_AUDIO/flac"
const val AUDIO_ALAC: String =
    "$BASE_TYPE_AUDIO/alac"
const val AUDIO_MSGSM: String =
    "$BASE_TYPE_AUDIO/gsm"
const val AUDIO_UNKNOWN: String =
    "$BASE_TYPE_AUDIO/x-unknown"

val validMimeTypes =
    listOf(
        AUDIO_MPEG,
        AUDIO_MP4,
        AUDIO_AAC,
        AUDIO_WEBM,
        AUDIO_MPEG_L1,
        AUDIO_MPEG_L2,
        AUDIO_RAW,
        AUDIO_ALAW,
        AUDIO_MLAW,
        AUDIO_AC3,
        AUDIO_CUSTOM_EC3,
        AUDIO_E_AC3,
        AUDIO_E_AC3_JOC,
        AUDIO_AC4,
        AUDIO_TRUEHD,
        AUDIO_DTS,
        AUDIO_DTS_HD,
        AUDIO_DTS_EXPRESS,
        AUDIO_VORBIS,
        AUDIO_OPUS,
        AUDIO_AMR_NB,
        AUDIO_AMR_WB,
        AUDIO_FLAC,
        AUDIO_ALAC,
        AUDIO_MSGSM,
        AUDIO_UNKNOWN,
    )
