package com.wapo.flagship.services.data;

import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;

public class PriorityTaskQueue {
    public static final Comparator<? super Task> PriorityComparator = new Comparator<Task>() {
        @Override
        public int compare(Task left, Task right) {
            //
            // reverse order: task with the highest priority should be the first in the queue;
            int lpc = left.getPriorityClass();
            int rpc = right.getPriorityClass();

            if (lpc < rpc) {
                return 1;
            } else if (lpc > rpc) {
                return -1;
            } else {
                long lp = left.getPriority();
                long rp = right.getPriority();
                return lp < rp ? 1 : (lp > rp ? -1 : 0);
            }
        }
    };

    private final HashSet<Task> _tasks = new HashSet<Task>();
    private final PriorityQueue<Task> _priorityQueue = new PriorityQueue<Task>(10, PriorityComparator);

    public synchronized Task poll() {
        Task task = _priorityQueue.poll();
        _tasks.remove(task);

        assert _priorityQueue.size() == _tasks.size() : "PriorityTaskQueue: inconsistency at poll";

        return task;
    }

    public synchronized Task add(Task task) {
        Task existing = null;
        for (Task t: _tasks) {
            if (t.equals(task)) {
                existing = t;
                break;
            }
        }

        if (existing != null) {
            if (PriorityComparator.compare(existing, task) > 0) {
                // existing task has a lower priority
                // we need to update it's priority & reorder the queue
                existing.updatePriority(task.getPriorityClass(), task.getPriority());
                _priorityQueue.clear();
                _priorityQueue.addAll(_tasks);
            }

            assert _priorityQueue.size() == _tasks.size() : "PriorityTaskQueue: inconsistency at add";

            existing.updateFromTask(task);

            return existing;
        }

        _tasks.add(task);
        _priorityQueue.add(task);

        assert _priorityQueue.size() == _tasks.size() : "PriorityTaskQueue: inconsistency at add";

        return task;
    }

    public synchronized void clear() {
        _tasks.clear();
        _priorityQueue.clear();
    }

    public synchronized boolean isEmpty() {
        return _tasks.isEmpty();
    }
}
