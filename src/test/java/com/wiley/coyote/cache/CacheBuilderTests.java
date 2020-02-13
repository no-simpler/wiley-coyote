package com.wiley.coyote.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CacheBuilderTests {

    @Test
    @DisplayName("Default params")
    void whenCacheIsBuiltWithDefaultParameters_thenCorrectInstanceIsReturned() {

        Cache<Double, Double> cache = Cache
                .newCache(Double.class, Double.class)
                .build();
        assertThat(
                cache,
                is(instanceOf(BasicCache.class))
        );
        assertThat(
                cache.stats().getMaxSize(),
                is(equalTo(Cache.DEFAULT_MAX_SIZE))
        );
        assertThat(
                cache.stats().getEvictionStrategy(),
                is(equalTo(Cache.DEFAULT_EVICTION_STRATEGY))
        );
    }

    @Test
    @DisplayName("Custom LRU")
    void whenLRUCacheWithCustomMaxSizeIsBuilt_thenCorrectInstanceIsReturned() {

        final int MAX_SIZE = 80;
        final Cache.EvictionStrategy EVICTION_STRATEGY
                = Cache.EvictionStrategy.LRU;

        Cache<Long, Double> cache = new CacheBuilder<Long, Double>()
                .setMaxSize(MAX_SIZE)
                .setEvictionStrategy(EVICTION_STRATEGY)
                .build();
        assertThat(
                cache,
                is(instanceOf(BasicCache.class))
        );
        assertThat(
                cache.stats().getMaxSize(),
                is(equalTo(MAX_SIZE))
        );
        assertThat(
                cache.stats().getEvictionStrategy(),
                is(equalTo(EVICTION_STRATEGY))
        );
    }

    @Test
    @DisplayName("Custom LFU")
    void whenLFUCacheWithCustomMaxSizeIsBuilt_thenCorrectInstanceIsReturned() {

        final int MAX_SIZE = 999;
        final Cache.EvictionStrategy EVICTION_STRATEGY
                = Cache.EvictionStrategy.LFU;

        Cache<Long, Double> cache = new CacheBuilder<Long, Double>()
                .setMaxSize(MAX_SIZE)
                .setEvictionStrategy(EVICTION_STRATEGY)
                .build();
        assertThat(
                cache,
                is(instanceOf(BasicCache.class))
        );
        assertThat(
                cache.stats().getMaxSize(),
                is(equalTo(MAX_SIZE))
        );
        assertThat(
                cache.stats().getEvictionStrategy(),
                is(equalTo(EVICTION_STRATEGY))
        );
    }

    @Test
    @DisplayName("Illegal capacity 1")
    void whenCacheWithNegativeMaxSizeIsBuilt_thenCorrectExceptionIsThrown() {
        assertThrows(
                IllegalCacheParameterException.class,
                () -> {
                    new CacheBuilder<String, Integer>()
                            .setMaxSize(-1)
                            .build();
                }
        );
    }

    @Test
    @DisplayName("Illegal capacity 2")
    void whenCacheWithZeroMaxSizeIsBuilt_thenCorrectExceptionIsThrown() {
        assertThrows(
                IllegalCacheParameterException.class,
                () -> {
                    new CacheBuilder<String, Integer>()
                            .setMaxSize(0)
                            .build();
                }
        );
    }

    @Test
    @DisplayName("Illegal strategy")
    void whenCacheNullEvictionStrategyIsBuilt_thenCorrectExceptionIsThrown() {
        assertThrows(
                IllegalCacheParameterException.class,
                () -> {
                    new CacheBuilder<String, Integer>()
                            .setEvictionStrategy(null)
                            .build();
                }
        );
    }
}
