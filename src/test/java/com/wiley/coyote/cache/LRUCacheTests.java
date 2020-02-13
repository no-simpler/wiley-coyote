package com.wiley.coyote.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LRUCacheTests extends CacheTestMethods {

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
    @DisplayName("LRU: Store & retrieve")
    void whenValuesAreStoredAndRetrieved_thenCorrectValuesAreReturned() {
        runStoreAndRetrieveTest(cache);
    }

    @Test
    @DisplayName("LRU: Scripted access")
    void whenCacheIsAccessedSeveralTimes_thenCorrectStatsAreKept() {
        runScriptedStatsTest(cache);
    }

    @Test
    @DisplayName("LRU: Randomized stress-test")
    void whenCacheUndergoesHeavyRandomizedAccessed_thenItBehavesCorrectly() {
        runRandomizedStatsTest(cache);
    }

    @Test
    @DisplayName("LRU: Near-hits")
    void whenStoringDataThatIsTooBigToBeHeldInMemory_thenNearHitsWillStartAppearing() {
        runNearHitsTest(cache);
    }

    @Test
    @DisplayName("LRU: Null key")
    void whenUsingNullKey_thenBehaviorIsAsUsual() {
        runNullKeyTest(cache);
    }

    @Test
    @DisplayName("LRU: Null value")
    void whenUsingNullValue_thenBehaviorIsAsUsual() {
        runNullKeyAndValueTest(cache);
    }
}
