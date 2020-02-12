package com.wiley.coyote.cache;

public class IllegalCacheParameterException extends CacheException {

    public IllegalCacheParameterException(String message) {
        super(message);
    }

    public IllegalCacheParameterException(String message, Throwable cause) {
        super(message, cause);
    }
}
