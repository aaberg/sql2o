package org.sql2o.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Cache<Key, Value> {
    private final Map<Key, Value> cacheMap;
    private final ReadWriteLock lock;

    public Cache() {
        cacheMap = new HashMap<>();
        lock = new ReentrantReadWriteLock();
    }

    public Value get(Key key, Callable<Value> valueDelegate) {
        Value value;

        // Check readlock first
        lock.readLock().lock();
        try {
            value = cacheMap.get(key);
            if (value != null) {
                return value;
            }
        } finally {
            lock.readLock().unlock();
        }

        // If not in cache, create a write lock, create and cache the object, and return it.
        lock.writeLock().lock();
        try {
            // Recheck state because another thread might have acquired write lock and changed state before we did
            value = cacheMap.get(key);
            if (value == null) {
                value = valueDelegate.call();
                cacheMap.put(key, value);
            }

            return value;

        } catch (Exception e) {
            throw new RuntimeException("Error while getting value from cache", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
