package io.mosip.registration.processor.packet.manager.utils;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.SftpJschConnectionDto;
import org.apache.commons.pool.BaseKeyedPoolableObjectFactory;

import java.util.Properties;

/**
 * This class is used to obtain session for sftp connection
 */
public class SftpSessionFactory extends BaseKeyedPoolableObjectFactory<SftpJschConnectionDto, Session> {

    /** The reg proc logger. */
    private static Logger regProcLogger = RegProcessorLogger.getLogger(SftpSessionFactory.class);

    /**
     * Method to create a new session.
     */
    @Override
    public Session makeObject(SftpJschConnectionDto sftpConnectionDto) throws Exception {
        Session session = null;

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(sftpConnectionDto.getUser(), sftpConnectionDto.getHost(),
                    sftpConnectionDto.getPort());
            if (sftpConnectionDto.getDmzServerPwd() != null && !sftpConnectionDto.getDmzServerPwd().isEmpty()) {
                session.setPassword(sftpConnectionDto.getDmzServerPwd());
            } else {
                jsch.addIdentity(sftpConnectionDto.getRegProcPPK());
            }
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
        } catch (Exception e) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    "Exception occured, will retry again to reconnect", e.getMessage() + ExceptionUtils.getStackTrace(e));

            makeObject(sftpConnectionDto);
        }
        return session != null && session.isConnected() ? session : makeObject(sftpConnectionDto);
    }

    /**
     * This is called when closing the pool object
     */
    @Override
    public void destroyObject(SftpJschConnectionDto sftpJschConnectionDto, Session session) {
        session.disconnect();
    }
}