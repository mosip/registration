package io.mosip.registration.service.security.impl;

import static io.mosip.registration.constants.LoggerConstants.LOG_PKT_RSA_ENCRYPTION;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.mosip.kernel.core.crypto.spi.CryptoCoreSpec;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.security.RSAEncryptionService;

/**
 * Accepts AES session key as bytes and encrypt it by using RSA algorithm
 * 
 * @author YASWANTH S
 * @author Balaji Sridharan
 * @since 1.0.0
 *
 */
@Service
public class RSAEncryptionServiceImpl extends BaseService implements RSAEncryptionService {

	static final Logger LOGGER = AppConfig.getLogger(RSAEncryptionServiceImpl.class);

	@Autowired
    private CryptoCoreSpec<byte[], byte[], SecretKey, PublicKey, PrivateKey, String> cryptoCore;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.service.packet.encryption.rsa.RSAEncryptionService#
	 * encrypt(byte[])
	 */
	@Override
	public byte[] encrypt(final byte[] sessionKey) throws RegBaseCheckedException {
		try {
			LOGGER.info(LOG_PKT_RSA_ENCRYPTION, APPLICATION_NAME, APPLICATION_ID,
					"Packet RSA Encryption had been called");

			// Validate the input parameters and required configuration parameters
			validateInputData(sessionKey);

//			String stationId = getStationId(RegistrationSystemPropertiesChecker.getMachineId());
//			String centerMachineId = getCenterId(stationId) + "_" + stationId;
//
//			// encrypt AES Session Key using RSA public key
//			KeyStore rsaPublicKey = policySyncDAO.getPublicKey(centerMachineId);
//			if (rsaPublicKey == null) {
//				throw new RegBaseCheckedException(
//						RegistrationExceptionConstants.REG_RSA_PUBLIC_KEY_NOT_FOUND.getErrorCode(),
//						RegistrationExceptionConstants.REG_RSA_PUBLIC_KEY_NOT_FOUND.getErrorMessage());
//			}
//			PublicKey publicKey = PublicKeyGenerationUtil.generatePublicKey(rsaPublicKey.getPublicKey());

			PublicKey publicKey = null;
			
			return cryptoCore.asymmetricEncrypt(publicKey, sessionKey);
		} catch (Exception compileTimeException) {
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_INVALID_DATA_RSA_ENCRYPTION.getErrorCode(),
					RegistrationExceptionConstants.REG_INVALID_DATA_RSA_ENCRYPTION.getErrorMessage(),
					compileTimeException);
		}
	}

	private void validateInputData(final byte[] dataToBeEncrypted) throws RegBaseCheckedException {
		if (isByteArrayEmpty(dataToBeEncrypted)) {
			throwRegBaseCheckedException(RegistrationExceptionConstants.REG_INVALID_DATA_FOR_RSA_ENCRYPTION);
		}
	}

}
