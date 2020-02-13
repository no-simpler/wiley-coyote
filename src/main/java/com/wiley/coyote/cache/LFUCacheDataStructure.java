package com.wiley.coyote.cache;

/**
 * The internal interface that summarizes the LFU cache data structure.
 *
 * @param <K>   the key type
 * @param <V>   the value type
 */
interface LFUCacheDataStructure<K, V> {

    /**
     * Returns the current number of mappings in the data structure.
     *
     * @return  the current count of mappings
     */
    int getSize();

    /**
     * Adds a fresh key-value mapping into the data structure; the key must not
     * be not present.
     *
     * @param key   the key not currently mapped in the data structure
     * @param value the value to map to that key
     */
    void addFresh(K key, V value);

    /**
     * Injects the new value into the key-value mapping in the data structure;
     * the key must by present.
     *
     * @param key   the key currently mapped in the data structure
     * @param value the value to inject
     */
    void replaceValue(K key, V value);

    /**
     * Removes the mapping associated with the given key from the data
     * structure; the key must be present.
     *
     * @param key   the key currently mapped in the data structure
     */
    void remove(K key);

    /**
     * Increment the frequency of the mapping associated with the given key in
     * the data structure; the key must be present.
     *
     * @param key   the key currently mapped in the data structure
     */
    void incrementFrequency(K key);

    /**
     * Removes one of the least frequent mappings from the data structure; the
     * data structure must not be empty.
     */
    void removeLeastFrequent();

    /**
     * Indicates whether a mapping associated with the given key is present in
     * the data structure.
     *
     * @param key   the key to check
     * @return  whether a mapping exists
     */
    boolean contains(K key);

    /**
     * Returns the value associated with the given key from the data structure;
     * the key must be present.
     *
     * @param key   the key currently mapped in the data structure
     * @return  the retrieved value
     */
    V get(K key);
}
