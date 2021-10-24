package io.mosip.registration.processor.core.tracing;

import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * Helper class to store data in vertx context map
 * taken from vertx-contextual-logging example
 */
public class ContextualData {

    private static final Logger logger = LoggerFactory.getLogger(ContextualData.class);

    /**
     * Put a value in the contextual data map.
     *
     * @param key the key of the data in the contextual data map
     * @param value the data value
     */
    public static void put(String key, Object value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        ContextInternal ctx = (ContextInternal) Vertx.currentContext();
        if (ctx == null) {
            if (logger.isTraceEnabled()) {
                logger.warn("Attempt to set contextual data from a non Vert.x thread", new Exception());
            }
        } else {
            contextualDataMap(ctx).put(key, value);
        }
    }

    /**
     * Get a value from the contextual data map.
     *
     * @param key the key of the data in the contextual data map
     *
     * @return the value or null if absent or the method is invoked on a non Vert.x thread
     */
    public static Object getOrDefault(String key) {
        Objects.requireNonNull(key);
        ContextInternal ctx = (ContextInternal) Vertx.currentContext();
        if (ctx != null) {
            return contextualDataMap(ctx).get(key);
        }
        return null;
    }

    /**
     * Get a value from the contextual data map.
     *
     * @param key the key of the data in the contextual data map
     * @param defaultValue the value returned when the {@code key} is not present in the contextual data map or the method is invoked on a non Vert.x thread
     *
     * @return the value or the {@code defaultValue} if absent or the method is invoked on a non Vert.x thread
     */
    public static Object getOrDefault(String key, Object defaultValue) {
        Objects.requireNonNull(key);
        ContextInternal ctx = (ContextInternal) Vertx.currentContext();
        if (ctx != null) {
            return contextualDataMap(ctx).getOrDefault(key, defaultValue);
        }
        return defaultValue;
    }

    /**
     * Get all values from the contextual data map.
     *
     * @return the values or null if the method is invoked on a non Vert.x thread
     */
    public static Map<String, Object> getAll() {
        ContextInternal ctx = (ContextInternal) Vertx.currentContext();
        if (ctx != null) {
            return new HashMap<>(contextualDataMap(ctx));
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static ConcurrentMap<String, Object> contextualDataMap(ContextInternal ctx) {
        Objects.requireNonNull(ctx);
        return (ConcurrentMap) ctx.contextData().computeIfAbsent(ContextualData.class, k -> new ConcurrentHashMap());
    }
}
