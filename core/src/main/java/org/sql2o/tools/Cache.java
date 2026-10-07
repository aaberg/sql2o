package org.sql2o.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Cache<Key, Value> {

    /**
     * Stands in for a cached null, so that a delegate returning null is still only called once per key. It never
     * leaves this class: {@link #get} turns it back into null on the way out.
     */
    private static final Object NULL_VALUE = new Object();

    private final Map<Key, Value> cacheMap;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public Cache() {
        cacheMap = new HashMap<>();
    }

    public Value get(Key key, Callable<Value> valueDelegate) {
        Value value = cacheMap.get(key);

        if (value != null) {
            return unwrap(value);
        }

        // If not in cache, create a write lock, create and cache the object, and return it.
        lock.writeLock().lock();
        try {
            // Recheck state because another thread might have acquired write lock and changed state before we did
            value = cacheMap.get(key);
            if (value == null) {
                value = valueDelegate.call();
                cacheMap.put(key, value == null ? nullValue() : value);
            }

            return unwrap(value);

        } catch (Exception e) {
            throw new RuntimeException("Error while getting value from cache", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * The cast is safe because the only value this method ever produces is the private sentinel, and every read
     * of it goes through {@link #unwrap}, which compares by identity and hands the caller back a null instead.
     */
    @SuppressWarnings("unchecked")
    private static <Value> Value nullValue() {
        return (Value) NULL_VALUE;
    }

    private static <Value> Value unwrap(Value value) {
        return value == NULL_VALUE ? null : value;
    }
}
