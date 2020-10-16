package io.mosip.registration.service.security.impl;

import static io.mosip.registration.constants.LoggerConstants.LOG_PKT_AES_ENCRYPTION;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import io.mosip.kernel.keygenerator.bouncycastle.util.KeyGeneratorUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.mosip.kernel.core.crypto.spi.CryptoCoreSpec;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.security.exception.MosipInvalidDataException;
import io.mosip.kernel.core.security.exception.MosipInvalidKeyException;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.security.AESEncryptionService;
import io.mosip.registration.service.security.RSAEncryptionService;

/**
 * API class to encrypt the data using AES algorithm
 * 
 * @author Balaji Sridharan
 * @since 1.0.0
 *
 */
@Service
public class AESEncryptionServiceImpl extends BaseService implements AESEncryptionService {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(AESEncryptionServiceImpl.class);
	@Autowired
	private RSAEncryptionService rsaEncryptionService;
	@Autowired
    private CryptoCoreSpec<byte[], byte[], SecretKey, PublicKey, PrivateKey, String> cryptoCore;

	/**
	 * Instance of {@code AuditFactory}
	 */
	@Autowired
	private AuditManagerService auditFactory;

	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.service.packet.encryption.aes.AESEncryptionService#
	 * encrypt(byte[])
	 */
	@Override
	public byte[] encrypt(final byte[] dataToEncrypt) throws RegBaseCheckedException {
		LOGGER.info(LOG_PKT_AES_ENCRYPTION, APPLICATION_NAME, APPLICATION_ID, "Packet encryption had been started");

		try {
			// Validate the input parameters and required configuration parameters
			validateInputData(dataToEncrypt);

			// Enable AES 256 bit encryption
			Security.setProperty("crypto.policy", "unlimited");

			KeyGenerator keyGenerator = KeyGeneratorUtils.getKeyGenerator("AES", 256);
			// Generate AES Session Key
			final SecretKey symmetricKey = keyGenerator.generateKey();

			// Encrypt the Data using AES
			final byte[] encryptedData = cryptoCore.symmetricEncrypt(symmetricKey, dataToEncrypt,null);

			LOGGER.info(LOG_PKT_AES_ENCRYPTION, APPLICATION_NAME, APPLICATION_ID,
					"In-Memory zip file encrypted using AES Algorithm successfully");

			// Encrypt the AES Session Key using RSA
			final byte[] rsaEncryptedKey = rsaEncryptionService.encrypt(symmetricKey.getEncoded());

			LOGGER.info(LOG_PKT_AES_ENCRYPTION, APPLICATION_NAME, APPLICATION_ID,
					"AES Session Key encrypted using RSA Algorithm successfully");

			// Combine AES Session Key, AES Key Splitter and RSA Encrypted Data
			auditFactory.audit(AuditEvent.PACKET_AES_ENCRYPTED, Components.PACKET_AES_ENCRYPTOR,
					RegistrationConstants.APPLICATION_NAME, AuditReferenceIdTypes.APPLICATION_ID.getReferenceTypeId());

			return CryptoUtil.combineByteArray(encryptedData, rsaEncryptedKey,
					String.valueOf(ApplicationContext.map().get(RegistrationConstants.KEY_SPLITTER)));
		} catch (MosipInvalidDataException mosipInvalidDataException) {
			throw new RegBaseCheckedException(RegistrationExceptionConstants.REG_INVALID_DATA_ERROR_CODE.getErrorCode(),
					RegistrationExceptionConstants.REG_INVALID_DATA_ERROR_CODE.getErrorMessage(),
					mosipInvalidDataException);
		} catch (MosipInvalidKeyException mosipInvalidKeyException) {
			throw new RegBaseCheckedException(RegistrationExceptionConstants.REG_INVALID_KEY_ERROR_CODE.getErrorCode(),
					RegistrationExceptionConstants.REG_INVALID_KEY_ERROR_CODE.getErrorMessage(),
					mosipInvalidKeyException);
		} catch (RuntimeException runtimeException) {
			throw new RegBaseUncheckedException(
					RegistrationExceptionConstants.REG_PACKET_AES_ENCRYPTION_EXCEPTION.getErrorCode(),
					RegistrationExceptionConstants.REG_PACKET_AES_ENCRYPTION_EXCEPTION.getErrorMessage(),
					runtimeException);
		}
	}

	private void validateInputData(final byte[] dataToBeEncrypted) throws RegBaseCheckedException {
		if (ApplicationContext.map().get(RegistrationConstants.KEY_SPLITTER) == null
				|| ApplicationContext.map().get(RegistrationConstants.KEY_SPLITTER).toString().isEmpty()) {
			throwRegBaseCheckedException(RegistrationExceptionConstants.REG_PACKET_KEY_SPLITTER_INVALID);
		}

		if (isByteArrayEmpty(dataToBeEncrypted)) {
			throwRegBaseCheckedException(RegistrationExceptionConstants.REG_PACKET_TO_BE_ENCRYPTED_INVALID);
		}
	}

}
