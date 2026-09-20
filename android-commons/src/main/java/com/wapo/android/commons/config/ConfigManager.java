package com.wapo.android.commons.config;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.text.TextUtils;

import com.wapo.android.commons.util.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import rx.subjects.BehaviorSubject;

public class ConfigManager {

    private final static String TAG = ConfigManager.class.getSimpleName();

    private static ConfigManager instance = null;
    private Map<Constants.ConfigType, ConfigModel> configModelMap = new HashMap<Constants.ConfigType, ConfigModel>();

    private Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "configManager") {
                @Override
                public void run() {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    super.run();
                }
            };
        }
    });
    
    private ConfigManager() {}
    public static ConfigManager instance() {
        synchronized (ConfigManager.class) {
            if (instance == null) {
                instance = new ConfigManager();
            }
        }
        return instance;
    }

    public void addConfigModel(Constants.ConfigType type, ConfigModel configModel) {
        // Override existing key/value entry.
        configModelMap.put(type, configModel);
    }

    public BaseConfig getConfigOfType(Constants.ConfigType type) {
        if(type == null) {
            Logger.w(TAG, "Config - getConfigOfType - Params(type) is null!");
            return null;
        }
        ConfigModel configModel = configModelMap.get(type);
        if(configModel != null) {
            return configModel.config;
        }
        return null;
    }

    public BehaviorSubject<BaseConfig> getConfigSubjectOfType(Constants.ConfigType configType) {
        if(configType == null) {
            Logger.w(TAG, "Config - getConfigSubjectOfType - Params(type) is null!");
            return null;
        }
        ConfigModel configModel = configModelMap.get(configType);
        if(configModel != null) {
            return configModel.configSubject;
        }
        return null;
    }

    public void removeAllConfigs() {
        if(configModelMap != null) {
            configModelMap.clear();
        }
    }

    public void loadLocalConfig(@NonNull Context context, Constants.ConfigType configType) {

        if(context == null || configType == null) {
            Logger.w(TAG, "Config - loadLocalConfig - Params(context or type) are null!");
            return;
        }
        if(!configModelMap.containsKey(configType)) {
            Logger.w(TAG, "Config - loadLocalConfig - " + configType + " is not available in map!");
            return;
        }

        ConfigModel configModel = configModelMap.get(configType);
        try {
            JSONObject jsonObject = ConfigHelper.updateAndLoadConfig(context, configModel.resId, configType);
            if(jsonObject != null) {
                configModel.config = BaseConfig.configFromJsonString(context, jsonObject.toString(), configModel.configClass);
                configModel.configSubject.onNext(configModel.config);
                Logger.d(TAG, "Config - ( " + configType.name() + " ) loaded!");
            } else {
                Logger.e(TAG, "Config - loadLocalConfig - Error in loading ( " + configType.name() + " ) with resId or from local config file!");
                // Safe to exit app.
            }
        } catch (JSONException e) {
            // Safe to exit app.
            configModel.config = null;
            e.printStackTrace();
        } catch(RuntimeException e) {
            // Safe to exit app.
            configModel.config = null;
            e.printStackTrace();
        }
    }

    public interface ConfigFailureListener {
        void onFailure(Constants.ConfigType type, Exception e);
    }

    public synchronized void loadRemoteConfig(
            final Context context,
            Constants.ConfigType configType,
            ConfigFailureListener failureListener
    ) {
        if (context == null || configType == null) {
            String errorMessage = "Config - execute - Params(context or type) are null!";
            Logger.w(TAG, errorMessage);
            triggerFailureListener(configType, new IllegalArgumentException(errorMessage), failureListener);
            return;
        }
        if (!configModelMap.containsKey(configType)) {
            String errorMessage = "Config - execute - ConfigType of type( " + configType + " ) is not available in map!";
            Logger.w(TAG, errorMessage);
            triggerFailureListener(configType, new IllegalArgumentException(errorMessage), failureListener);
            return;
        }

        final ConfigModel configModel = configModelMap.get(configType);

        if (TextUtils.isEmpty(configModel.url)) {
            String errorMessage = "Config - execute - url is empty!";
            Logger.w(TAG, errorMessage);
            triggerFailureListener(configType, new IllegalArgumentException(errorMessage), failureListener);
            return;
        }

        ConfigProcessor configProcessor = new ConfigProcessor(
                context,
                configType,
                configModel.url,
                ConfigHelper.getFileName(configType),
                new ConfigProcessor.OnCompleteListener() {
                    @Override
                    public void onComplete() {
                    }

                    @Override
                    public void onSuccess(@NonNull JSONObject jsonObject, Constants.ConfigType configType) throws JSONException {
                        Logger.d(TAG, "Config - Remote ( " + configType.name() + " ) loaded!");
                        configModel.config = BaseConfig.configFromJsonString(context, jsonObject.toString(), configModel.configClass);
                        configModel.configSubject.onNext(configModel.config);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        triggerFailureListener(configType, e, failureListener);
                    }
                }
        );
        execute(configProcessor);
    }

    private void triggerFailureListener(
        @Nullable Constants.ConfigType configType,
        Exception e,
        @Nullable ConfigFailureListener failureListener
    ) {
        Constants.ConfigType type = Objects.requireNonNullElse(configType, Constants.ConfigType.UNKNOWN);
        if (failureListener != null) {
            failureListener.onFailure(type, e);
        }
    }

    private void execute(Runnable task) {
        if(!executor.isShutdown()) {
            executor.execute(task);
        }
    }

    public static class ConfigModel {
        private Class configClass;
        private int resId;
        private String url;
        private BaseConfig config;
        private BehaviorSubject<BaseConfig> configSubject;

        public ConfigModel(Class<? extends BaseConfig> configClass, int configFileResId, String url) {
            this.configClass = configClass;
            this.resId = configFileResId;
            this.url = url;
            configSubject = BehaviorSubject.create();
        }
    }
}
