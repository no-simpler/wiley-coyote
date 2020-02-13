package com.wiley.coyote.cache;

import com.wiley.coyote.cache.LRUCacheDataStructureImplementation.LRUCacheEntry;

/**
 * Implements an in-memory cache employing the LRU (least recently used)
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
class LRUCache<K, V> extends AbstractCache<K, V> {

    /**
     * Cache capacity.
     */
    private final int MAX_SIZE;

    /**
     * The LRU data structure.
     */
    private final LRUCacheDataStructure<K, V> dataStructure;

    /**
     * The cache stats companion object.
     */
    private final Stats stats;

    /**
     * The implementation of the {@link com.wiley.coyote.cache.Cache.Stats}
     * interface for the LRU cache.
     */
    private class LRUStats extends AbstractStats {

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
            return EvictionStrategy.LRU;
        }
    }

    /**
     * Initializes the stats and the data structure.
     *
     * @param maxSize   the maximum number of elements kept
     */
    LRUCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalCacheParameterException(
                    String.format("Attempted to create cache with non-positive max size (%d)", maxSize)
            );
        }
        this.MAX_SIZE = maxSize;
        this.dataStructure = new LRUCacheDataStructureImplementation<>(MAX_SIZE);
        this.stats = new LRUStats();
    }

    @Override
    public V put(K key, V value) {
        V previousValue = null;

        if (dataStructure.contains(key)) {
            LRUCacheEntry<K, V> existingEntry = dataStructure.get(key);
            previousValue = existingEntry.getValue();

            LRUCacheEntry<K, V> updatedEntry = new LRUCacheEntry<>(key, value);

            dataStructure.remove(existingEntry);
            dataStructure.addFirst(updatedEntry);
            registerUpdate();
        } else {
            if (dataStructure.getSize() >= MAX_SIZE) {
                dataStructure.removeLast();
                registerEviction();
            }
            LRUCacheEntry<K, V> newEntry = new LRUCacheEntry<>(key, value);
            dataStructure.addFirst(newEntry);
            registerInsertion();
        }

        return previousValue;
    }

    @Override
    public V get(K key) {
        V value = null;
        if (dataStructure.contains(key)) {
            LRUCacheEntry<K, V> existingEntry = dataStructure.get(key);
            value = existingEntry.getValue();

            dataStructure.remove(existingEntry);

            if (value == null) {
                registerNearHit();
            } else {
                dataStructure.addFirst(existingEntry);
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
