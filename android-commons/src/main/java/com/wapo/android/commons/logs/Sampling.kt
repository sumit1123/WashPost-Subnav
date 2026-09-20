package com.wapo.android.commons.logs

/**
 * Named sampling rules supported by remote log configuration.
 *
 * The configured sampling value is a keep threshold in the 0.0..1.0 range.
 * For example, `0.1` keeps about 10% of logs and drops the rest.
 *
 * @property key Key used to look up the corresponding sampling rate.
 */
enum class Sampling(val key: String) {
    /** Uses the `none` sampling rule from the config */
    None("none"),

    /** Uses the `one` sampling rule from the config */
    One("one"),
}