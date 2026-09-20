/*
 * Copyright (c) 2018. The Washington Post. All rights reserved.
 */

package com.wapo.android.remotelog.logger;

public class StopWatch {

    private long startTime = 0;
    private Double stopTimeInMillis = new Double(0);
    private String extraMessage;
    private Double prevRunTime = new Double(0);

    public StopWatch() {
        this(false);
    }

    public StopWatch(boolean startOnCreate) {
        if (startOnCreate) {
            this.startTime = System.nanoTime();
        }
    }

    public void start() {
        if (this.startTime == 0) {
            this.startTime = System.nanoTime();
        }
    }

    public void restart() {
        if (this.startTime > 0) {
            this.prevRunTime = getStopTimeInMillis();
            this.startTime = 0;
            start();
        } else {
            start();
        }
    }

    public void stop() {
        this.stopTimeInMillis = (double) ((System.nanoTime() - this.startTime) / (1000 * 1000));
    }

    public Double getStopTimeInMillis() {
        return this.stopTimeInMillis;
    }

    public Double getTotalTimeInMillis() {
        return this.prevRunTime + this.stopTimeInMillis;
    }

    public String getExtraMessage() {
        return extraMessage;
    }

    public void setExtraMessage(String extraMessage) {
        this.extraMessage = extraMessage;
    }

    public void appendExtraMessage(String appendedExtraMessage) {
        StringBuilder sb = new StringBuilder(this.extraMessage);
        this.extraMessage = sb.append(appendedExtraMessage).toString();
    }

    public boolean isStopped() {
        return stopTimeInMillis != 0;
    }
}
