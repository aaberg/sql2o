package org.sql2o.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * just inherit and implement evaluate
 * User: dimzon
 * Date: 4/6/14
 * Time: 10:35 PM
 */
public abstract class AbstractCache<K,V,E> {
    private final Map<K,V> map;
    private final ReentrantReadWriteLock.ReadLock rl;
    private final ReentrantReadWriteLock.WriteLock wl;
    /***
     * @param map - allows to define your own map implementation
     */
    public AbstractCache(Map<K, V> map) {
        this.map = map;
        ReentrantReadWriteLock rrwl = new ReentrantReadWriteLock();
        rl = rrwl.readLock();
        wl = rrwl.writeLock();
    }

    public AbstractCache(){
        this(new HashMap<K, V>());
    }

    private final ThreadLocal<Boolean> tl = new ThreadLocal<Boolean>(){
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    public V get(K key,E param){
        V value;
        if(tl.get()){
            // evaluate implementation can re-enter into the cache
            // current thread has exclusive cache lock
            value = map.get(key);
            if(value==null){
                value = evaluate(key, param);
                map.put(key,value);
            }
            return value;
        }

        // let's take read lock first
        rl.lock();
        try {
            value = map.get(key);
        } finally {
            rl.unlock();
        }
        if(value!=null) return value;
        wl.lock();
        tl.set(Boolean.TRUE);
        try {
            value = map.get(key);
            if(value==null){
                value = evaluate(key, param);
                map.put(key,value);
            }
        } finally {
            tl.set(Boolean.FALSE);
            wl.unlock();
        }
        return value;
    }

    protected abstract V evaluate(K key, E param);

}
