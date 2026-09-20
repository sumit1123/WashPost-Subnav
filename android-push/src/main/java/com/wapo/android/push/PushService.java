/*
 *
 *  *  Copyright (c) 2018. The Washington Post. All rights reserved.
 *
 */

package com.wapo.android.push;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.washingtonpost.android.config.domain.models.config.PushConfigStub;

import java.util.ArrayList;
import java.util.List;

public class PushService {

    private static PushService pushService;
    protected PushListener listener;
    private Context _context;
    private PushProvider pushProvider;
    private List<IamProvider> iamProviders;

    private PushService(Context context) {
        this._context = context;
    }

    public static PushService getInstance() {
        return pushService;
    }

    public static void init(Context context,
                            PushProvider pushProvider,
                            PushConfigStub pushConfig,
                            PushListener pushListener) {
        if (pushService == null) {
            pushService = new PushService(context);
        }
        pushService.listener = pushListener;
        pushService.pushProvider = pushProvider;
        pushService.pushProvider.init(context, pushConfig);
    }

    public PushManager getPushManager() {
        return pushProvider.getPushManager();
    }

    public PushListener getListener() {
        return listener;
    }

    public void addIamProvider(IamProvider provider) {
        if (iamProviders == null)
            iamProviders = new ArrayList<>();
        // Add only if there is not already one in the list from the same provider
        Class<? extends IamProvider> newProviderClass = provider.getClass();
        for (IamProvider iamProvider : iamProviders) {
            if (newProviderClass == iamProvider.getClass())
                return;
        }
        iamProviders.add(provider);
    }

    public List<IamProvider> getIamProviders() {
        return iamProviders;
    }

    public interface PushProvider {
        void init(@NonNull Context context, @Nullable PushConfigStub pushConfig);
        @NonNull PushManager getPushManager();
    }

    public interface IamProvider {
        void pauseInAppAutomation(boolean pause);
    }
}

