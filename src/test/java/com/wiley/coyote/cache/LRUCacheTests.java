package com.wiley.coyote.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNull;

class LRUCacheTests {

    private static final int MAX_SIZE = 100;

    private Cache<Integer, MemoryHogger> cache;

    @BeforeEach
    void createCache() {
        this.cache = new CacheBuilder<Integer, MemoryHogger>()
                .setMaxSize(MAX_SIZE)
                .setEvictionStrategy(Cache.EvictionStrategy.LRU)
                .build();
    }

    @Test
    @DisplayName("Store & retrieve")
    void whenValuesAreStoredAndRetrieved_thenCorrectValuesAreReturned() {

        // Store references to prevent GC
        MemoryHogger[] references = new MemoryHogger[20];

        // Put references into cache
        for (int i = 0; i < 10; ++i) {
            references[i] = MemoryHogger.bytes(1);
            cache.put(i, references[i]);
        }

        // Retrieve existing references from cache
        for (int i = 0; i < 10; ++i) {
            assertThat(cache.containsKey(i), is(true));
            MemoryHogger retrievedValue = cache.get(i);
            assertThat(retrievedValue, is(equalTo(references[i])));
        }

        // Retrieve non-existing references from cache
        for (int i = 10; i < 20; ++i) {
            assertThat(cache.containsKey(i), is(false));
            MemoryHogger retrievedValue = cache.get(i);
            assertNull(retrievedValue);
        }
    }

    @Test
    @DisplayName("Scripted access")
    void whenCacheIsAccessedSeveralTimes_thenCorrectStatsAreKept() {

        // Store references to prevent GC
        MemoryHogger[] references = new MemoryHogger[120];

        // Put references into cache
        for (int i = 0; i < 60; ++i) {
            references[i] = MemoryHogger.bytes(1);
            cache.put(i, references[i]);
        }

        // Put references on top, to force updates
        for (int i = 0; i < 40; ++i) {
            references[i] = MemoryHogger.bytes(1);
            cache.put(i, references[i]);
        }

        // Retrieve references from cache
        for (int i = 55; i < 75; ++i) {
            cache.get(i);
        }

        // Put more references than there is space, to force evictions
        for (int i = 60; i < 120; ++i) {
            references[i] = MemoryHogger.bytes(1);
            cache.put(i, references[i]);
        }

        // Check stats
        assertThat(cache.stats().getSize(), is(100));
        assertThat(cache.stats().getNumberOfPutRequests(), is(160L));
        assertThat(cache.stats().getNumberOfGetRequests(), is(20L));
        assertThat(cache.stats().getNumberOfHits(), is(5L));
        assertThat(cache.stats().getNumberOfNearHits(), is(0L));
        assertThat(cache.stats().getNumberOfMisses(), is(15L));
        assertThat(cache.stats().getNumberOfInsertions(), is(120L));
        assertThat(cache.stats().getNumberOfEvictions(), is(20L));
        assertThat(cache.stats().getNumberOfUpdates(), is(40L));
    }

    @Test
    @DisplayName("Randomized stress-test")
    void whenCacheUndergoesHeavyRandomizedAccessed_thenItBehavesCorrectly() {

        // Randomness seeds
        final int KEY_RANGE = 200;
        final Random RNG = new Random();
        final long VALUE_SIZE = 1;
        final int[] thresholds = {0, 20, 55, 30, 90, 75};
        int numberOfIterations = 1_000_000 + RNG.nextInt(10_000_000);

        // Store references to prevent GC
        MemoryHogger[] references = new MemoryHogger[KEY_RANGE];

        // Stat counters
        long puts = 0L, inserts = 0L, evicts = 0L, updates = 0L;
        long gets = 0L, hits = 0L, nearHits = 0L, misses = 0L;

        // Storage variables
        int key;
        MemoryHogger value;
        int size = 0, mismatchedGets = 0;

        // Commence randomized caching
        for (int i = 0; i < numberOfIterations; ++i) {
            if (RNG.nextInt(100) > thresholds[RNG.nextInt(thresholds.length)]) {
                // Simulate put request
                ++puts;
                key = RNG.nextInt(KEY_RANGE);
                value = MemoryHogger.bytes(VALUE_SIZE);
                if (references[key] == null) {
                    ++inserts;
                    if (size >= MAX_SIZE) ++evicts;
                    else ++size;
                } else {
                    if (cache.containsKey(key)) ++updates;
                    else {
                        ++inserts;
                        if (size >= MAX_SIZE) ++evicts;
                        else ++size;
                    }
                }
                references[key] = value;
                cache.put(key, value);
            } else {
                // Simulate get request
                ++gets;
                key = RNG.nextInt(KEY_RANGE);
                value = cache.get(key);
                if (value != null && value != references[key])
                    ++mismatchedGets;
                if (value == null) {
                    ++misses;
                } else {
                    ++hits;
                }
            }
        }

        // Check stats
        assertThat(cache.stats().getSize(), is(equalTo(size)));
        assertThat(mismatchedGets, is(equalTo(0)));
        assertThat(cache.stats().getNumberOfPutRequests(), is(equalTo(puts)));
        assertThat(cache.stats().getNumberOfGetRequests(), is(equalTo(gets)));
        assertThat(cache.stats().getNumberOfHits(), is(equalTo(hits)));
        assertThat(cache.stats().getNumberOfNearHits(), is(equalTo(nearHits)));
        assertThat(cache.stats().getNumberOfMisses(), is(equalTo(misses)));
        assertThat(cache.stats().getNumberOfInsertions(), is(equalTo(inserts)));
        assertThat(cache.stats().getNumberOfEvictions(), is(equalTo(evicts)));
        assertThat(cache.stats().getNumberOfUpdates(), is(equalTo(updates)));
    }

    @Test
    @DisplayName("Near-hits")
    void whenStoringDataThatIsTooBigToBeHeldInMemory_thenNearHitsWillStartAppearing() {

        // Grab 25% of memory with every new cached value
        long maxMemory = Runtime.getRuntime().maxMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        final long VALUE_SIZE = (freeMemory + (maxMemory - totalMemory)) / 4;

        // Put enough values into cache to make sure they start being GC'd
        for (int i = 0; i < 100; ++i)
            cache.put(i, MemoryHogger.bytes(VALUE_SIZE));

        // Attempt to retrieve each one
        for (int i = 0; i < 100; ++i)
            cache.get(i);

        // Check that near-hits were encountered
        assertThat(
                cache.stats().getNumberOfNearHits(),
                is(greaterThan(0L))
        );
    }

    @Test
    @DisplayName("Null key")
    void whenUsingNullKey_thenBehaviorIsAsUsual() {
        MemoryHogger value = MemoryHogger.bytes(1);
        cache.put(null, value);
        assertThat(cache.get(null), is(equalTo(value)));
    }

    @Test
    @DisplayName("Null value")
    void whenUsingNullValue_thenBehaviorIsAsUsual() {
        MemoryHogger value = null;
        cache.put(null, value);
        assertThat(cache.get(null), is(equalTo(value)));
    }
}
