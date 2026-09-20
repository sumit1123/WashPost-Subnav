package com.wapo.flagship.features.nightmode;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import com.wapo.android.commons.util.Logger;

class LightSensorManager {
    private static final String TAG = "LightSensorManager";
    private static final int DAY_TO_NIGHT_DEFAULT_THRESHOLD_LUX = 30;
    private static final int NIGHT_TO_DAY_DEFAULT_THRESHOLD_LUX = 50;
    private static final long DELAY_NS = 10_000_000;

    private enum Environment {DAY, NIGHT, UNKNOWN}

    private final SensorManager sensorManager;
    private final Sensor lightSensor;
    private Environment environment = Environment.UNKNOWN;

    private EnvironmentChangedListener environmentChangedListener;
    private int dayToNightDefaultThreshold = DAY_TO_NIGHT_DEFAULT_THRESHOLD_LUX;
    private int nightToDayDefaultThreshold = NIGHT_TO_DAY_DEFAULT_THRESHOLD_LUX;
    private SensorEventListener lightSensorListener;
    private float smoothLux = -1;
    private long timestamp;

    public LightSensorManager(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        lightSensorListener = new LightSensorListener();
    }

    public int getDayToNightDefaultThreshold() {
        return dayToNightDefaultThreshold;
    }

    public void setDayToNightDefaultThreshold(int dayToNightDefaultThreshold) {
        this.dayToNightDefaultThreshold = dayToNightDefaultThreshold;
    }

    public int getNightToDayDefaultThreshold() {
        return nightToDayDefaultThreshold;
    }

    public void setNightToDayDefaultThreshold(int nightToDayDefaultThreshold) {
        this.nightToDayDefaultThreshold = nightToDayDefaultThreshold;
    }

    public EnvironmentChangedListener getEnvironmentChangedListener() {
        return environmentChangedListener;
    }

    public void setEnvironmentChangedListener(EnvironmentChangedListener environmentChangedListener) {
        this.environmentChangedListener = environmentChangedListener;
    }

    public void enable() {
        if (lightSensor != null){
            sensorManager.registerListener(lightSensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Logger.w(TAG, "Light sensor in not supported");
        }
    }

    public void disable() {
        sensorManager.unregisterListener(lightSensorListener);
    }


    private void checkLux() {
        if (smoothLux <= dayToNightDefaultThreshold){
            if (environment == Environment.UNKNOWN || environment == Environment.DAY){
                environment = Environment.NIGHT;
                if (environmentChangedListener != null){
                    environmentChangedListener.onNightDetected();
                }
            }
        } else if (smoothLux >= nightToDayDefaultThreshold){
            if (environment == Environment.UNKNOWN || environment == Environment.NIGHT){
                environment = Environment.DAY;
                if (environmentChangedListener != null){
                    environmentChangedListener.onDayDetected();
                }
            }
        }
    }

    public static interface EnvironmentChangedListener {
        void onDayDetected();
        void onNightDetected();
    }

    private class LightSensorListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent event) {
            float lux = event.values[0];
            if (timestamp == 0){
                timestamp = event.timestamp;
                smoothLux = lux;
                return;
            }

            if (event.timestamp - timestamp < DELAY_NS){
                return;
            }

            timestamp = event.timestamp;

            smoothLux += (lux - smoothLux) / 10;

            checkLux();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    }
}
