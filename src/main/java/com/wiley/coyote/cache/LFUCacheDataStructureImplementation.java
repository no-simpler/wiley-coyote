package com.wiley.coyote.cache;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

class LFUCacheDataStructureImplementation<K, V> implements LFUCacheDataStructure<K, V> {

    private final boolean DEBUG_MODE = false;

    private final Map<K, LFUCacheEntry<K, V>> map;

    private final Map<Long, LinkedHashSet<K>> keysPerFrequency;

    private final Map<Long, LFUCacheEntryFrequency> nodePerFrequency;

    private LFUCacheEntryFrequency minFrequency;

    private LFUCacheEntryFrequency maxFrequency;

    private int numberOfFrequencies;

    static class LFUCacheEntry<K, V> {
        private final K key;
        private SoftReference<V> value;
        private LFUCacheEntryFrequency frequency;

        LFUCacheEntry(K key, V value) {
            this.key = key;
            this.value = new SoftReference<>(value);
            this.frequency = null;
        }

        void setValue(V value) {
            this.value = new SoftReference<>(value);
        }

        V getValue() {
            return value.get();
        }

        private void setFrequency(LFUCacheEntryFrequency frequency) {
            this.frequency = frequency;
        }
    }

    static class LFUCacheEntryFrequency {
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

    LFUCacheDataStructureImplementation(int maxSize) {
        map = new HashMap<>(maxSize);
        keysPerFrequency = new HashMap<>();
        nodePerFrequency = new HashMap<>();
        maxFrequency = null;
        minFrequency = null;
        numberOfFrequencies = 0;
    }

    @Override
    public int getSize() {
        return map.size();
    }

    @Override
    public void add(LFUCacheEntry<K, V> newEntry) {
        if (DEBUG_MODE)
            System.out.printf("Size %d (%df). Adding key %s.%n", map.size(), numberOfFrequencies, newEntry.key);
        ensureConsistency();
        ensureEntryIsAddable(newEntry);

        // Diverge based on presence of other entries
        if (numberOfFrequencies == 0) {
            // Add the very first entry to this data structure
            newEntry.setFrequency(new LFUCacheEntryFrequency());

            // Set min and max frequencies
            minFrequency = newEntry.frequency;
            maxFrequency = newEntry.frequency;

            // Ensure frequency node is tracked
            nodePerFrequency.put(newEntry.frequency.value, newEntry.frequency);
            ++numberOfFrequencies;
        } else if (nodePerFrequency.containsKey(1L)) {
            // Add to pre-existing entries with frequency 1
            newEntry.setFrequency(nodePerFrequency.get(1L));
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
            nodePerFrequency.put(newEntry.frequency.value, newEntry.frequency);
            ++numberOfFrequencies;
        }

        // Add new key, creating fresh sets as necessary
        if (!keysPerFrequency.containsKey(newEntry.frequency.value)) {
            keysPerFrequency.put(newEntry.frequency.value, new LinkedHashSet<>());
        }
        keysPerFrequency.get(newEntry.frequency.value).add(newEntry.key);

        // Track entry itself
        map.put(newEntry.key, newEntry);
        ensureContinuity();
    }

    @Override
    public void remove(LFUCacheEntry<K, V> entry) {
        if (DEBUG_MODE)
            System.out.printf("Size %d (%df). Removing key %s.%n", map.size(), numberOfFrequencies, entry.key);
        ensureConsistency();
        ensureEntryIsRemovable(entry);

        // Untrack entry's key
        map.remove(entry.key);
        keysPerFrequency.get(entry.frequency.value).remove(entry.key);

        // If removed last key of a frequency, vacate that frequency
        if (keysPerFrequency.get(entry.frequency.value).size() == 0) {
            // Untrack frequency
            keysPerFrequency.remove(entry.frequency.value);
            nodePerFrequency.remove(entry.frequency.value);
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
    public void incrementFrequency(LFUCacheEntry<K, V> entry) {
        if (DEBUG_MODE)
            System.out.printf("Size %d (%df). Incrementing key %s.", map.size(), numberOfFrequencies, entry.key);
        ensureConsistency();
        ensureEntryIsIncrementable(entry);

        // Establish mode of operation
        boolean vacatingPreviousFrequency = (keysPerFrequency.get(entry.frequency.value).size() == 1);
        boolean movingToOccupiedFrequency = nodePerFrequency.containsKey(entry.frequency.value + 1);

        // Depending on mode, perform excellently
        if (vacatingPreviousFrequency && movingToOccupiedFrequency)     vacateOldAndJoinNew(entry);
        else if (vacatingPreviousFrequency)                             vacateOldAndCreateNew(entry);
        else if (movingToOccupiedFrequency)                             leaveOldAndJoinNew(entry);
        else                                                            leaveOldAndCreateNew(entry);

        ensureContinuity();
    }

    private void vacateOldAndCreateNew(LFUCacheEntry<K, V> entry) {
        // Extract old and new frequencies
        LFUCacheEntryFrequency oldFrequency = entry.frequency;
        LFUCacheEntryFrequency newFrequency = new LFUCacheEntryFrequency(oldFrequency.value + 1);
        if (DEBUG_MODE) System.out.printf(" (vacating %d -> creating %d)%n", oldFrequency.value, newFrequency.value);

        // Untrack key at old frequency
        keysPerFrequency.get(oldFrequency.value).remove(entry.key);
        keysPerFrequency.remove(oldFrequency.value);

        // Untrack old frequency
        nodePerFrequency.remove(oldFrequency.value);

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
        nodePerFrequency.put(newFrequency.value, newFrequency);

        // Track key at new frequency
        keysPerFrequency.put(newFrequency.value, new LinkedHashSet<>());
        keysPerFrequency.get(newFrequency.value).add(entry.key);
    }

    private void vacateOldAndJoinNew(LFUCacheEntry<K, V> entry) {
        // Extract old and new frequencies
        LFUCacheEntryFrequency oldFrequency = entry.frequency;
        LFUCacheEntryFrequency newFrequency = nodePerFrequency.get(entry.frequency.value + 1);
        if (DEBUG_MODE)
            System.out.printf(" (vacating %d -> joining %d)%n", oldFrequency.value, newFrequency.value);
        ensureNeighborlyLinks(entry);

        // Untrack key at old frequency
        keysPerFrequency.get(oldFrequency.value).remove(entry.key);
        keysPerFrequency.remove(oldFrequency.value);

        // Untrack old frequency
        nodePerFrequency.remove(oldFrequency.value);

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
        keysPerFrequency.get(newFrequency.value).add(entry.key);
    }

    private void leaveOldAndCreateNew(LFUCacheEntry<K, V> entry) {
        // Extract old and new frequencies
        LFUCacheEntryFrequency oldFrequency = entry.frequency;
        LFUCacheEntryFrequency newFrequency = new LFUCacheEntryFrequency(oldFrequency.value + 1);
        if (DEBUG_MODE) System.out.printf(" (leaving %d -> creating %d)%n", oldFrequency.value, newFrequency.value);

        // Untrack key at old frequency
        keysPerFrequency.get(oldFrequency.value).remove(entry.key);

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
        nodePerFrequency.put(newFrequency.value, newFrequency);

        // Track key at new frequency
        keysPerFrequency.put(newFrequency.value, new LinkedHashSet<>());
        keysPerFrequency.get(newFrequency.value).add(entry.key);
    }

    private void leaveOldAndJoinNew(LFUCacheEntry<K, V> entry) {
        // Extract old and new frequencies
        LFUCacheEntryFrequency oldFrequency = entry.frequency;
        LFUCacheEntryFrequency newFrequency = nodePerFrequency.get(entry.frequency.value + 1);
        if (DEBUG_MODE) System.out.printf(" (leaving %d -> joining %d)%n", oldFrequency.value, newFrequency.value);
        ensureNeighborlyLinks(entry);

        // Untrack key at old frequency
        keysPerFrequency.get(oldFrequency.value).remove(entry.key);

        // Inject new frequency
        entry.setFrequency(newFrequency);

        // Track key at new frequency
        keysPerFrequency.get(newFrequency.value).add(entry.key);
    }

    @Override
    public void removeLeastFrequent() {
        if (minFrequency == null)
            throw new CacheLogicException("Attempted to remove least frequent value from empty DS");
        LinkedHashSet<K> keysAtMinFrequency;
        try {
            keysAtMinFrequency = keysPerFrequency.get(minFrequency.value);
        } catch (NullPointerException exception) {
            throw new CacheLogicException(
                    "Attempted to remove least frequent value from DS without least frequent values", exception
            );
        }
        remove(get(keysAtMinFrequency.iterator().next()));
    }

    @Override
    public boolean contains(K key) {
        ensureConsistency();
        ensureContinuity();
        return map.containsKey(key);
    }

    @Override
    public LFUCacheEntry<K, V> get(K key) {
        LFUCacheEntry<K, V> entry = null;
        if (contains(key)) {
            entry = map.get(key);
            if (entry == null)
                throw new CacheLogicException("DS contains null entry despite not allowing it");
        }
        return entry;
    }

    private void ensureEntryIsAddable(LFUCacheEntry<K, V> entry) {
        if (entry == null)
            throw new CacheLogicException("Attempted to add null entry to DS");
        if (map.containsKey(entry.key))
            throw new CacheLogicException("Attempted to add key that is already recorded in DS");
        if (entry.frequency != null)
            throw new CacheLogicException("Attempted to add entry with non-null frequency to DS");
        if (keysPerFrequency.containsKey(1L) && keysPerFrequency.get(1L) == null)
            throw new CacheLogicException("Attempted to increment frequency into an existing null key set");
        if (nodePerFrequency.containsKey(1L) && nodePerFrequency.get(1L) == null)
            throw new CacheLogicException("Attempted to increment frequency into an existing null node");

    }

    private void ensureEntryIsRemovable(LFUCacheEntry<K, V> entry) {
        if (entry == null)
            throw new CacheLogicException("Attempted to remove null entry from DS");
        if (map.size() == 0)
            throw new CacheLogicException("Attempted to remove from zero-size DS");
        if (!map.containsKey(entry.key))
            throw new CacheLogicException("Attempted to remove entry that is not recorded in DS");
        if (!keysPerFrequency.containsKey(entry.frequency.value)
                || !nodePerFrequency.containsKey(entry.frequency.value)) {
            throw new CacheLogicException("Attempted to remove entry with frequency not recorded in DS");
        }
        if (keysPerFrequency.get(entry.frequency.value) == null)
            throw new CacheLogicException("Attempted to remove entry with frequency without stored keys");
        if (!keysPerFrequency.get(entry.frequency.value).contains(entry.key))
            throw new CacheLogicException("Attempted to remove entry with frequency without stored entry key");
        if (nodePerFrequency.get(entry.frequency.value) == null)
            throw new CacheLogicException("Attempted to remove entry with frequency without stored node");
    }

    private void ensureEntryIsIncrementable(LFUCacheEntry<K, V> entry) {
        if (entry == null)
            throw new CacheLogicException("Attempted to increment null entry from DS");
        if (map.size() == 0)
            throw new CacheLogicException("Attempted to increment entry in zero-size DS");
        if (!map.containsKey(entry.key))
            throw new CacheLogicException("Attempted to increment entry that is not recorded in DS");
        if (!keysPerFrequency.containsKey(entry.frequency.value)
                || !nodePerFrequency.containsKey(entry.frequency.value)) {
            throw new CacheLogicException("Attempted to increment entry with frequency not recorded in DS");
        }
        if (keysPerFrequency.get(entry.frequency.value) == null)
            throw new CacheLogicException("Attempted to increment frequency without stored keys");
        if (!keysPerFrequency.get(entry.frequency.value).contains(entry.key))
            throw new CacheLogicException("Attempted to increment frequency without stored entry key");
        if (nodePerFrequency.get(entry.frequency.value) == null)
            throw new CacheLogicException("Attempted to increment frequency without stored node");
        if (keysPerFrequency.containsKey(entry.frequency.value + 1)
                && keysPerFrequency.get(entry.frequency.value + 1) == null)
            throw new CacheLogicException("Attempted to increment frequency into an existing null key set");
        if (nodePerFrequency.containsKey(entry.frequency.value + 1)
                && nodePerFrequency.get(entry.frequency.value + 1) == null)
            throw new CacheLogicException("Attempted to increment frequency into an existing null node");
    }

    private void ensureNeighborlyLinks(LFUCacheEntry<K, V> entry) {
        LFUCacheEntryFrequency supposedRightNeighbor = nodePerFrequency.get(entry.frequency.value + 1);
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
        if (numberOfFrequencies == 0 && map.size() != 0) {
            throw new CacheLogicException("DS contains entries but not frequencies");
        }
        if (numberOfFrequencies != keysPerFrequency.size())
            throw new CacheLogicException(String.format(
                    "Sizes of map (freq-keys) and DLL within DS do not match (%d != %d)",
                    numberOfFrequencies, keysPerFrequency.size()
            ));
        if (numberOfFrequencies != nodePerFrequency.size())
            throw new CacheLogicException(String.format(
                    "Sizes of map (freq-node) and DLL within DS do not match (%d != %d)",
                    numberOfFrequencies, nodePerFrequency.size()
            ));
        if (numberOfFrequencies > map.size())
            throw new CacheLogicException("DS registers more frequencies than values");
        if (numberOfFrequencies > 0 && (minFrequency == null || maxFrequency == null))
            throw new CacheLogicException("Non-empty DS has null minFrequency/maxFrequency");
        if (numberOfFrequencies == 0 && (minFrequency != null || maxFrequency != null))
            throw new CacheLogicException("Empty DS has non-null minFrequency/maxFrequency");
        if (numberOfFrequencies > 0 && (minFrequency.left != null || maxFrequency.right != null))
            throw new CacheLogicException("Non-empty DS has non-terminal minFrequency/maxFrequency");
        if (keysPerFrequency.containsKey(0L))
            throw new CacheLogicException("Map (freq-keys) within DS contain keys for frequency zero");
        if (nodePerFrequency.containsKey(0L))
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
                        size, numberOfFrequencies, keysPerFrequency.size(), nodePerFrequency.size()
                        ));
            if (size > numberOfFrequencies)
                throw new CacheLogicException(String.format(
                        "DLL within non-empty DS is longer than recorded (%d > %d/%d/%d)",
                        size, numberOfFrequencies, keysPerFrequency.size(), nodePerFrequency.size()
                        ));
        }
    }
}