package com.wapo.flagship.services.data;

import java.util.HashSet;

public abstract class Task {
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_RUNNING = 1;
    public static final int STATUS_COMPLETE = 2;

    protected ITaskProcessor _taskProcessor;
    private TaskStatus _status = TaskStatus.NotInitialized;
    protected long _priority;
    private Throwable _error;
    protected int _priorityClass;
    protected final HashSet<ITaskStatusListener> _listeners = new HashSet<ITaskStatusListener>();

    protected boolean _wasReported = false;
    protected int _lastReportedStatus;
    protected int _lastReportedArg = -1;
    private boolean _wasSuspended = false;

    public Task(int priorityClass, long priority) {
        _priorityClass = priorityClass;
        _priority = priority;
    }

    public abstract void cancel();

    public abstract void execute();

    public synchronized void init(ITaskProcessor processor) {
        _taskProcessor = processor;
        _status = TaskStatus.Pending;
        _wasReported = false;
        _lastReportedStatus = -1;
        _lastReportedArg = -1;
        _error = null;
    }

    public synchronized void finish(ITaskProcessor processor) {
        _listeners.clear();
    }

    public long getPriority() {
        return _priority;
    }

    public int getPriorityClass() {
        return _priorityClass;
    }

    public synchronized TaskStatus getStatus() {
        return _status;
    }

    protected synchronized void setStatus(TaskStatus status) {
        assert status != TaskStatus.Error : "for errors use setError(Exception) method";

        TaskStatus oldStatus = _status;
        _status = status;
        switch(_status) {
            case Complete:
                notifyStatusChange(STATUS_COMPLETE);
                break;

            case Pending:
                if (oldStatus == TaskStatus.NotInitialized) {
                    notifyStatusChange(STATUS_PENDING);
                } else if (oldStatus == TaskStatus.Running) {
                    _wasSuspended = true;
                }
                break;

            case Running:
                if (!_wasSuspended) {
                    notifyStatusChange(STATUS_RUNNING);
                }
                break;

            case Canceled:
                notifyStatusChange(STATUS_COMPLETE);
                break;
        }
    }

    protected synchronized void setError(Throwable error) {
        _status = TaskStatus.Error;
        _error = error;
        notifyError(error);
    }

    public synchronized Throwable getError() {
        return _error;
    }

    public void updatePriority(int priorityClass, long priority) {
        _priorityClass = priorityClass;
        _priority = priority;
    }

    public synchronized void updateFromTask(Task task) {
        assert false : "not implemented";
    }

    public synchronized boolean addListener(ITaskStatusListener listener) {
        if(listener == null) {
            return false;
        }

        if (_listeners.add(listener)) {
            if (_status == TaskStatus.Error) {
                if (_error != null) {
                    listener.onTaskError(_error);
                }
            } else if (_wasReported) {
                listener.onTaskStatusChanged(_lastReportedStatus);
            }

            return false;
        }

        return true;
    }

    protected final synchronized boolean notifyStatusChange(int status) {
        if (_wasReported && _lastReportedStatus == status) {
            return false;
        }

        _lastReportedStatus = status;
        _wasReported = true;
        for (ITaskStatusListener l: _listeners) {
            l.onTaskStatusChanged(status);
        }

        return true;
    }

    protected final synchronized void notifyError(Throwable error) {
        for (ITaskStatusListener l: _listeners) {
            l.onTaskError(error);
        }
    }

    @Override
    public String toString() {
        return String.format("%s, pc: %d, p: %d", this.getClass().getSimpleName(), _priorityClass, _priority);
    }
}
