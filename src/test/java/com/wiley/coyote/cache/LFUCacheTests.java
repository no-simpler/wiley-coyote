package com.wiley.coyote.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LFUCacheTests extends CacheTestMethods {

    private static final int MAX_SIZE = 100;

    private Cache<Integer, MemoryHogger> cache;

    @BeforeEach
    void createCache() {
        this.cache = new CacheBuilder<Integer, MemoryHogger>()
                .setMaxSize(MAX_SIZE)
                .setEvictionStrategy(Cache.EvictionStrategy.LFU)
                .build();
    }

    @Test
    @DisplayName("LFU: Store & retrieve")
    void whenValuesAreStoredAndRetrieved_thenCorrectValuesAreReturned() {
        runStoreAndRetrieveTest(cache);
    }

    @Test
    @DisplayName("LFU: Scripted access")
    void whenCacheIsAccessedSeveralTimes_thenCorrectStatsAreKept() {
        runScriptedStatsTest(cache);
    }

    @Test
    @DisplayName("LFU: Randomized stress-test")
    void whenCacheUndergoesHeavyRandomizedAccessed_thenItBehavesCorrectly() {
        runRandomizedStatsTest(cache);
    }

    @Test
    @DisplayName("LFU: Near-hits")
    void whenStoringDataThatIsTooBigToBeHeldInMemory_thenNearHitsWillStartAppearing() {
        runNearHitsTest(cache);
    }

    @Test
    @DisplayName("LFU: Null key")
    void whenUsingNullKey_thenBehaviorIsAsUsual() {
        runNullKeyTest(cache);
    }

    @Test
    @DisplayName("LFU: Null value")
    void whenUsingNullValue_thenBehaviorIsAsUsual() {
        runNullKeyAndValueTest(cache);
    }
}
