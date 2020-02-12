package com.wiley.coyote.cache;

import com.wiley.coyote.cache.LFUCacheDataStructureImplementation.LFUCacheEntry;

public interface LFUCacheDataStructure<K, V> {
    int getSize();

    void add(LFUCacheEntry<K, V> entry);

    void remove(LFUCacheEntry<K, V> entry);

    void incrementFrequency(LFUCacheEntry<K, V> entry);

    void removeLeastFrequent();

    boolean contains(K key);

    LFUCacheEntry<K, V> get(K key);
}
