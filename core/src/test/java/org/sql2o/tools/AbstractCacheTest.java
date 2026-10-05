package org.sql2o.tools;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link AbstractCache}.
 *
 * <p>Unlike {@link Cache}, this one takes the read lock before the first lookup, so a value published by
 * another thread is visible to the fast path. What still has to hold is that the delegate runs once per key.
 */
public class AbstractCacheTest {

    private static final int THREADS = 4;
    private static final long TIMEOUT_SECONDS = 20;

    private static class CountingCache extends AbstractCache<String, String, String> {

        private final AtomicInteger calls = new AtomicInteger();

        private CountingCache() {
            super();
        }

        private CountingCache(Map<String, String> map) {
            super(map);
        }

        @Override
        protected String evaluate(String key, String param) {
            calls.incrementAndGet();
            return "value of " + key + " with " + param;
        }
    }

    @Test
    public void evaluateProvidesTheValueAndIsNotAskedAgain() {
        final CountingCache cache = new CountingCache();

        assertEquals("value of key with param", cache.get("key", "param"));
        assertEquals("value of key with param", cache.get("key", "other param"));
        assertEquals(1, cache.calls.get());
    }

    @Test
    public void theParamIsHandedToEvaluate() {
        final CountingCache cache = new CountingCache();

        assertEquals("value of key with first", cache.get("key", "first"));
    }

    @Test
    public void keysAreIndependent() {
        final CountingCache cache = new CountingCache();

        assertEquals("value of a with p", cache.get("a", "p"));
        assertEquals("value of b with p", cache.get("b", "p"));
        assertEquals(2, cache.calls.get());
    }

    /**
     * A null from evaluate has to be cached, or every request for that key pays for another evaluation and the
     * "delegate runs once per key" guarantee quietly stops holding.
     */
    @Test
    public void aNullFromEvaluateIsCachedAndStillReportedAsNull() {
        final AtomicInteger calls = new AtomicInteger();
        final AbstractCache<String, String, String> cache = new AbstractCache<String, String, String>() {
            @Override
            protected String evaluate(String key, String param) {
                calls.incrementAndGet();
                return null;
            }
        };

        assertNull(cache.get("key", "p"));
        assertNull(cache.get("key", "p"));
        assertEquals(1, calls.get());
    }

    /**
     * The Map constructor looks like an invitation to pass a concurrent map, which refuses null values, so
     * caching a null must not go through a plain put.
     */
    @Test
    public void aNullIsCacheableInAMapThatRejectsNulls() {
        final AbstractCache<String, String, String> cache =
                new AbstractCache<String, String, String>(new ConcurrentHashMap<>()) {
                    @Override
                    protected String evaluate(String key, String param) {
                        return null;
                    }
                };

        assertNull(cache.get("key", "p"));
        assertNull(cache.get("key", "p"));
    }

    @Test
    public void anInjectedConcurrentMapIsUsed() {
        final CountingCache cache = new CountingCache(new ConcurrentHashMap<>());

        assertEquals("value of key with param", cache.get("key", "param"));
        assertEquals(1, cache.calls.get());
    }

    @Test
    public void aValueIsEvaluatedOnceEvenWhenEveryoneAsksAtTheSameTime() throws Exception {
        final CountingCache cache = new CountingCache();
        final ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            final CountDownLatch startTogether = new CountDownLatch(1);
            final List<Future<String>> results = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                results.add(pool.submit(() -> {
                    startTogether.await();
                    return cache.get("key", "param");
                }));
            }

            startTogether.countDown();
            for (final Future<String> result : results) {
                assertEquals("value of key with param", result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, cache.calls.get());
    }

    /**
     * Covers the recheck under the write lock: a value published after the read lock section must be found there,
     * and evaluate must not run a second time.
     *
     * <p>This cannot be staged with two threads. The first lookup happens under the read lock, and whoever holds
     * the write lock blocks readers, so a second thread can only ever read null before the value exists and again
     * after it does - it never gets to the recheck with a value in place. The map below publishes the value during
     * the first lookup instead, which is the interleaving the recheck exists for, without the race.
     */
    @Test
    public void aValuePublishedAfterTheReadLockSectionIsFoundByTheRecheck() {
        final AtomicInteger calls = new AtomicInteger();
        final AtomicInteger lookups = new AtomicInteger();
        final Map<String, String> map = new HashMap<String, String>() {
            @Override
            public String get(Object key) {
                if (lookups.getAndIncrement() == 0) {
                    super.put((String) key, "value");
                    return null;
                }
                return super.get(key);
            }
        };

        final AbstractCache<String, String, String> cache = new AbstractCache<String, String, String>(map) {
            @Override
            protected String evaluate(String key, String param) {
                calls.incrementAndGet();
                return "from evaluate";
            }
        };

        assertEquals("value", cache.get("key", "p"));
        assertEquals(0, calls.get(), "the recheck must find the value instead of evaluating again");
    }

    @Test
    public void parallelWorkOnManyKeysStaysConsistent() throws Exception {
        final int keyCount = 300;
        final CountingCache cache = new CountingCache(new ConcurrentHashMap<>());
        final ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            final List<Future<List<String>>> rounds = new ArrayList<>();
            for (int t = 0; t < THREADS; t++) {
                rounds.add(pool.submit(() -> {
                    final List<String> seen = new ArrayList<>();
                    for (int key = 0; key < keyCount; key++) {
                        seen.add(cache.get("key" + key, "p"));
                    }
                    return seen;
                }));
            }

            for (final Future<List<String>> round : rounds) {
                final List<String> seen = round.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                for (int key = 0; key < keyCount; key++) {
                    assertEquals("value of key" + key + " with p", seen.get(key));
                }
            }
        } finally {
            pool.shutdownNow();
        }

        for (int key = 0; key < keyCount; key++) {
            final String keyName = "key" + key;
            assertEquals("value of " + keyName + " with p", cache.get(keyName, "p"));
        }
        assertEquals(keyCount, cache.calls.get(), "the write lock must keep every key to a single evaluation");
    }
}