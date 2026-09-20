/*
 * Copyright (c) 2018. The Washington Post. All rights reserved.
 */

package com.wapo.android.remotelog.logger;

import java.util.HashMap;
import java.util.Map;

public class StopWatchFactory {

    private Map<String, Map<String, StopWatch>> stopWatches;

    private static StopWatchFactory instance = null;

    private StopWatchFactory() {
        this.stopWatches = new HashMap<String, Map<String, StopWatch>>();
    }

    public synchronized static StopWatchFactory getInstance() {
        if (instance == null) {
            instance = new StopWatchFactory();
        }
        return instance;
    }

    public void startStopWatch(String baseEvent, String concreteEvent) throws Exception {
        startStopWatch(baseEvent, concreteEvent, null);
    }

    public void restartStopWatch(String baseEvent, String concreteEvent) throws Exception {
        restartStopWatch(baseEvent, concreteEvent, null);
    }

    public void restartStopWatch(String baseEvent, String concreteEvent, String extraMessage) throws Exception {
        if (null == baseEvent || concreteEvent == null) {
            throw new Exception("base Event and concreteevent must both be populated.");
        }
        boolean isOldWatch = false;

        Map<String, StopWatch> baseEventMap = this.stopWatches.get(baseEvent);
        if (baseEventMap != null) {
            StopWatch oldWatch = baseEventMap.get(concreteEvent);
            if (oldWatch != null) {
                oldWatch.restart();
                isOldWatch = true;
            }
        } else {
            baseEventMap = new HashMap<String, StopWatch>();
        }

        if (!isOldWatch) {
            HashMap<String, StopWatch> concreteEventMap = new HashMap<String, StopWatch>();
            StopWatch newWatch = new StopWatch(true);
            if (extraMessage != null) {
                newWatch.setExtraMessage(extraMessage);
            }
            concreteEventMap.put(concreteEvent, newWatch);
            baseEventMap.putAll(concreteEventMap);
            this.stopWatches.put(baseEvent, baseEventMap);
        }


    }

    public void startStopWatch(String baseEvent, String concreteEvent, String extraMessage) throws Exception {
        if (null == baseEvent || concreteEvent == null) {
            throw new Exception("base Event and concreteevent must both be populated.");
        }

        StopWatch newWatch = new StopWatch(true);
        if (extraMessage != null) {
            newWatch.setExtraMessage(extraMessage);
        }

        HashMap<String, StopWatch> concreteEventMap = new HashMap<String, StopWatch>();
        concreteEventMap.put(concreteEvent, newWatch);

        Map<String, StopWatch> baseEventMap = this.stopWatches.get(baseEvent);

        if (null == baseEventMap) {
            baseEventMap = new HashMap<String, StopWatch>();
        }

        baseEventMap.putAll(concreteEventMap);
        this.stopWatches.put(baseEvent, baseEventMap);
    }

    public void stopStopWatch(String baseEvent, String concreteEvent) throws Exception {
        if (baseEvent == null || concreteEvent == null || this.stopWatches.get(baseEvent) == null || this.stopWatches.get(baseEvent).get(concreteEvent) == null) {
            throw new Exception("baseevent or concreteevent was null or was not found: " + baseEvent + ", " + concreteEvent);
        }

        Map<String, StopWatch> baseEventMap = this.stopWatches.get(baseEvent);
        baseEventMap.get(concreteEvent).stop();
    }

    public Map<String, StopWatch> stopStopWatchAndDumpStopWatches(String baseEvent, String concreteEvent) throws Exception {
        this.stopStopWatch(baseEvent, concreteEvent);
        return this.stopWatches.remove(baseEvent);
    }

    public Map<String, StopWatch> dumpStopWatches(String baseEvent) throws Exception {
        if (baseEvent == null || this.stopWatches.get(baseEvent) == null) {
            throw new Exception("baseevent was null or was not found.");
        }

        return this.stopWatches.remove(baseEvent);
    }

    public void addMessageStopWatch(String baseEvent, String concreteEvent, String extraMessage) throws Exception {
        if (null == baseEvent || concreteEvent == null) {
            throw new Exception("base Event and concreteevent must both be populated.");
        }

        Map<String, StopWatch> baseEventMap = this.stopWatches.get(baseEvent);

        baseEventMap.get(concreteEvent).appendExtraMessage(extraMessage);
    }

    public Double getStopTimeInMills(String baseEvent, String concreteEvent) throws Exception {
        if (baseEvent == null || concreteEvent == null || this.stopWatches.get(baseEvent) == null || this.stopWatches.get(baseEvent).get(concreteEvent) == null) {
            throw new Exception("baseevent or concreteevent was null or was not found: " + baseEvent + ", " + concreteEvent);
        }

        Map<String, StopWatch> baseEventMap = this.stopWatches.get(baseEvent);
        return baseEventMap.get(concreteEvent).getStopTimeInMillis();
    }

    public boolean isStopWatchRunning(String baseEvent, String concreteEvent) {
        Map<String, StopWatch> concreteStopWatchMap = this.stopWatches.get(baseEvent);
        if (concreteStopWatchMap == null) {
            return false;
        }

        StopWatch concreteEventVal = concreteStopWatchMap.get(concreteEvent);
        return concreteEventVal != null && !concreteEventVal.isStopped();
    }
}
