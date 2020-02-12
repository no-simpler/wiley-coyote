package com.wiley.coyote.cache;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

class LRUCacheDataStructureImplementation<K, V> implements LRUCacheDataStructure<K, V> {

    private final boolean DEBUG_MODE = false;

    private final Map<K, LRUCacheEntry<K, V>> map;

    private LRUCacheEntry<K, V> head;

    private LRUCacheEntry<K, V> tail;

    private int size;

    static class LRUCacheEntry<K, V> {
        private final K key;
        private final SoftReference<V> value;
        private LRUCacheEntry<K, V> left;
        private LRUCacheEntry<K, V> right;

        LRUCacheEntry(K key, V value) {
            this.key = key;
            this.value = new SoftReference<>(value);
            this.left = null;
            this.right = null;
        }

        V getValue() {
            return value.get();
        }
    }

    LRUCacheDataStructureImplementation(int maxSize) {
        map = new HashMap<>(maxSize);
        head = null;
        tail = null;
        size = 0;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public void addFirst(LRUCacheEntry<K, V> newEntry) {
        ensureConsistency();
        if (newEntry == null)
            throw new CacheLogicException("Attempted to add null entry to DS");
        if (map.containsKey(newEntry.key))
            throw new CacheLogicException("Attempted to add entry that is already recorded in DS");
        if (size == 0) {
            head = newEntry;
            tail = newEntry;
        } else {
            LRUCacheEntry<K, V> temp = head;
            head = newEntry;
            newEntry.left = null;
            newEntry.right = temp;
            if (temp != null) temp.left = newEntry;
        }
        map.put(newEntry.key, newEntry);
        ++size;
        ensureContinuity();
    }

    @Override
    public void remove(LRUCacheEntry<K, V> entry) {
        ensureConsistency();
        if (entry == null)
            throw new CacheLogicException("Attempted to remove null entry from DS");
        if (size == 0)
            throw new CacheLogicException("Attempted to remove from zero-size DS");
        if (!map.containsKey(entry.key))
            throw new CacheLogicException("Attempted to remove entry that is not recorded in DS");
        LRUCacheEntry<K, V> leftNeighbor = entry.left;
        LRUCacheEntry<K, V> rightNeighbor = entry.right;
        if (leftNeighbor != null) leftNeighbor.right = rightNeighbor;
        if (rightNeighbor != null) rightNeighbor.left = leftNeighbor;
        if (entry == head) head = rightNeighbor;
        if (entry == tail) tail = leftNeighbor;
        entry.left = null;
        entry.right = null;
        map.remove(entry.key);
        --size;
        if (map.size() != size)
            throw new CacheLogicException("Mismatched DS size after removal of entry");
        ensureContinuity();
    }

    @Override
    public void removeLast() {
        remove(tail);
    }

    @Override
    public boolean contains(K key) {
        ensureConsistency();
        ensureContinuity();
        if (key == null)
            throw new CacheLogicException("Attempted to check key in DS by null key");
        return map.containsKey(key);
    }

    @Override
    public LRUCacheEntry<K, V> get(K key) {
        LRUCacheEntry<K, V> entry = null;
        if (contains(key)) {
            entry = map.get(key);
            if (entry == null)
                throw new CacheLogicException("DS contains null entry despite not allowing it");
        }
        return entry;
    }

    private void ensureConsistency() {
        if (size < 0)
            throw new CacheLogicException("Size of DS is negative");
        if (size != map.size())
            throw new CacheLogicException("Sizes of map and DLL within DS do not match");
        if (size > 0 && (head == null || tail == null))
            throw new CacheLogicException("Non-empty DS has null head/tail");
        if (size > 0 && (head.left != null || tail.right != null))
            throw new CacheLogicException("Non-empty DS has non-terminal head/tail");
    }

    private void ensureContinuity() {
        if (!DEBUG_MODE) return;
        LRUCacheEntry<K, V> temp = head;
        int counter = size + 1;
        if (temp != null) {
            while (temp.right != null && counter > 0) {
                temp = temp.right;
                --counter;
            }
            if (counter == 0)
                throw new CacheLogicException("DLL within non-empty DS likely contains loops");
            if (! temp.equals(tail))
                throw new CacheLogicException("DLL within non-empty DS is not continuous between head and tail");
        }
    }
}