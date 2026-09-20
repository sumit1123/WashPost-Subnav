/*
 * Copyright (c) 2018. The Washington Post. All rights reserved.
 */

package com.wapo.android.remotelog.logger;

public enum Level {

    DEBUG {
        public String toString() {
            return "DEBUG";
        }
    },

    WARNING {
        public String toString() {
            return "WARNING";
        }
    },

    ERROR {
        public String toString() {
            return "ERROR";
        }
    },

    //Rainbow specific logging levels
    PAYWALL {
        public String toString() {
            return "PAYWALL";
        }
    },

    VERBOSE {
        public String toString() {
            return "VERBOSE";
        }
    },

    METRICS {
        public String toString() {
            return "METRICS";
        }
    }
}

