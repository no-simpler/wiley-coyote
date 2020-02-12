package com.wiley.coyote.cache;

import com.wiley.coyote.cache.LFUCacheDataStructureImplementation.LFUCacheEntry;

class LFUCache<K, V> extends AbstractCache<K, V> {

    private final int MAX_SIZE;

    private final LFUCacheDataStructure<K, V> ds;

    private final Stats stats;

    private class LFUStats extends AbstractStats {

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
            return EvictionStrategy.LFU;
        }
    }

    LFUCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalCacheParameterException(
                    String.format("Attempted to create cache with non-positive max size (%d)", maxSize)
            );
        }
        this.MAX_SIZE = maxSize;
        this.ds = new LFUCacheDataStructureImplementation<>(MAX_SIZE);
        this.stats = new LFUStats();
    }

    @Override
    public V put(K key, V value) {
        V previousValue = null;

        if (ds.contains(key)) {
            LFUCacheEntry<K, V> entry = ds.get(key);
            previousValue = entry.getValue();

            entry.setValue(value);

            ds.incrementFrequency(entry);
            registerUpdate();
        } else {
            if (ds.getSize() >= MAX_SIZE) {
                ds.removeLeastFrequent();
                registerEviction();
            }
            ds.add(new LFUCacheEntry<>(key, value));
            registerInsertion();
        }

        return previousValue;
    }

    @Override
    public V get(K key) {
        V value = null;

        if (ds.contains(key)) {
            LFUCacheEntry<K, V> entry = ds.get(key);
            value = entry.getValue();

            if (value == null) {
                ds.remove(entry);
                registerNearHit();
            } else {
                ds.incrementFrequency(entry);
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
