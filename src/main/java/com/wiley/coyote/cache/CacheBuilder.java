package com.wiley.coyote.cache;

/**
 * The mutable builder of the various implementations of the {@link Cache}
 * interface.
 *
 * @param <K>   the key type
 * @param <V>   the value type
 */
public class CacheBuilder<K, V> {

    private Cache.EvictionStrategy evictionStrategy = Cache.DEFAULT_EVICTION_STRATEGY;

    private int maxSize = Cache.DEFAULT_MAX_SIZE;

    public CacheBuilder() {}

    CacheBuilder(Class<K> keyClazz, Class<V> valueClazz) {}

    public CacheBuilder<K, V> setEvictionStrategy(Cache.EvictionStrategy evictionStrategy) {
        this.evictionStrategy = evictionStrategy;
        return this;
    }

    public CacheBuilder<K, V> setMaxSize(int maxSize) {
        this.maxSize = maxSize;
        return this;
    }

    public Cache<K, V> build() {
        validateCacheParameters();
        return new BasicCache<>(maxSize, evictionStrategy);
    }

    private void validateCacheParameters() {
        if (evictionStrategy == null) {
            throw new IllegalCacheParameterException(
                    "Cache eviction strategy must not be null"
            );
        }
        if (maxSize == 0) {
            throw new IllegalCacheParameterException(
                    "Cache size limit must not be zero"
            );
        } else if (maxSize < 0) {
            throw new IllegalCacheParameterException(
                    "Cache size limit must not be negative"
            );
        }
    }
}
