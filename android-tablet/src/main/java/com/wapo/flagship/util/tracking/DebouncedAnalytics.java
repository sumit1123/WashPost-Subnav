package com.wapo.flagship.util.tracking;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DebouncedAnalytics {
    // A single-threaded scheduler for a timer
    final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> scheduledTask;

    public synchronized void fireEvent(MeasurementMap eventData) {
        // Cancels the previously scheduled task if it exists
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }

        // Schedule a new task to run after a short delay
        scheduledTask = scheduler.schedule(() -> {
            // This code only runs if the timer completes without being reset
            sendAnalytics(eventData);
        }, 500, TimeUnit.MILLISECONDS);
    }

    private void sendAnalytics(MeasurementMap map) {
        Measurement.trackEvent(map, Events.EVENT_ANSWERBOT_THREAD);
    }
}