package com.wiley.coyote.cache;

import com.wiley.coyote.cache.LRUCacheDataStructureImplementation.LRUCacheEntry;

public class LRUCache<K, V> extends AbstractCache<K, V> {

    private final int MAX_SIZE;

    private final LRUCacheDataStructure<K, V> ds;

    private final Stats stats;

    private class LRUStats extends AbstractStats {

        @Override
        public int getSize() {
            return ds.getSize();
        }

        @Override
        public int getMaxSize() {
            return MAX_SIZE;
        }

        @Override
        public EvictionStrategy getEvictionStrategy() {
            return EvictionStrategy.LRU;
        }
    }

    public LRUCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalCacheParameterException(
                    String.format("Attempted to create cache with non-positive max size (%d)", maxSize)
            );
        }
        this.MAX_SIZE = maxSize;
        this.ds = new LRUCacheDataStructureImplementation<>(MAX_SIZE);
        this.stats = new LRUStats();
    }

    @Override
    public V put(K key, V value) {
        V previousValue = null;

        if (ds.contains(key)) {
            LRUCacheEntry<K, V> existingEntry = ds.get(key);
            previousValue = existingEntry.getValue();

            LRUCacheEntry<K, V> updatedEntry = new LRUCacheEntry<>(key, value);

            ds.remove(existingEntry);
            ds.addFirst(updatedEntry);
            registerUpdate();
        } else {
            if (ds.getSize() >= MAX_SIZE) {
                ds.removeLast();
                registerEviction();
            }
            LRUCacheEntry<K, V> newEntry = new LRUCacheEntry<>(key, value);
            ds.addFirst(newEntry);
            registerInsertion();
        }

        return previousValue;
    }

    @Override
    public V get(K key) {
        V value = null;
        if (ds.contains(key)) {
            LRUCacheEntry<K, V> existingEntry = ds.get(key);
            value = existingEntry.getValue();

            ds.remove(existingEntry);

            if (value == null) {
                registerNearHit();
            } else {
                ds.addFirst(existingEntry);
                registerHit();
            }
        } else {
            registerMiss();
        }
        return value;
    }

    @Override
    public boolean containsKey(K key) {
        return ds.contains(key);
    }

    @Override
    public Stats stats() {
        return this.stats;
    }
}
