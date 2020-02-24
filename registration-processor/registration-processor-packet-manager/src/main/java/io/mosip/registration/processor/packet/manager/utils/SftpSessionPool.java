package io.mosip.registration.processor.packet.manager.utils;

import com.jcraft.jsch.Session;
import io.mosip.registration.processor.core.packet.dto.SftpJschConnectionDto;
import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.impl.StackKeyedObjectPool;

/**
 * This class is used to create sftp session pool.
 */
public class SftpSessionPool {

    private KeyedObjectPool<SftpJschConnectionDto, Session> pool;

    private static SftpSessionPool SINGLETON_INSTANCE = null;

    private SftpSessionPool(int maxSession) {
        getSftpSessionPool(maxSession);
    }

    public static SftpSessionPool getInstance(int maxSession) {
        if (SINGLETON_INSTANCE == null) {
            SINGLETON_INSTANCE = new SftpSessionPool(maxSession);
        }
        return SINGLETON_INSTANCE;
    }

    /**
     *
     * @return the org.apache.commons.pool.KeyedObjectPool class
     */
    public KeyedObjectPool<SftpJschConnectionDto, Session> getPool() {
        return pool;
    }

    /**
     *
     * @return the org.apache.commons.pool.KeyedObjectPool class
     */
    private void getSftpSessionPool(int maxSession) {
        pool = new StackKeyedObjectPool<SftpJschConnectionDto, Session>(new SftpSessionFactory(), maxSession);
    }
}

