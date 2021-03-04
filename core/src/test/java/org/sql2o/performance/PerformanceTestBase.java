package org.sql2o.performance;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;

/**
 * Basically a {@link Runnable} with an Integer input.
 */
public abstract class PerformanceTestBase implements Function<Integer, Void>, AutoCloseable
{
    private Stopwatch watch = Stopwatch.createUnstarted();

    public Void apply(Integer input)
    {
        run(input);
        return null;
    }

    public void initialize()
    {
        watch.reset();
        init();
    }

    public abstract void init();
    public abstract void run(int input);
    @Override
    public abstract void close();

    String getName()
    {
        return getClass().getSimpleName();
    }

    Stopwatch getWatch()
    {
        return watch;
    }
}
