package com.wiley.coyote.cache;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * The LFU cache data structure that provides constant-time implementation of
 * the standard CRUD operations.
 * <p>
 * The implementation encapsulates key-value mappings in Entries, which also
 * contain a frequency node. A doubly-linked list of all frequency nodes is
 * maintained, ordered by frequency value. Whenever a frequency becomes unused,
 * its node is removed from the list and all related maps.
 * <p>
 * Three hash maps are maintained:
 * <p><ul>
 * <li>A hash map that maps a key to its entry.</li>
 * <li>A hash map that maps a frequency value to a list of keys that use that
 * frequency.</li>
 * <li>A hash map that maps a frequency value to its frequency node.</li>
 * </ul>
 *
 * @param <K>   the key type
 * @param <V>   the value type
 */
class LFUCacheDataStructure<K, V> implements CacheDataStructure<K, V> {

    private final boolean DEBUG_MODE = false;

    private final Map<K, LFUCacheEntry<K, V>> mapKeyToEntry;

    private final Map<Long, LinkedHashSet<K>> mapFrequencyToKeys;

    private final Map<Long, LFUCacheEntryFrequency> mapFrequencyToNode;

    private LFUCacheEntryFrequency minFrequency;

    private LFUCacheEntryFrequency maxFrequency;

    private int numberOfFrequencies;

    private static class LFUCacheEntry<K, V> {
        private final K key;
        private SoftReference<V> value;
        private LFUCacheEntryFrequency frequency;

        private LFUCacheEntry(K key, V value) {
            this.key = key;
            this.value = new SoftReference<>(value);
            this.frequency = null;
        }

        private void setValue(V value) {
            this.value = new SoftReference<>(value);
        }

        private V getValue() {
            return value.get();
        }

        private void setFrequency(LFUCacheEntryFrequency frequency) {
            this.frequency = frequency;
        }
    }

    private static class LFUCacheEntryFrequency {
        private final long value;
        private LFUCacheEntryFrequency left;
        private LFUCacheEntryFrequency right;

        private LFUCacheEntryFrequency() {
            this.value = 1L;
            this.left = null;
            this.right = null;
        }

        private LFUCacheEntryFrequency(long value) {
            if (value < 1)
                throw new CacheLogicException("Attempted to create frequency smaller than 1");
            this.value = value;
            this.left = null;
            this.right = null;
        }
    }

    LFUCacheDataStructure(int maxSize) {
        mapKeyToEntry = new HashMap<>(maxSize);
        mapFrequencyToKeys = new HashMap<>();
        mapFrequencyToNode = new HashMap<>();
        maxFrequency = null;
        minFrequency = null;
        numberOfFrequencies = 0;
    }

    @Override
    public int getSize() {
        return mapKeyToEntry.size();
    }

    @Override
    public void add(K key, V value) {
        if (DEBUG_MODE)
            System.out.printf("Size %d (%df). Adding key %s.%n", mapKeyToEntry.size(), numberOfFrequencies, key);
        ensureConsistency();
        ensureKeyIsAddable(key);

        LFUCacheEntry<K, V> newEntry = new LFUCacheEntry<>(key, value);

        // Diverge based on presence of other entries
        if (numberOfFrequencies == 0) {
            // Add the very first entry to this data structure
            newEntry.setFrequency(new LFUCacheEntryFrequency());

            // Set min and max frequencies
            minFrequency = newEntry.frequency;
            maxFrequency = newEntry.frequency;

            // Ensure frequency node is tracked
            mapFrequencyToNode.put(newEntry.frequency.value, newEntry.frequency);
            ++numberOfFrequencies;
        } else if (mapFrequencyToNode.containsKey(1L)) {
            // Add to pre-existing entries with frequency 1
            newEntry.setFrequency(mapFrequencyToNode.get(1L));
        } else {
            // Add as first entry with frequency 1 (there are others)
            newEntry.setFrequency(new LFUCacheEntryFrequency());

            // New frequency becomes new min frequency (max frequency unaffected)
            LFUCacheEntryFrequency temp = minFrequency;
            minFrequency = newEntry.frequency;
            minFrequency.right = temp;
            minFrequency.left = null;
            temp.left = minFrequency;

            // Ensure frequency node is tracked
            mapFrequencyToNode.put(newEntry.frequency.value, newEntry.frequency);
            ++numberOfFrequencies;
        }

        // Add new key, creating fresh sets as necessary
        if (!mapFrequencyToKeys.containsKey(newEntry.frequency.value)) {
            mapFrequencyToKeys.put(newEntry.frequency.value, new LinkedHashSet<>());
        }
        mapFrequencyToKeys.get(newEntry.frequency.value).add(newEntry.key);

        // Track entry itself
        mapKeyToEntry.put(newEntry.key, newEntry);
        ensureContinuity();
    }

    @Override
    public void replaceValue(K key, V value) {
        if (DEBUG_MODE)
            System.out.printf("Size %d (%df). Updating key %s.%n", mapKeyToEntry.size(), numberOfFrequencies, key);
        ensureConsistency();
        ensureKeyIsUpdatable(key);

        // Inject new value
        LFUCacheEntry<K, V> entry = mapKeyToEntry.get(key);
        entry.setValue(value);
    }

    @Override
    public void remove(K key) {
        if (DEBUG_MODE)
            System.out.printf("Size %d (%df). Removing key %s.%n", mapKeyToEntry.size(), numberOfFrequencies, key);
        ensureConsistency();
        ensureKeyIsRemovable(key);

        LFUCacheEntry<K, V> entry = mapKeyToEntry.get(key);

        // Untrack entry's key
        mapKeyToEntry.remove(entry.key);
        mapFrequencyToKeys.get(entry.frequency.value).remove(entry.key);

        // If removed last key of a frequency, vacate that frequency
        if (mapFrequencyToKeys.get(entry.frequency.value).size() == 0) {
            // Untrack frequency
            mapFrequencyToKeys.remove(entry.frequency.value);
            mapFrequencyToNode.remove(entry.frequency.value);
            --numberOfFrequencies;

            // Break and re-stitch linked list of frequencies
            LFUCacheEntryFrequency leftNeighbor = entry.frequency.left;
            LFUCacheEntryFrequency rightNeighbor = entry.frequency.right;
            if (leftNeighbor != null) leftNeighbor.right = rightNeighbor;
            if (rightNeighbor != null) rightNeighbor.left = leftNeighbor;
            entry.frequency.left = null;
            entry.frequency.right = null;

            // Adjust min & max frequencies if necessary
            if (entry.frequency == minFrequency) minFrequency = rightNeighbor;
            if (entry.frequency == maxFrequency) maxFrequency = leftNeighbor;
        }

        ensureContinuity();
    }

    @Override
    public void elevate(K key) {
        if (DEBUG_MODE)
            System.out.printf("Size %d (%df). Incrementing key %s.", mapKeyToEntry.size(), numberOfFrequencies, key);
        ensureConsistency();
        ensureKeyIsElevatable(key);

        LFUCacheEntry<K, V> entry = mapKeyToEntry.get(key);

        // Establish mode of operation
        boolean vacatingPreviousFrequency = (mapFrequencyToKeys.get(entry.frequency.value).size() == 1);
        boolean movingToOccupiedFrequency = mapFrequencyToNode.containsKey(entry.frequency.value + 1);

        // Depending on mode, perform excellently
        if (vacatingPreviousFrequency && movingToOccupiedFrequency)     vacateOldAndJoinNewFrequency(entry);
        else if (vacatingPreviousFrequency)                             vacateOldAndCreateNewFrequency(entry);
        else if (movingToOccupiedFrequency)                             leaveOldAndJoinNewFrequency(entry);
        else                                                            leaveOldAndCreateNewFrequency(entry);

        ensureContinuity();
    }

    private void vacateOldAndCreateNewFrequency(LFUCacheEntry<K, V> entry) {
        // Extract old and new frequencies
        LFUCacheEntryFrequency oldFrequency = entry.frequency;
        LFUCacheEntryFrequency newFrequency = new LFUCacheEntryFrequency(oldFrequency.value + 1);
        if (DEBUG_MODE) System.out.printf(" (vacating %d -> creating %d)%n", oldFrequency.value, newFrequency.value);

        // Untrack key at old frequency
        mapFrequencyToKeys.get(oldFrequency.value).remove(entry.key);
        mapFrequencyToKeys.remove(oldFrequency.value);

        // Untrack old frequency
        mapFrequencyToNode.remove(oldFrequency.value);

        // Break and re-stitch linked list of frequencies
        newFrequency.left = oldFrequency.left;
        newFrequency.right = oldFrequency.right;
        if (oldFrequency.left != null) oldFrequency.left.right = newFrequency;
        if (oldFrequency.right != null) oldFrequency.right.left = newFrequency;
        oldFrequency.left = null;
        oldFrequency.right = null;

        // Adjust min & max frequencies if necessary
        if (minFrequency == oldFrequency) minFrequency = newFrequency;
        if (maxFrequency == oldFrequency) maxFrequency = newFrequency;

        // Inject new frequency
        entry.setFrequency(newFrequency);

        // Track new frequency
        mapFrequencyToNode.put(newFrequency.value, newFrequency);

        // Track key at new frequency
        mapFrequencyToKeys.put(newFrequency.value, new LinkedHashSet<>());
        mapFrequencyToKeys.get(newFrequency.value).add(entry.key);
    }

    private void vacateOldAndJoinNewFrequency(LFUCacheEntry<K, V> entry) {
        // Extract old and new frequencies
        LFUCacheEntryFrequency oldFrequency = entry.frequency;
        LFUCacheEntryFrequency newFrequency = mapFrequencyToNode.get(entry.frequency.value + 1);
        if (DEBUG_MODE)
            System.out.printf(" (vacating %d -> joining %d)%n", oldFrequency.value, newFrequency.value);
        ensureNeighborlyLinks(entry);

        // Untrack key at old frequency
        mapFrequencyToKeys.get(oldFrequency.value).remove(entry.key);
        mapFrequencyToKeys.remove(oldFrequency.value);

        // Untrack old frequency
        mapFrequencyToNode.remove(oldFrequency.value);

        // Break and re-stitch linked list of frequencies
        if (oldFrequency.left != null) oldFrequency.left.right = newFrequency;
        newFrequency.left = oldFrequency.left;
        oldFrequency.left = null;
        oldFrequency.right = null;

        // One less used frequency in the world
        --numberOfFrequencies;

        // Adjust min frequency if necessary
        if (minFrequency == oldFrequency) minFrequency = newFrequency;

        // Inject new frequency
        entry.setFrequency(newFrequency);

        // Track key at new frequency
        mapFrequencyToKeys.get(newFrequency.value).add(entry.key);
    }

    private void leaveOldAndCreateNewFrequency(LFUCacheEntry<K, V> entry) {
        // Extract old and new frequencies
        LFUCacheEntryFrequency oldFrequency = entry.frequency;
        LFUCacheEntryFrequency newFrequency = new LFUCacheEntryFrequency(oldFrequency.value + 1);
        if (DEBUG_MODE) System.out.printf(" (leaving %d -> creating %d)%n", oldFrequency.value, newFrequency.value);

        // Untrack key at old frequency
        mapFrequencyToKeys.get(oldFrequency.value).remove(entry.key);

        // Break and re-stitch linked list of frequencies
        if (oldFrequency.right != null) {
            newFrequency.right = oldFrequency.right;
            oldFrequency.right.left = newFrequency;
        }
        newFrequency.left = oldFrequency;
        oldFrequency.right = newFrequency;

        // One more frequency in the world
        ++numberOfFrequencies;

        // Adjust max frequency if necessary
        if (maxFrequency == oldFrequency) maxFrequency = newFrequency;

        // Inject new frequency
        entry.setFrequency(newFrequency);

        // Track new frequency
        mapFrequencyToNode.put(newFrequency.value, newFrequency);

        // Track key at new frequency
        mapFrequencyToKeys.put(newFrequency.value, new LinkedHashSet<>());
        mapFrequencyToKeys.get(newFrequency.value).add(entry.key);
    }

    private void leaveOldAndJoinNewFrequency(LFUCacheEntry<K, V> entry) {
        // Extract old and new frequencies
        LFUCacheEntryFrequency oldFrequency = entry.frequency;
        LFUCacheEntryFrequency newFrequency = mapFrequencyToNode.get(entry.frequency.value + 1);
        if (DEBUG_MODE) System.out.printf(" (leaving %d -> joining %d)%n", oldFrequency.value, newFrequency.value);
        ensureNeighborlyLinks(entry);

        // Untrack key at old frequency
        mapFrequencyToKeys.get(oldFrequency.value).remove(entry.key);

        // Inject new frequency
        entry.setFrequency(newFrequency);

        // Track key at new frequency
        mapFrequencyToKeys.get(newFrequency.value).add(entry.key);
    }

    @Override
    public void evict() {
        if (minFrequency == null)
            throw new CacheLogicException("Attempted to remove least frequent value from empty DS");
        LinkedHashSet<K> keysAtMinFrequency;
        try {
            keysAtMinFrequency = mapFrequencyToKeys.get(minFrequency.value);
        } catch (NullPointerException exception) {
            throw new CacheLogicException(
                    "Attempted to remove least frequent value from DS without least frequent values", exception
            );
        }
        remove(keysAtMinFrequency.iterator().next());
    }

    @Override
    public boolean contains(K key) {
        return mapKeyToEntry.containsKey(key);
    }

    @Override
    public V get(K key) {
        V value = null;
        if (contains(key)) {
            LFUCacheEntry<K, V> entry = mapKeyToEntry.get(key);
            if (entry == null)
                throw new CacheLogicException("DS contains null entry despite not allowing it");
            value = entry.getValue();
        }
        return value;
    }

    private void ensureKeyIsAddable(K key) {
        if (mapKeyToEntry.containsKey(key))
            throw new CacheLogicException("Attempted to add key that is already recorded in DS");
        if (mapFrequencyToKeys.containsKey(1L) && mapFrequencyToKeys.get(1L) == null)
            throw new CacheLogicException("Attempted to add entry to DS with null key set at frequency 1");
        if (mapFrequencyToNode.containsKey(1L) && mapFrequencyToNode.get(1L) == null)
            throw new CacheLogicException("Attempted to add entry to DS with null node at frequency 1");
    }

    private void ensureKeyIsUpdatable(K key) {
        if (mapKeyToEntry.size() == 0)
            throw new CacheLogicException("Attempted to update in zero-size DS");
        if (!mapKeyToEntry.containsKey(key))
            throw new CacheLogicException("Attempted to update entry that is not recorded in DS");
        LFUCacheEntry<K, V> entry = mapKeyToEntry.get(key);
        if (entry == null)
            throw new CacheLogicException("Attempted to update entry that is mapped to null in DS");
        if (!mapFrequencyToKeys.containsKey(entry.frequency.value)
                || !mapFrequencyToNode.containsKey(entry.frequency.value))
            throw new CacheLogicException("Attempted to update entry with frequency not recorded in DS");
        if (mapFrequencyToKeys.get(entry.frequency.value) == null)
            throw new CacheLogicException("Attempted to update entry with frequency without stored keys");
        if (!mapFrequencyToKeys.get(entry.frequency.value).contains(entry.key))
            throw new CacheLogicException("Attempted to update entry with frequency without stored entry key");
        if (mapFrequencyToNode.get(entry.frequency.value) == null)
            throw new CacheLogicException("Attempted to update entry with frequency without stored node");
    }

    private void ensureKeyIsRemovable(K key) {
        if (mapKeyToEntry.size() == 0)
            throw new CacheLogicException("Attempted to remove from zero-size DS");
        if (!mapKeyToEntry.containsKey(key))
            throw new CacheLogicException("Attempted to remove entry that is not recorded in DS");
        LFUCacheEntry<K, V> entry = mapKeyToEntry.get(key);
        if (entry == null)
            throw new CacheLogicException("Attempted to remove entry that is mapped to null in DS");
        if (!mapFrequencyToKeys.containsKey(entry.frequency.value)
                || !mapFrequencyToNode.containsKey(entry.frequency.value))
            throw new CacheLogicException("Attempted to remove entry with frequency not recorded in DS");
        if (mapFrequencyToKeys.get(entry.frequency.value) == null)
            throw new CacheLogicException("Attempted to remove entry with frequency without stored keys");
        if (!mapFrequencyToKeys.get(entry.frequency.value).contains(entry.key))
            throw new CacheLogicException("Attempted to remove entry with frequency without stored entry key");
        if (mapFrequencyToNode.get(entry.frequency.value) == null)
            throw new CacheLogicException("Attempted to remove entry with frequency without stored node");
    }

    private void ensureKeyIsElevatable(K key) {
        if (mapKeyToEntry.size() == 0)
            throw new CacheLogicException("Attempted to increment entry in zero-size DS");
        if (!mapKeyToEntry.containsKey(key))
            throw new CacheLogicException("Attempted to increment entry that is not recorded in DS");
        LFUCacheEntry<K, V> entry = mapKeyToEntry.get(key);
        if (entry == null)
            throw new CacheLogicException("Attempted to increment entry that is mapped to null in DS");
        if (!mapFrequencyToKeys.containsKey(entry.frequency.value)
                || !mapFrequencyToNode.containsKey(entry.frequency.value))
            throw new CacheLogicException("Attempted to increment entry with frequency not recorded in DS");
        if (mapFrequencyToKeys.get(entry.frequency.value) == null)
            throw new CacheLogicException("Attempted to increment frequency without stored keys");
        if (!mapFrequencyToKeys.get(entry.frequency.value).contains(entry.key))
            throw new CacheLogicException("Attempted to increment frequency without stored entry key");
        if (mapFrequencyToNode.get(entry.frequency.value) == null)
            throw new CacheLogicException("Attempted to increment frequency without stored node");
        if (mapFrequencyToKeys.containsKey(entry.frequency.value + 1)
                && mapFrequencyToKeys.get(entry.frequency.value + 1) == null)
            throw new CacheLogicException("Attempted to increment frequency into an existing null key set");
        if (mapFrequencyToNode.containsKey(entry.frequency.value + 1)
                && mapFrequencyToNode.get(entry.frequency.value + 1) == null)
            throw new CacheLogicException("Attempted to increment frequency into an existing null node");
    }

    private void ensureNeighborlyLinks(LFUCacheEntry<K, V> entry) {
        LFUCacheEntryFrequency supposedRightNeighbor = mapFrequencyToNode.get(entry.frequency.value + 1);
        boolean lTR = entry.frequency.right == supposedRightNeighbor;
        boolean rTL = supposedRightNeighbor.left == entry.frequency;
        if (!lTR && !rTL)
            throw new CacheLogicException(String.format(
                    "Two neighbor frequencies do not have proper links (%d -x-> %d, %d <-x- %d)",
                    entry.frequency.value, entry.frequency.right.value,
                    supposedRightNeighbor.left.value, supposedRightNeighbor.value
            ));
        if (!lTR)
            throw new CacheLogicException(String.format(
                    "Two neighbor frequencies do not have proper L-to-R link (%d -x-> %d, %d <- %d)",
                    entry.frequency.value, entry.frequency.right.value,
                    supposedRightNeighbor.left.value, supposedRightNeighbor.value
            ));
        if (!rTL)
            throw new CacheLogicException(String.format(
                    "Two neighbor frequencies do not have proper R-to-L link (%d -> %d, %d <-x- %d)",
                    entry.frequency.value, entry.frequency.right.value,
                    supposedRightNeighbor.left.value, supposedRightNeighbor.value
            ));
    }

    private void ensureConsistency() {
        if (numberOfFrequencies < 0)
            throw new CacheLogicException("DS registers negative number of frequencies");
        if (numberOfFrequencies == 0 && mapKeyToEntry.size() != 0) {
            throw new CacheLogicException("DS contains entries but not frequencies");
        }
        if (numberOfFrequencies != mapFrequencyToKeys.size())
            throw new CacheLogicException(String.format(
                    "Sizes of map (freq-keys) and DLL within DS do not match (%d != %d)",
                    numberOfFrequencies, mapFrequencyToKeys.size()
            ));
        if (numberOfFrequencies != mapFrequencyToNode.size())
            throw new CacheLogicException(String.format(
                    "Sizes of map (freq-node) and DLL within DS do not match (%d != %d)",
                    numberOfFrequencies, mapFrequencyToNode.size()
            ));
        if (numberOfFrequencies > mapKeyToEntry.size())
            throw new CacheLogicException("DS registers more frequencies than values");
        if (numberOfFrequencies > 0 && (minFrequency == null || maxFrequency == null))
            throw new CacheLogicException("Non-empty DS has null minFrequency/maxFrequency");
        if (numberOfFrequencies == 0 && (minFrequency != null || maxFrequency != null))
            throw new CacheLogicException("Empty DS has non-null minFrequency/maxFrequency");
        if (numberOfFrequencies > 0 && (minFrequency.left != null || maxFrequency.right != null))
            throw new CacheLogicException("Non-empty DS has non-terminal minFrequency/maxFrequency");
        if (mapFrequencyToKeys.containsKey(0L))
            throw new CacheLogicException("Map (freq-keys) within DS contain keys for frequency zero");
        if (mapFrequencyToNode.containsKey(0L))
            throw new CacheLogicException("Map (freq-node) within DS contain node for frequency zero");
    }

    private void ensureContinuity() {
        if (!DEBUG_MODE) return;
        LFUCacheEntryFrequency temp = minFrequency;
        int counter = numberOfFrequencies + 100;
        int size = 0;
        if (temp != null) {
            ++size;
            while (temp.right != null && counter > 0) {
                if (temp.value < 0)
                    throw new CacheLogicException("DLL within non-empty DS contains negative frequency");
                if (temp.value == 0)
                    throw new CacheLogicException("DLL within non-empty DS contains frequency zero");
                temp = temp.right;
                ++size;
                --counter;
            }
            if (counter == 0)
                throw new CacheLogicException("DLL within non-empty DS likely contains loops");
            if (! temp.equals(maxFrequency))
                throw new CacheLogicException("DLL within non-empty DS is not continuous");
            if (size < numberOfFrequencies)
                throw new CacheLogicException(String.format(
                        "DLL within non-empty DS is shorter than recorded (%d < %d/%d/%d)",
                        size, numberOfFrequencies, mapFrequencyToKeys.size(), mapFrequencyToNode.size()
                        ));
            if (size > numberOfFrequencies)
                throw new CacheLogicException(String.format(
                        "DLL within non-empty DS is longer than recorded (%d > %d/%d/%d)",
                        size, numberOfFrequencies, mapFrequencyToKeys.size(), mapFrequencyToNode.size()
                        ));
        }
    }
}