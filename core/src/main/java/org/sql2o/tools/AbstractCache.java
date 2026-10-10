package org.sql2o.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * just inherit and implement evaluate
 * User: dimzon
 * Date: 4/6/14
 * Time: 10:35 PM
 */
public abstract class AbstractCache<K,V,E> {

    /**
     * Stands in for a cached null. A map is free to refuse null values (ConcurrentHashMap throws), and a caller
     * cannot tell an absent entry from a cached one, so nulls are stored as this instance and handed back as null
     * by {@link #get}. It never leaves this class.
     */
    private static final Object NULL_VALUE = new Object();

    private final Map<K,V> map;
    private final Lock rl;
    private final Lock wl;
    /***
     * @param map - allows to define your own map implementation
     */
    public AbstractCache(Map<K, V> map) {
        this.map = map;
        ReadWriteLock rrwl = new ReentrantReadWriteLock();
        rl = rrwl.readLock();
        wl = rrwl.writeLock();
    }

    public AbstractCache(){
        this(new HashMap<K, V>());
    }

    public V get(K key,E param){
        V value;

        try {
            // let's take read lock first
            rl.lock();
            value = map.get(key);
        } finally {
            rl.unlock();
        }
        if(value!=null) return unwrap(value);

        try {
            wl.lock();
            value = map.get(key);
            if(value==null){
                value = evaluate(key, param);
                map.put(key, value==null ? nullValue() : value);
            }
        } finally {
            wl.unlock();
        }
        return unwrap(value);
    }

    protected abstract V evaluate(K key, E param);

    /**
     * The cast is safe because the only value this method ever produces is the private sentinel, and every read
     * of it goes through {@link #unwrap}, which compares by identity and hands the caller back a null instead.
     */
    @SuppressWarnings("unchecked")
    private static <V> V nullValue() {
        return (V) NULL_VALUE;
    }

    private static <V> V unwrap(V value) {
        return value==NULL_VALUE ? null : value;
    }
}
