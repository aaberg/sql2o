package org.sql2o.tools;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
     * A null from evaluate is stored but never accepted on the fast path, so the next request evaluates again.
     */
    @Test
    public void aNullFromEvaluateIsNotCached() {
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
        assertEquals(2, calls.get());
    }

    /**
     * The Map constructor looks like an invitation to pass a concurrent map, but evaluate returning null then
     * blows up in the put. Nothing in sql2o hits it, since PojoIntrospector uses the default HashMap.
     */
    @Test
    public void aMapThatRejectsNullsBreaksOnANullFromEvaluate() {
        final AbstractCache<String, String, String> cache =
                new AbstractCache<String, String, String>(new ConcurrentHashMap<>()) {
                    @Override
                    protected String evaluate(String key, String param) {
                        return null;
                    }
                };

        assertThrows(NullPointerException.class, () -> cache.get("key", "p"));
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
     * Covers the recheck under the write lock: the second thread gets past the read lock while the cache is still
     * empty and then finds the value the first thread cached.
     */
    @Test
    public void aThreadThatWaitedForTheWriteLockReusesTheValueItFound() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final CountDownLatch insideEvaluate = new CountDownLatch(1);
        final CountDownLatch letEvaluateFinish = new CountDownLatch(1);
        final AbstractCache<String, String, String> cache =
                new AbstractCache<String, String, String>(new ConcurrentHashMap<>()) {
                    @Override
                    protected String evaluate(String key, String param) {
                        calls.incrementAndGet();
                        insideEvaluate.countDown();
                        try {
                            assertTrue(letEvaluateFinish.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(e);
                        }
                        return "value";
                    }
                };
        final ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            final Future<String> first = pool.submit(() -> cache.get("key", "p"));
            assertTrue(insideEvaluate.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

            final Future<String> second = pool.submit(() -> cache.get("key", "p"));
            // nothing is cached yet, so let the second thread finish its read and block on the write lock
            Thread.sleep(100);

            letEvaluateFinish.countDown();

            assertEquals("value", first.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            assertEquals("value", second.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, calls.get());
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