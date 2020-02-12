package com.wiley.coyote;

import com.wiley.coyote.cache.Cache;
import com.wiley.coyote.cache.MemoryHogger;

import java.util.Random;

/**
 * Main class, showcases cache implementations.
 */
public class CoyoteApplication {

    /**
     * Specifies maximum number of elements in each cache.
     */
    private static final int CACHE_CAPACITY = 75;

    /**
     * Specifies zero-based range for cache keys, which are randomly chosen
     * from this range on each cache request.
     */
    private static final int KEY_RANGE = 100;

    /**
     * Number of requests to put each cache through.
     */
    private static final long NUM_OF_REQUESTS = 1000L;

    /**
     * Approximate percentage of get requests (as opposed to put requests) to
     * make against each cache.
     */
    private static final int PERCENTAGE_OF_GETS = 50;

    /**
     * Random instance to use during showcase.
     */
    private static final Random RNG = new Random();

    /**
     * Main entry point. Showcases both types of caches (LRU and LFU) by
     * repeatedly storing/retrieving memory-heavy resources. After stress-
     * testing both caches, prints stats summary for each into console.
     * <p>
     * Divides get requests into 'hits' (resource successfully retrieved from
     * cache), 'misses' (no such key in cache), and 'near-hits'. The latter
     * occurs when a resource stored in cache has been garbage collected (due
     * to insufficient memory) by the time it is accessed.
     *
     * @param args  command line arguments
     */
    public static void main(String[] args) {

        // Iterate over possible cache eviction strategies
        for (Cache.EvictionStrategy strategy : Cache.EvictionStrategy.values()) {

            // Create a cache for that strategy
            Cache<Integer, MemoryHogger> cache
                    = Cache.newCache(Integer.class, MemoryHogger.class)
                    .setEvictionStrategy(strategy)
                    .setMaxSize(CACHE_CAPACITY)
                    .build();

            // Work the cache hard
            for (long i = 0; i < NUM_OF_REQUESTS; ++i) {
                if ((RNG.nextInt(100) + 1) > PERCENTAGE_OF_GETS) {
                    cache.put(
                            RNG.nextInt(KEY_RANGE),
                            MemoryHogger.megaBytes(1 + RNG.nextInt(10))
                    );
                } else {
                    cache.get(RNG.nextInt(KEY_RANGE));
                }
            }

            // Print stats on hits and misses
            System.out.printf("%n==========%n");
            cache.stats().printStats();

        }
    }
}
