package com.munger.stereocamera.utility;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executor;

public class SingleThreadedExecutor implements Executor
{
    private final Thread thread;
    private final Queue<Runnable> queue;
    private boolean isRunning;
    private final Object lock = new Object();

    public SingleThreadedExecutor(String name)
    {
        isRunning = true;
        queue = new LinkedList<>();
        thread = new Thread(() ->
        {
            Thread.currentThread().setName(name);
            synchronized (lock) {
                while (isRunning) {
                    exec();
                }
            }
        });
        thread.start();
    }

    public int taskCount()
    {
        return queue.size();
    }

    private boolean isExecuting = false;

    public boolean isExecuting()
    {
        return isExecuting;
    }

    public void setPriority(int priority)
    {
        thread.setPriority(priority);
    }

    private void exec()
    {
        if (queue.isEmpty())
            try {lock.wait(); } catch (Exception ignored) { }

        if (!isRunning)
            return;

        Runnable r = queue.remove();
        isExecuting = true;
        r.run();
        isExecuting = false;
    }

    public void stop()
    {
        synchronized (lock)
        {
            if (!isRunning)
                return;

            isRunning = false;
            lock.notify();
        }
    }

    @Override
    public void execute(Runnable command) {
        synchronized (lock)
        {
            if (!isRunning)
                return;

            queue.add(command);
            lock.notify();
        }
    }
}
