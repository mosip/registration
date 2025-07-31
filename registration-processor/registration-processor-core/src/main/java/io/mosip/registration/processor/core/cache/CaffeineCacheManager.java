package io.mosip.registration.processor.core.cache;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import org.apache.camel.com.github.benmanes.caffeine.cache.Cache;
import org.apache.camel.com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;

public class CaffeineCacheManager {

    /** The logger. */
    private final Logger logger = RegProcessorLogger.getLogger(CaffeineCacheManager.class);

    private final boolean enableCache;

    private Cache<String, String> cache;

    public CaffeineCacheManager(long caffeineCacheExpiry, long caffeineCacheSize, boolean enableCache) {
        this.enableCache = enableCache;
        logger.debug("Caffeine Cache Expiry Time : {} min, Max Size : {}, Enabled : {} ",
                caffeineCacheExpiry, caffeineCacheSize, this.enableCache);
        if(this.enableCache) {
            this.cache = Caffeine.newBuilder()
                    .expireAfterWrite(caffeineCacheExpiry,TimeUnit.MINUTES)
                    .maximumSize(caffeineCacheSize) // optional: set a size limit
                    .build();
        }
    }

    /**
     * Adds a string to the cache with a specific key.
     *
     * @param key   The key for the cache entry.
     * @param value The string value to be cached.
     */
    public void addToCache(String key, String value) {
        if(enableCache)
            cache.put(key, value);
    }

    /**
     * Gets a value from the cache by key.
     *
     * @param key The key to retrieve.
     * @return The cached string or null if not present or expired.
     */
    public String getFromCache(String key) {
        return enableCache ? cache.getIfPresent(key) : null;
    }

    /**
     * Removes a key from the cache immediately.
     *
     * @param key The key to remove.
     */
    public void removeFromCache(String key) {
        if(enableCache)
            cache.invalidate(key);
    }


    /**
     Checks if a key exists in the cache. If not, stores the provided default value.
     @param key The key to check.
     @param defaultValue The value to store if the key is missing.
     @return true if the key already existed in the cache, false if the default was inserted.
     */
    public boolean checkAndPutIfAbsent(String key, String defaultValue) {
        if(!enableCache) {
            return false;
        }

        String val = getFromCache(key);
        if(val != null) {
            logger.debug("Caffeine Cache Hit - Key: {}, Value: {}", key, val);
            return true;
        } else {
            addToCache(key, defaultValue);
            logger.debug("Caffeine Cache Miss - Key: {}, inserted default value: {}", key, defaultValue);
            return false;
        }
    }

    /**
     Overloaded method that inserts a default value of "1" if the key is missing.
     @param key The key to check and possibly insert.
     @return true if the key was already present, false otherwise.
     */
    public boolean checkAndPutIfAbsent(String key) {
        return checkAndPutIfAbsent(key, "1");
    }
}
