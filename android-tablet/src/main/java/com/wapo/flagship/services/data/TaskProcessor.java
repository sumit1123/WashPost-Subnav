package com.wapo.flagship.services.data;

import android.content.Context;
import androidx.annotation.NonNull;
import com.wapo.android.commons.util.Logger;
import android.util.Log;
import com.wapo.flagship.data.CacheManager;
import com.wapo.flagship.util.ReachabilityUtil;
import com.washingtonpost.android.config.domain.manager.ConfigManager;
import com.washingtonpost.android.config.domain.models.config.Config;

public class TaskProcessor implements Runnable, ITaskProcessor {
    private static final String TAG = TaskProcessor.class.getName();

    private final PriorityTaskQueue _queue;
    private final Context _context;
    private final CacheManager _cacheManager;
    private final OnCompleteListener _completionCallback;

    public TaskProcessor(
            @NonNull PriorityTaskQueue queue,
            @NonNull Context context,
            @NonNull CacheManager cacheManager,
            @NonNull OnCompleteListener completionCallback
    ) {
        _queue = queue;
        _context = context;
        _cacheManager = cacheManager;
        _completionCallback = completionCallback;
    }

    @Override
    public void run() {
        try {
            //
            // TODO: restore previous tasks
            Task task = null;
            while ((task = _queue.poll()) != null) {
                if (isInterrupted()) {
                    return;
                }

                if (task.getStatus() == TaskStatus.NotInitialized) {
                    task.init(this);
                }

                if (TaskStatus.Pending != task.getStatus()) {
                    String msg = String.format("Unexpected task state before execution; task: %s", task);
                    Logger.e(TAG, msg);

                    assert false : msg;

                    continue;
                }

                try {
                    task.execute();
                } catch (Throwable e) {
                    task.setError(e);
                }

                switch (task.getStatus()) {
                    case Complete:
                    case Canceled:
                        // Task is done, nothing to do here
                        task.finish(this);
                        break;

                    case Pending:
                        // task haven't done yet, put it back into the queue
                        _queue.add(task);
                        break;

                    case Error: {
                        Throwable e = task.getError();
                        String msg = e == null ?
                                String.format("Unknown error while executing task %s", task) :
                                Log.getStackTraceString(e);
                        Logger.w(TAG, msg);
                        task.finish(this);
                        break;
                    }

                    case NotInitialized:
                    case Running: {
                        String msg = String.format("Unexpected task state after execution; task: %s", task);
                        Logger.e(TAG, msg);
                        assert false : msg;
                    }
                }
            }
        } finally {
            _completionCallback.onComplete(this);
        }
    }

    private boolean isInterrupted() {
        return Thread.currentThread().isInterrupted();
    }

    @Override
    public boolean shouldUseNetwork() {
        return ReachabilityUtil.shouldUseNetwork(_context);
    }

    @Override
    public Task enqueueTask(Task task) {
        return _queue.add(task);
    }

    @Override
    public Context getContext() {
        return _context;
    }

    @Override
    public Config getConfig() {
        return ConfigManager.Companion.getInstance().getConfig();
    }

    @Override
    public CacheManager getCacheManager() {
        return _cacheManager;
    }


    public interface OnCompleteListener {
        void onComplete(TaskProcessor processor);
    }
}
