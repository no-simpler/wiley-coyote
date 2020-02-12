package com.wiley.coyote.cache;

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
        Cache<K, V> instance = null;
        validateCacheParameters();
        switch (this.evictionStrategy) {
            case LRU:
                instance = new LRUCache<>(maxSize);
                break;
            case LFU:
                instance = new LFUCache<>(maxSize);
                break;
        }
        return instance;
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
