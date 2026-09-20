package com.wapo.flagship.util.tracking;


import java.util.HashMap;

/**
 * Created by dowelld0-v on 10/13/14.
 */
public class MeasurementMap extends HashMap<String, Object> {

    public void setEvar(String key, Object value) {
        this.put(key, value);
    }

    public Object getEvar(String key) {
        return this.get(key);
    }
}
