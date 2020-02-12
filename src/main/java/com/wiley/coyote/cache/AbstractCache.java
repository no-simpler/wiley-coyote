package com.wiley.coyote.cache;

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
