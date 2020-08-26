package io.mosip.registration.service.security;


import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.service.security.impl.LocalClientSecurityImpl;
import io.mosip.registration.service.security.impl.TPMClientSecurityImpl;

import org.apache.commons.lang3.RandomStringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Base64;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

public class ClientSecurityFacade {

    private static final Logger LOGGER = AppConfig.getLogger(ClientSecurityFacade.class);
    private static final String LOGGER_CLASS_NAME = "REGISTRATION - ClientSecurityFacade";

    private static final String KEY_PATH = System.getProperty("user.home");
    private static final String KEYS_DIR = ".mosipkeys";
    private static final String DB_PWD_FILE = "db.conf";

    private static ClientSecurity clientSecurity = null;

    private static BouncyCastleProvider provider;

    static {
        provider = new BouncyCastleProvider();
        Security.addProvider(provider);
        Security.setProperty("crypto.policy", "unlimited");
    }

    private static void initializeClientSecurity() {
        LOGGER.debug(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, "initializeClientSecurity >>> started");
        try {
            clientSecurity = new TPMClientSecurityImpl();

        } catch(Exception | java.lang.NoClassDefFoundError e) {
            LOGGER.debug(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, ExceptionUtils.getStackTrace(e));
            LOGGER.warn(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, "SWITCHING TO LOCAL SECURITY IMPL");

            try {
                clientSecurity = new LocalClientSecurityImpl();
            } catch (Exception ex) {
                LOGGER.error(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, ExceptionUtils.getStackTrace(ex));
                LOGGER.error(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, "Failed to load Client security instance");
            }
        }

        if(clientSecurity == null) {
            LOGGER.error(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID,
                    "Failed to create client security instance, exiting application.");
            System.exit(1);
        }

        LOGGER.debug(LOGGER_CLASS_NAME, APPLICATION_NAME, APPLICATION_ID, "initializeClientSecurity >>> Done");
    }

    public static ClientSecurity getClientSecurity() {
        if(clientSecurity == null ) {
            initializeClientSecurity();
        }
        return clientSecurity;
    }

    public static String getClientInstancePublicKey() throws RegBaseCheckedException {
        String environment = (String) ApplicationContext.map().get(RegistrationConstants.SERVER_ACTIVE_PROFILE);

        if(RegistrationConstants.SERVER_PROD_PROFILE.equalsIgnoreCase(environment) && !clientSecurity.isTPMInstance()) {
            LOGGER.info("REGISTRATION  - SECURITY_FACADE", APPLICATION_NAME, APPLICATION_ID, "TPM IS REQUIRED TO BE ENABLED.");
            throw new RegBaseCheckedException(RegistrationExceptionConstants.TPM_REQUIRED.getErrorCode(),
                    RegistrationExceptionConstants.TPM_REQUIRED.getErrorMessage());
        }

        LOGGER.info("REGISTRATION  - SECURITY_FACADE", APPLICATION_NAME, APPLICATION_ID, "CURRENT PROFILE : " +
                environment != null ? environment : RegistrationConstants.SERVER_NO_PROFILE);

        return CryptoUtil.encodeBase64(getClientSecurity().getSigningPublicPart());
    }

    public static boolean isDBInitializeRequired() {
        File parentDir = new File(KEY_PATH + File.separator + KEYS_DIR);
        if(!parentDir.exists())
            parentDir.mkdirs();

        File dbConf = new File(KEY_PATH + File.separator + KEYS_DIR + File.separator + DB_PWD_FILE);
        if(dbConf.exists())
            return false;

        return true;
    }

    public static String getDBSecret() throws IOException, NoSuchAlgorithmException {
        File dbConf = new File(KEY_PATH + File.separator + KEYS_DIR + File.separator + DB_PWD_FILE);
        if(!dbConf.exists()) {
            LOGGER.info("REGISTRATION  - SECURITY_FACADE", APPLICATION_NAME, APPLICATION_ID,
                    "getDBSecret invoked - DB_PWD_FILE not found !");
            String newBootPassowrd = RandomStringUtils.random(20, true, true);
            byte[] cipher = getClientSecurity().asymmetricEncrypt(newBootPassowrd.getBytes());

            try(FileOutputStream fos = new FileOutputStream(dbConf)) {
                fos.write(Base64.getEncoder().encode(cipher));
                LOGGER.debug("REGISTRATION  - SECURITY_FACADE", APPLICATION_NAME, APPLICATION_ID, "Generated new derby boot key");
            }
        }

        String key = new String(Files.readAllBytes(dbConf.toPath()));
        return new String(getClientSecurity().asymmetricDecrypt(Base64.getDecoder().decode(key)));
    }
}