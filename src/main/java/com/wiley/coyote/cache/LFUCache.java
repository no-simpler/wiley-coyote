package com.wiley.coyote.cache;

import com.wiley.coyote.cache.LFUCacheDataStructureImplementation.LFUCacheEntry;

/**
 * Implements an in-memory cache employing the LFU (least frequently used)
 * eviction strategy.
 * <p>
 * The cache accepts both null keys and null values. The values are stored as
 * soft references, which allows them to be garbage collected whenever the JVM
 * runs out of memory, unless the user retains a regular reference to the
 * stored object.
 * <p>
 * This implementation is not synchronized.
 *
 * @param <K>   the key type
 * @param <V>   the value type
 */
class LFUCache<K, V> extends AbstractCache<K, V> {

    /**
     * Cache capacity.
     */
    private final int MAX_SIZE;

    /**
     * The LFU data structure.
     */
    private final LFUCacheDataStructure<K, V> dataStructure;

    /**
     * The cache stats companion object.
     */
    private final Stats stats;

    /**
     * The implementation of the {@link com.wiley.coyote.cache.Cache.Stats}
     * interface for the LFU cache.
     */
    private class LFUStats extends AbstractStats {

        @Override
        public int getSize() {
            return dataStructure.getSize();
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

    /**
     * Initializes the stats and the data structure.
     *
     * @param maxSize   the maximum number of elements kept
     */
    LFUCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalCacheParameterException(
                    String.format("Attempted to create cache with non-positive max size (%d)", maxSize)
            );
        }
        this.MAX_SIZE = maxSize;
        this.dataStructure = new LFUCacheDataStructureImplementation<>(MAX_SIZE);
        this.stats = new LFUStats();
    }

    @Override
    public V put(K key, V value) {
        V previousValue = null;

        if (dataStructure.contains(key)) {
            LFUCacheEntry<K, V> entry = dataStructure.get(key);
            previousValue = entry.getValue();

            entry.setValue(value);

            dataStructure.incrementFrequency(entry);
            registerUpdate();
        } else {
            if (dataStructure.getSize() >= MAX_SIZE) {
                dataStructure.removeLeastFrequent();
                registerEviction();
            }
            dataStructure.add(new LFUCacheEntry<>(key, value));
            registerInsertion();
        }

        return previousValue;
    }

    @Override
    public V get(K key) {
        V value = null;

        if (dataStructure.contains(key)) {
            LFUCacheEntry<K, V> entry = dataStructure.get(key);
            value = entry.getValue();

            if (value == null) {
                dataStructure.remove(entry);
                registerNearHit();
            } else {
                dataStructure.incrementFrequency(entry);
                registerHit();
            }
        } else {
            registerMiss();
        }

        return value;
    }

    @Override
    public boolean containsKey(K key) {
        return dataStructure.contains(key);
    }

    @Override
    public Stats stats() {
        return this.stats;
    }
}
