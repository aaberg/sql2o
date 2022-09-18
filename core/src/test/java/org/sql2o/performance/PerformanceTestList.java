package org.sql2o.performance;

import com.google.common.base.Function;
import com.google.common.collect.Ordering;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author aldenquimby@gmail.com
 */
public class PerformanceTestList extends ArrayList<PerformanceTestBase>
{
    public void run(int iterations)
    {
        // initialize
        for (PerformanceTestBase test : this)
        {
            test.initialize();
        }

        final Random rand = new Random();

        for (int i = 1; i <= iterations; i++)
        {
            Iterable<PerformanceTestBase> sortedByRandom = orderBy(this, new Function<PerformanceTestBase, Comparable>()
            {
                public Comparable apply(PerformanceTestBase input)
                {
                    return rand.nextInt();
                }
            });

            for (PerformanceTestBase test : sortedByRandom)
            {
                test.getWatch().start();
                test.run(i);
                test.getWatch().stop();
            }
        }

        // close up
        for (PerformanceTestBase test : this)
        {
            test.close();
        }
    }

    public void printResults(String heading)
    {
        Iterable<PerformanceTestBase> sortedByTime = orderBy(this, new Function<PerformanceTestBase, Comparable>()
        {
            public Comparable apply(PerformanceTestBase input)
            {
                return input.getWatch().elapsed(TimeUnit.MILLISECONDS);
            }
        });

        System.out.println(heading + " Results");
        System.out.println("-------------------------");

        PerformanceTestBase fastest = null;

        for (PerformanceTestBase test : sortedByTime)
        {
            long millis = test.getWatch().elapsed(TimeUnit.MILLISECONDS);
            String testName = test.getName().replaceAll(heading + "$", "");

            if (fastest == null)
            {
                fastest = test;
                System.out.println(String.format("%s took %dms", testName, millis));
            }
            else
            {
                long fastestMillis = fastest.getWatch().elapsed(TimeUnit.MILLISECONDS);
                double percentSlower = (double)(millis - fastestMillis)/fastestMillis*100;
                System.out.println(String.format("%s took %dms (%.2f%% slower)", testName, millis, percentSlower));
            }
        }
    }

    private static <T> Iterable<T> orderBy(Iterable<T> iterable, Function<T, ? extends Comparable> selector)
    {
        return Ordering.natural().onResultOf(selector).sortedCopy(iterable);
    }
}
