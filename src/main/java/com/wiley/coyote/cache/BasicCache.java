package com.wiley.coyote.cache;

/**
 * Implements an in-memory cache employing either the LRU (least recently used)
 * or the LFU (least frequently used) eviction strategy, depending on the
 * constructor arguments.
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
class BasicCache<K, V> extends AbstractCache<K, V> {

    /**
     * Cache capacity.
     */
    private final int MAX_SIZE;

    /**
     * Cache eviction strategy.
     */
    private final EvictionStrategy EVICTION_STRATEGY;

    /**
     * The cache data structure.
     */
    private final CacheDataStructure<K, V> dataStructure;

    /**
     * The cache stats companion object.
     */
    private final Stats stats;

    /**
     * The implementation of the {@link Stats} interface for this cache.
     */
    private class BasicStats extends AbstractStats {

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
            return EVICTION_STRATEGY;
        }
    }

    /**
     * Initializes the stats and the data structure.
     *
     * @param maxSize   the maximum number of elements kept
     */
    BasicCache(int maxSize, EvictionStrategy evictionStrategy) {
        if (maxSize <= 0) {
            throw new IllegalCacheParameterException(
                    String.format("Attempted to create cache with non-positive max size (%d)", maxSize)
            );
        }
        this.MAX_SIZE = maxSize;
        this.EVICTION_STRATEGY = evictionStrategy;
        switch (EVICTION_STRATEGY) {
            case LRU:
                this.dataStructure = new LRUCacheDataStructure<>(MAX_SIZE);
                break;
            case LFU:
                this.dataStructure = new LFUCacheDataStructure<>(MAX_SIZE);
                break;
            default:
                throw new IllegalCacheParameterException(
                        String.format(
                                "Attempted to create cache with unsupported eviction strategy (%s)",
                                evictionStrategy.toString()
                        )
                );
        }
        this.stats = new BasicStats();
    }

    @Override
    public V put(K key, V value) {
        V previousValue = null;

        if (dataStructure.contains(key)) {
            // Key already mapped; update value
            previousValue = dataStructure.get(key);
            dataStructure.replaceValue(key, value);
            registerUpdate();
        } else {
            // Key is not mapped; optionally evict and add new mapping
            if (dataStructure.getSize() >= MAX_SIZE) {
                dataStructure.evict();
                registerEviction();
            }
            dataStructure.add(key, value);
            registerInsertion();
        }

        return previousValue;
    }

    @Override
    public V get(K key) {
        V value = null;

        if (dataStructure.contains(key)) {
            value = dataStructure.get(key);
            if (value == null) {
                dataStructure.remove(key);
                registerNearHit();
            } else {
                dataStructure.elevate(key);
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
