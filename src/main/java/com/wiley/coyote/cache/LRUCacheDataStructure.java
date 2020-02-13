package com.wiley.coyote.cache;

import com.wiley.coyote.cache.LRUCacheDataStructureImplementation.LRUCacheEntry;

/**
 * The internal interface that summarizes the LRU cache data structure.
 * <p>
 * The data structure has the following abilities: add a new entry to the
 * front (it becomes the most recent); remove a known existing entry; remove
 * the least recently used entry; check the presence of an entry by its key;
 * retrieve an entry by its key.
 *
 * @param <K>   the key type
 * @param <V>   the value type
 */
interface LRUCacheDataStructure<K, V> {
    int getSize();

    void addFirst(LRUCacheEntry<K, V> newEntry);

    void remove(LRUCacheEntry<K, V> entry);

    void removeLast();

    boolean contains(K key);

    LRUCacheEntry<K, V> get(K key);
}
