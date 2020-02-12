package com.wiley.coyote.cache;

public class CacheLogicException extends CacheException {

    public CacheLogicException(String message) {
        super(message);
    }

    public CacheLogicException(String message, Throwable cause) {
        super(message, cause);
    }
}
