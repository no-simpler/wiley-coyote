package com.wiley.coyote.cache;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

/**
 * The LRU cache data structure that provides constant-time implementation of
 * the standard CRUD operations.
 * <p>
 * The implementation encapsulates key-value mappings in Entries. A doubly-
 * linked list of all Entries is maintained, ordered by last access time.
 * Whenever an entry is accessed it is moved to the front of the list.
 * <p>
 * A hash map is maintained that maps a key to its entry.
 *
 * @param <K>   the key type
 * @param <V>   the value type
 */
class LRUCacheDataStructure<K, V> implements CacheDataStructure<K, V> {

    private final boolean DEBUG_MODE = false;

    private final Map<K, LRUCacheEntry<K, V>> mapKeyToEntry;

    private LRUCacheEntry<K, V> newestEntry;

    private LRUCacheEntry<K, V> oldestEntry;

    private int numberOfEntries;

    private static class LRUCacheEntry<K, V> {
        private final K key;
        private SoftReference<V> value;
        private LRUCacheEntry<K, V> left;
        private LRUCacheEntry<K, V> right;

        private LRUCacheEntry(K key, V value) {
            this.key = key;
            this.value = new SoftReference<>(value);
            this.left = null;
            this.right = null;
        }

        private V getValue() {
            return value.get();
        }

        private void setValue(V value) {
            this.value = new SoftReference<>(value);
        }
    }

    LRUCacheDataStructure(int maxSize) {
        mapKeyToEntry = new HashMap<>(maxSize);
        newestEntry = null;
        oldestEntry = null;
        numberOfEntries = 0;
    }

    @Override
    public int getSize() {
        return numberOfEntries;
    }

    @Override
    public void add(K key, V value) {
        if (DEBUG_MODE) System.out.printf("Size %d. Adding key %s.%n", mapKeyToEntry.size(), key);
        ensureConsistency();
        ensureKeyIsAddable(key);

        LRUCacheEntry<K, V> newEntry = new LRUCacheEntry<>(key, value);

        if (numberOfEntries == 0) {
            newestEntry = newEntry;
            oldestEntry = newEntry;
        } else {
            LRUCacheEntry<K, V> temp = newestEntry;
            newestEntry = newEntry;
            newEntry.left = null;
            newEntry.right = temp;
            if (temp != null) temp.left = newEntry;
        }
        mapKeyToEntry.put(newEntry.key, newEntry);
        ++numberOfEntries;

        ensureContinuity();
    }

    @Override
    public void replaceValue(K key, V value) {
        if (DEBUG_MODE) System.out.printf("Size %d. Updating key %s.%n", mapKeyToEntry.size(), key);
        ensureConsistency();
        ensureKeyIsUpdatable(key);

        // Inject new value
        LRUCacheEntry<K, V> entry = mapKeyToEntry.get(key);
        entry.setValue(value);
    }

    @Override
    public void remove(K key) {
        if (DEBUG_MODE) System.out.printf("Size %d. Removing key %s.%n", mapKeyToEntry.size(), key);
        ensureConsistency();
        ensureKeyIsRemovable(key);

        LRUCacheEntry<K, V> entry = mapKeyToEntry.get(key);

        LRUCacheEntry<K, V> leftNeighbor = entry.left;
        LRUCacheEntry<K, V> rightNeighbor = entry.right;
        if (leftNeighbor != null) leftNeighbor.right = rightNeighbor;
        if (rightNeighbor != null) rightNeighbor.left = leftNeighbor;
        if (entry == newestEntry) newestEntry = rightNeighbor;
        if (entry == oldestEntry) oldestEntry = leftNeighbor;
        entry.left = null;
        entry.right = null;
        mapKeyToEntry.remove(entry.key);
        --numberOfEntries;

        ensureContinuity();
    }

    @Override
    public void elevate(K key) {
        if (DEBUG_MODE) System.out.printf("Size %d. Elevating key %s.%n", mapKeyToEntry.size(), key);
        ensureConsistency();
        ensureKeyIsElevatable(key);
        if (numberOfEntries == 1) return;

        LRUCacheEntry<K, V> entry = mapKeyToEntry.get(key);

        // Remove entry from the list and re-stitch the list
        LRUCacheEntry<K, V> leftNeighbor = entry.left;
        LRUCacheEntry<K, V> rightNeighbor = entry.right;
        if (leftNeighbor != null) leftNeighbor.right = rightNeighbor;
        if (rightNeighbor != null) rightNeighbor.left = leftNeighbor;
        if (entry == newestEntry) newestEntry = rightNeighbor;
        if (entry == oldestEntry) oldestEntry = leftNeighbor;

        // Put entry to the top of the list
        LRUCacheEntry<K, V> temp = newestEntry;
        newestEntry = entry;
        entry.left = null;
        entry.right = temp;
        if (temp != null) temp.left = entry;

        ensureContinuity();
    }

    @Override
    public void evict() {
        remove(oldestEntry.key);
    }

    @Override
    public boolean contains(K key) {
        return mapKeyToEntry.containsKey(key);
    }

    @Override
    public V get(K key) {
        V value = null;
        if (contains(key)) {
            LRUCacheEntry<K, V> entry = mapKeyToEntry.get(key);
            if (entry == null)
                throw new CacheLogicException("DS contains null entry despite not allowing it");
            value = entry.getValue();
        }
        return value;
    }

    private void ensureKeyIsAddable(K key) {
        if (mapKeyToEntry.containsKey(key))
            throw new CacheLogicException("Attempted to add key that is already recorded in DS");
    }

    private void ensureKeyIsUpdatable(K key) {
        if (mapKeyToEntry.size() == 0)
            throw new CacheLogicException("Attempted to update in zero-size DS");
        if (!mapKeyToEntry.containsKey(key))
            throw new CacheLogicException("Attempted to update entry that is not recorded in DS");
        LRUCacheEntry<K, V> entry = mapKeyToEntry.get(key);
        if (entry == null)
            throw new CacheLogicException("Attempted to update entry that is mapped to null in DS");
    }

    private void ensureKeyIsRemovable(K key) {
        if (mapKeyToEntry.size() == 0)
            throw new CacheLogicException("Attempted to remove from zero-size DS");
        if (!mapKeyToEntry.containsKey(key))
            throw new CacheLogicException("Attempted to remove entry that is not recorded in DS");
        LRUCacheEntry<K, V> entry = mapKeyToEntry.get(key);
        if (entry == null)
            throw new CacheLogicException("Attempted to remove entry that is mapped to null in DS");
    }

    private void ensureKeyIsElevatable(K key) {
        if (mapKeyToEntry.size() == 0)
            throw new CacheLogicException("Attempted to elevate entry in zero-size DS");
        if (!mapKeyToEntry.containsKey(key))
            throw new CacheLogicException("Attempted to elevate entry that is not recorded in DS");
        LRUCacheEntry<K, V> entry = mapKeyToEntry.get(key);
        if (entry == null)
            throw new CacheLogicException("Attempted to elevate entry that is mapped to null in DS");
    }

    private void ensureConsistency() {
        if (numberOfEntries < 0)
            throw new CacheLogicException("Size of DS is negative");
        if (numberOfEntries != mapKeyToEntry.size())
            throw new CacheLogicException("Sizes of map and DLL within DS do not match");
        if (numberOfEntries > 0 && (newestEntry == null || oldestEntry == null))
            throw new CacheLogicException("Non-empty DS has null head/tail");
        if (numberOfEntries > 0 && (newestEntry.left != null || oldestEntry.right != null))
            throw new CacheLogicException("Non-empty DS has non-terminal head/tail");
    }

    private void ensureContinuity() {
        if (!DEBUG_MODE) return;
        LRUCacheEntry<K, V> temp = newestEntry;
        int counter = numberOfEntries + 100;
        int size = 0;
        if (temp != null) {
            ++size;
            while (temp.right != null && counter > 0) {
                temp = temp.right;
                ++size;
                --counter;
            }
            if (counter == 0)
                throw new CacheLogicException("DLL within non-empty DS likely contains loops");
            if (! temp.equals(oldestEntry))
                throw new CacheLogicException("DLL within non-empty DS is not continuous");
            if (size < numberOfEntries)
                throw new CacheLogicException(String.format(
                        "DLL within non-empty DS is shorter than recorded (%d < %d)", size, numberOfEntries
                ));
            if (size > numberOfEntries)
                throw new CacheLogicException(String.format(
                        "DLL within non-empty DS is longer than recorded (%d > %d)", size, numberOfEntries
                ));
        }
    }
}