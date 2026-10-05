package org.sql2o.tools;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Cache}.
 *
 * <p>Cache is shared between threads, so most of these tests are about what concurrent callers are guaranteed:
 * the delegate runs once per key, and everyone gets that one value.
 */
public class CacheTest {

    private static final int THREADS = 4;
    private static final long TIMEOUT_SECONDS = 20;

    private static String value(String key) {
        return "value of " + key;
    }

    private static Callable<String> countingDelegate(AtomicInteger calls) {
        return () -> {
            calls.incrementAndGet();
            return "value";
        };
    }

    @Test
    public void theDelegateProvidesTheFirstValue() throws Exception {
        final Cache<String, String> cache = new Cache<>();
        final AtomicInteger calls = new AtomicInteger();

        assertEquals("value", cache.get("key", countingDelegate(calls)));
        assertEquals(1, calls.get());
    }

    @Test
    public void aCachedValueIsReturnedWithoutCallingTheDelegate() throws Exception {
        final Cache<String, String> cache = new Cache<>();
        final AtomicInteger calls = new AtomicInteger();
        cache.get("key", countingDelegate(calls));

        assertEquals("value", cache.get("key", countingDelegate(calls)));
        assertEquals(1, calls.get());
    }

    @Test
    public void keysAreIndependent() throws Exception {
        final Cache<String, String> cache = new Cache<>();

        assertEquals("value of a", cache.get("a", () -> value("a")));
        assertEquals("value of b", cache.get("b", () -> value("b")));
        assertEquals("value of a", cache.get("a", () -> {
            throw new AssertionError("must not be asked again");
        }));
    }

    /**
     * The fast path only accepts a non-null value, so a delegate that returns null leaves nothing to cache and
     * runs again on the next request.
     */
    @Test
    public void aNullFromTheDelegateIsNotCached() throws Exception {
        final Cache<String, String> cache = new Cache<>();
        final AtomicInteger calls = new AtomicInteger();

        assertNull(cache.get("key", () -> null));
        assertEquals("second", cache.get("key", () -> {
            calls.incrementAndGet();
            return "second";
        }));
        assertEquals(1, calls.get());
    }

    @Test
    public void aFailingDelegateIsReported() {
        final Cache<String, String> cache = new Cache<>();

        final var ex = assertThrows(RuntimeException.class, () -> cache.get("key", () -> {
            throw new IllegalStateException("boom");
        }));

        assertEquals("Error while getting value from cache", ex.getMessage());
        assertInstanceOf(IllegalStateException.class, ex.getCause());
    }

    @Test
    public void aValueIsEvaluatedOnceEvenWhenEveryoneAsksAtTheSameTime() throws Exception {
        final Cache<String, String> cache = new Cache<>();
        final AtomicInteger calls = new AtomicInteger();
        final ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            final CountDownLatch startTogether = new CountDownLatch(1);
            final List<Future<String>> results = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                results.add(pool.submit(() -> {
                    startTogether.await();
                    return cache.get("key", countingDelegate(calls));
                }));
            }

            startTogether.countDown();
            for (final Future<String> result : results) {
                assertEquals("value", result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, calls.get());
    }

    /**
     * Covers the recheck under the write lock: the second thread reaches the lock while the first is still
     * evaluating, and finds the value already cached once the first one lets go.
     */
    @Test
    public void aThreadThatWaitedForTheWriteLockReusesTheValueItFound() throws Exception {
        final Cache<String, String> cache = new Cache<>();
        final AtomicInteger calls = new AtomicInteger();
        final CountDownLatch insideDelegate = new CountDownLatch(1);
        final CountDownLatch letDelegateFinish = new CountDownLatch(1);
        final ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            final Future<String> first = pool.submit(() -> cache.get("key", () -> {
                calls.incrementAndGet();
                insideDelegate.countDown();
                assertTrue(letDelegateFinish.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
                return "value";
            }));
            assertTrue(insideDelegate.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

            final Future<String> second = pool.submit(() -> cache.get("key", countingDelegate(calls)));
            // the cache is still empty, so let the second thread get past the unsynchronised read and onto the lock
            Thread.sleep(100);

            letDelegateFinish.countDown();

            assertEquals("value", first.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            assertEquals("value", second.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, calls.get());
    }

    /**
     * Many keys in parallel. What is being pinned here is that the delegate still runs exactly once per key:
     * the unsynchronised read at {@link Cache} before the lock may miss an entry, but the recheck under the
     * write lock catches that. The cost of that read is a data race on the HashMap internals, not a wrong value.
     */
    @Test
    public void parallelWorkOnManyKeysStaysConsistent() throws Exception {
        final int keyCount = 300;
        final Cache<Integer, String> cache = new Cache<>();
        final AtomicInteger calls = new AtomicInteger();
        final ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            final List<Future<List<String>>> rounds = new ArrayList<>();
            for (int t = 0; t < THREADS; t++) {
                rounds.add(pool.submit(() -> {
                    final List<String> seen = new ArrayList<>();
                    for (int i = 0; i < keyCount; i++) {
                        final int key = i;
                        seen.add(cache.get(key, () -> {
                            calls.incrementAndGet();
                            return "value " + key;
                        }));
                    }
                    return seen;
                }));
            }

            for (final Future<List<String>> round : rounds) {
                final List<String> seen = round.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                for (int key = 0; key < keyCount; key++) {
                    assertEquals("value " + key, seen.get(key));
                }
            }
        } finally {
            pool.shutdownNow();
        }

        // every key is still in the cache and nobody has to recompute it
        for (int i = 0; i < keyCount; i++) {
            final int key = i;
            assertEquals("value " + key, cache.get(key, () -> {
                throw new AssertionError("key " + key + " was lost");
            }));
        }
        // exactly once per key: the unlocked read can only cost an extra trip to the write lock, because the
        // recheck under that lock is what decides whether the delegate runs
        assertEquals(keyCount, calls.get(), "one evaluation per key");
    }
}