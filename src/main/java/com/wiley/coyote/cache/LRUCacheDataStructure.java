package com.wiley.coyote.cache;

import com.wiley.coyote.cache.LRUCacheDataStructureImplementation.LRUCacheEntry;

public interface LRUCacheDataStructure<K, V> {
    int getSize();

    void addFirst(LRUCacheEntry<K, V> newEntry);

    void remove(LRUCacheEntry<K, V> entry);

    void removeLast();

    boolean contains(K key);

    LRUCacheEntry<K, V> get(K key);
}
