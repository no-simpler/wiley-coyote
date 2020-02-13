package com.wiley.coyote.cache;

import com.wiley.coyote.cache.LFUCacheDataStructureImplementation.LFUCacheEntry;

/**
 * The internal interface that summarizes the LFU cache data structure.
 * <p>
 * The data structure has the following abilities: add a new entry; remove a
 * known existing entry; remove the entry with the least frequency; increment
 * the frequency of an existing entry by one; check the presence of an entry by
 * its key; retrieve an entry by its key.
 *
 * @param <K>   the key type
 * @param <V>   the value type
 */
interface LFUCacheDataStructure<K, V> {
    int getSize();

    void add(LFUCacheEntry<K, V> entry);

    void remove(LFUCacheEntry<K, V> entry);

    void incrementFrequency(LFUCacheEntry<K, V> entry);

    void removeLeastFrequent();

    boolean contains(K key);

    LFUCacheEntry<K, V> get(K key);
}
