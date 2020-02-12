package com.wiley.coyote.cache;

/**
 * Represents an instance of an in-memory cache, characterized by its maximum
 * capacity and its eviction strategy (LRU or LFU).
 *
 * This interface emulates a subset of the {@link java.util.Map} interface, and
 * follows its contract where applicable. The implementations are not required
 * to be thread-safe. Also, as part of the contract of this interface, the
 * implementations must not prevent the stored values from being garbage
 * collected in case of memory shortage.
 *
 * The interface houses a few static factory methods that employ
 * {@link CacheBuilder} class to produce new cache instances.
 *
 * @param <K>   the type of keys maintained by this cache
 * @param <V>   the type of mapped values
 * @author Eric Sargazakov
 */
public interface Cache<K, V> {

    /**
     * Default cache capacity used by the CacheBuilder class.
     */
    int DEFAULT_MAX_SIZE = 100;

    /**
     * Default eviction strategy used by the CacheBuilder class.
     */
    EvictionStrategy DEFAULT_EVICTION_STRATEGY = EvictionStrategy.LRU;

    /**
     * Supported eviction strategies.
     */
    enum EvictionStrategy {
        LRU, LFU
    }

    /**
     * It puts the value in the key or else it gets the hose again.
     *
     * @param key   the key with which to associate the new value
     * @param value the value to store in the cache
     * @return  the previous value associated with the key, or null
     */
    V put(K key, V value);

    /**
     * Associates the value with the key, unless a non-null value is already
     * associated with it.
     *
     * @param key   the key with which to associate the new value
     * @param value the value to store in the cache
     * @return  the previous value associated with the key, or null
     */
    default V putIfAbsent(K key, V value) {
        V previousValue = get(key);
        if (previousValue == null)
            previousValue = put(key, value);
        return previousValue;
    }

    /**
     * Retrieves the value associated with the key. When this method returns
     * null, it can mean one of the three possibilities: the key does not exist
     * in the cache; the value associated with the key is null; the value
     * associated with the key has been garbage collected.
     *
     * @param key   the key for which to retrieve the value
     * @return  the value currently associated with the key, or null
     */
    V get(K key);

    /**
     * Indicates whether a mapping involving the key is currently present in
     * the cache. This method is not concerned with whether the value is not
     * null.
     *
     * @param key   the key for which to check the presence of a mapping
     * @return  true if this cache contains a mapping for the key
     */
    boolean containsKey(K key);

    /**
     * Returns a companion object of class {@link Stats}. The companion may
     * then be queried for the current stats of the cache, such as its size or
     * the number of requests processed thus far.
     *
     * @return  the Stats companion object
     */
    Stats stats();

    /**
     * Represents a companion object for the cache object, and contains metrics,
     * describing the cache's current state.
     */
    interface Stats {
        int getSize();

        int getMaxSize();

        EvictionStrategy getEvictionStrategy();

        long getNumberOfGetRequests();

        long getNumberOfHits();

        long getNumberOfNearHits();

        long getNumberOfMisses();

        long getNumberOfPutRequests();

        long getNumberOfInsertions();

        long getNumberOfEvictions();

        long getNumberOfUpdates();

        default void printStats() {

            long numberOfPutRequests = getNumberOfPutRequests();
            long numberOfGetRequests = getNumberOfGetRequests();
            long numberOfRequests = numberOfPutRequests + numberOfGetRequests;

            long percentageOfPutRequests = Math.round(100.0 * numberOfPutRequests / numberOfRequests);
            long percentageOfGetRequests = 100L - percentageOfPutRequests;

            long numberOfInsertions = getNumberOfInsertions();
            long numberOfEvictions = getNumberOfEvictions();
            long numberOfUpdates = getNumberOfUpdates();

            long percentageOfInsertions = Math.round(100.0 * numberOfInsertions / numberOfPutRequests);
            long percentageOfEvictions = Math.round(100.0 * numberOfEvictions / numberOfInsertions);
            long percentageOfUpdates = 100L - percentageOfInsertions;

            long numberOfHits = getNumberOfHits();
            long numberOfNearHits = getNumberOfNearHits();
            long numberOfMisses = getNumberOfMisses();

            long percentageOfHits = Math.round(100.0 * numberOfHits / numberOfGetRequests);
            long percentageOfNearHits = Math.round(100.0 * numberOfNearHits / numberOfGetRequests);
            long percentageOfMisses = 100L - percentageOfHits - percentageOfNearHits;

            System.out.printf(
                    "%s cache (max. capacity: %d):%n", getEvictionStrategy().toString(), getMaxSize()
            );
            System.out.printf("- current size : %d%n", getSize());
            System.out.printf("- requests     : %d%n", numberOfRequests);
            System.out.printf("    - put requests : %d%% (%d)%n", percentageOfPutRequests, numberOfPutRequests);
            System.out.printf("        - insertions   : %d%% (%d) (incl. %d%% evictions (%d))%n",
                    percentageOfInsertions, numberOfInsertions, percentageOfEvictions, numberOfEvictions);
            System.out.printf("        - updates      : %d%% (%d)%n", percentageOfUpdates, numberOfUpdates);
            System.out.printf("    - get requests : %d%% (%d)%n", percentageOfGetRequests, numberOfGetRequests);
            System.out.printf("        - hits         : %d%% (%d)%n", percentageOfHits, numberOfHits);
            System.out.printf("        - near-hits    : %d%% (%d)%n", percentageOfNearHits, numberOfNearHits);
            System.out.printf("        - misses       : %d%% (%d)%n", percentageOfMisses, numberOfMisses);
        }
    }

    /**
     * Returns a new {@link CacheBuilder}, which allows to incrementally build
     * a new cache instance.
     *
     * @param keyClazz      the class of keys to be used in the new cache
     * @param valueClazz    the class of values to be stored in the new cache
     * @param <K>           the key type
     * @param <V>           the value type
     * @return  a new instance of the CacheBuilder class
     */
    static <K, V> CacheBuilder<K, V> newCache(Class<K> keyClazz, Class<V> valueClazz) {
        return new CacheBuilder<>(keyClazz, valueClazz);
    }

    /**
     * Returns a new Cache object, pre-configured with the LRU eviction
     * strategy and the default capacity.
     *
     * @param <K>   the key type
     * @param <V>   the value type
     * @return  a new instance of one of the classes implementing the Cache
     *          interface
     */
    static <K, V> Cache<K, V> buildLRUCache() {
        return new CacheBuilder<K, V>()
                .setEvictionStrategy(EvictionStrategy.LRU)
                .build();
    }

    /**
     * Returns a new Cache object, pre-configured with the LRU eviction
     * strategy and the given capacity.
     *
     * @param maxSize   the capacity limit for the new cache
     * @param <K>       the key type
     * @param <V>       the value type
     * @return  a new instance of one of the classes implementing the Cache
     *          interface
     */
    static <K, V> Cache<K, V> buildLRUCache(int maxSize) {
        return new CacheBuilder<K, V>()
                .setEvictionStrategy(EvictionStrategy.LRU)
                .setMaxSize(maxSize)
                .build();
    }

    /**
     * Returns a new Cache object, pre-configured with the LFU eviction
     * strategy and the default capacity.
     *
     * @param <K>   the key type
     * @param <V>   the value type
     * @return  a new instance of one of the classes implementing the Cache
     *          interface
     */
    static <K, V> Cache<K, V> buildLFUCache() {
        return new CacheBuilder<K, V>()
                .setEvictionStrategy(EvictionStrategy.LFU)
                .build();
    }

    /**
     * Returns a new Cache object, pre-configured with the LFU eviction
     * strategy and the given capacity.
     *
     * @param maxSize   the capacity limit for the new cache
     * @param <K>       the key type
     * @param <V>       the value type
     * @return  a new instance of one of the classes implementing the Cache
     *          interface
     */
    static <K, V> Cache<K, V> buildLFUCache(int maxSize) {
        return new CacheBuilder<K, V>()
                .setEvictionStrategy(EvictionStrategy.LFU)
                .setMaxSize(maxSize)
                .build();
    }
}
