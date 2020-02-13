package com.wiley.coyote.cache;

/**
 * Implements the cache stats and exposes to the extending classes a small API
 * of protected methods, as a way to register cache events (for example, an
 * incoming get request).
 *
 * @param <K>   the key type
 * @param <V>   the value type
 */
abstract class AbstractCache<K, V> implements Cache<K, V> {

    private long numberOfHits = 0L;

    private long numberOfNearHits = 0L;

    private long numberOfMisses = 0L;

    private long numberOfInsertions = 0L;

    private long numberOfEvictions = 0L;

    private long numberOfUpdates = 0L;

    protected void registerHit() {
        ++numberOfHits;
    }

    protected void registerNearHit() {
        ++numberOfNearHits;
    }

    protected void registerMiss() {
        ++numberOfMisses;
    }

    protected void registerInsertion() {
        ++numberOfInsertions;
    }

    protected void registerEviction() {
        ++numberOfEvictions;
    }

    protected void registerUpdate() {
        ++numberOfUpdates;
    }

    /**
     * Implements the majority of {@link com.wiley.coyote.cache.Cache.Stats}
     * methods.
     */
    protected abstract class AbstractStats implements Stats {

        @Override
        public long getNumberOfGetRequests() {
            return numberOfHits + numberOfMisses + numberOfNearHits;
        }

        @Override
        public long getNumberOfHits() {
            return numberOfHits;
        }

        @Override
        public long getNumberOfNearHits() {
            return numberOfNearHits;
        }

        @Override
        public long getNumberOfMisses() {
            return numberOfMisses;
        }

        @Override
        public long getNumberOfPutRequests() {
            return numberOfInsertions + numberOfUpdates;
        }

        @Override
        public long getNumberOfInsertions() {
            return numberOfInsertions;
        }

        @Override
        public long getNumberOfEvictions() {
            return numberOfEvictions;
        }

        @Override
        public long getNumberOfUpdates() {
            return numberOfUpdates;
        }
    }
}
