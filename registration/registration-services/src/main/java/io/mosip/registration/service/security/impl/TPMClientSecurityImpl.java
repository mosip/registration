package io.mosip.registration.service.security.impl;

import java.io.IOException;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.LoggerConstants;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.service.security.ClientSecurity;
import io.mosip.registration.tpm.asymmetric.RSACipher;
import io.mosip.registration.tpm.sign.Signature;

import tss.Tpm;
import tss.TpmFactory;

public class TPMClientSecurityImpl  implements ClientSecurity {
	
	private static final Logger LOGGER = AppConfig.getLogger(TPMClientSecurityImpl.class);

	private static Tpm tpm;
	
	public TPMClientSecurityImpl() {
		LOGGER.info(LoggerConstants.TPM_CLIENT_SECURITY_IMPL, RegistrationConstants.APPLICATION_ID,
				RegistrationConstants.APPLICATION_NAME, "Getting the instance of platform TPM Security Impl");

		if (tpm == null) {
			LOGGER.info(LoggerConstants.TPM_CLIENT_SECURITY_IMPL, RegistrationConstants.APPLICATION_ID,
					RegistrationConstants.APPLICATION_NAME, "Instantiating the instance of Platform TPM");

			tpm = TpmFactory.platformTpm();
		}

		LOGGER.info(LoggerConstants.TPM_CLIENT_SECURITY_IMPL, RegistrationConstants.APPLICATION_ID,
				RegistrationConstants.APPLICATION_NAME, "Completed getting the instance of platform TPM Security Impl");
	}
	
	@Override
	public byte[] signData(byte[] dataToSign) {
		try {
			LOGGER.info(LoggerConstants.TPM_SERVICE_SIGN, RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, "Signing the data by using TPM");

			return Signature.signData(tpm, dataToSign);
			
		} catch (RuntimeException runtimeException) {
			throw new RegBaseUncheckedException(RegistrationExceptionConstants.TPM_UTIL_SIGN_ERROR.getErrorCode(),
					RegistrationExceptionConstants.TPM_UTIL_SIGN_ERROR.getErrorMessage(), runtimeException);
		}
	}

	/**
	 * Validates the signed data against the actual data using the public part.<br>
	 * The validation requires TSS library but TPM is not required.
	 */
	@Override
	public boolean validateSignature(byte[] signature, byte[] actualData) {
		try {
			LOGGER.info(LoggerConstants.TPM_SERVICE_VALIDATE_SIGN_BY_PUBLIC_PART,
					RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					"Validating the signature using Public Part");

			return Signature.validateSignatureUsingPublicPart(signature, actualData, getSigningPublicPart());
			
		} catch (RuntimeException runtimeException) {
			throw new RegBaseUncheckedException(
					RegistrationExceptionConstants.TPM_UTIL_VALIDATE_SIGN_ERROR.getErrorCode(),
					RegistrationExceptionConstants.TPM_UTIL_VALIDATE_SIGN_ERROR.getErrorMessage(), runtimeException);
		}
	}

	@Override
	public byte[] asymmetricEncrypt(byte[] plainData) {
		try {
			LOGGER.info(LoggerConstants.TPM_SERVICE_ASYMMETRIC_ENCRYPTION, RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, "Encrypting the data by asymmetric algorithm using TPM");

			return RSACipher.encrypt(tpm, plainData);
			
		} catch (RuntimeException runtimeException) {
			throw new RegBaseUncheckedException(
					RegistrationExceptionConstants.TPM_UTIL_ASYMMETRIC_ENCRYPT_ERROR.getErrorCode(),
					RegistrationExceptionConstants.TPM_UTIL_ASYMMETRIC_ENCRYPT_ERROR.getErrorMessage(),
					runtimeException);
		}
	}

	@Override
	public byte[] asymmetricDecrypt(byte[] ciperData) {
		try {
			LOGGER.info(LoggerConstants.TPM_SERVICE_ASYMMETRIC_DECRYPTION, RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, "Decrypting the data by asymmetric algorithm using TPM");

			return RSACipher.decrypt(tpm, ciperData);
			
		} catch (RuntimeException runtimeException) {
			throw new RegBaseUncheckedException(
					RegistrationExceptionConstants.TPM_UTIL_ASYMMETRIC_DECRYPT_ERROR.getErrorCode(),
					RegistrationExceptionConstants.TPM_UTIL_ASYMMETRIC_DECRYPT_ERROR.getErrorMessage(),
					runtimeException);
		}
	}

	@Override
	public byte[] getSigningPublicPart() {
		try {
			LOGGER.info(LoggerConstants.TPM_SERVICE_GET_SIGN_PUBLIC, RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, "Fetching TPM public key");

			return Signature.getKey(tpm).outPublic.toTpm();
			
		} catch (RuntimeException runtimeException) {
			throw new RegBaseUncheckedException(
					RegistrationExceptionConstants.TPM_UTIL_GET_SIGN_KEY_ERROR.getErrorCode(),
					RegistrationExceptionConstants.TPM_UTIL_GET_SIGN_KEY_ERROR.getErrorMessage(), runtimeException);
		}
	}

	@Override
	public void closeSecurityInstance() throws RegBaseCheckedException {
		LOGGER.info(LoggerConstants.LOG_TPM_INITIALIZATION, RegistrationConstants.APPLICATION_ID, RegistrationConstants.APPLICATION_NAME,
				"Closing the instance of Platform TPM");

		try {
			if (tpm != null) {
				tpm.close();
			}
		} catch (IOException ioException) {
			throw new RegBaseCheckedException(RegistrationExceptionConstants.TPM_INIT_CLOSE_TPM_INSTANCE_ERROR.getErrorCode(),
					RegistrationExceptionConstants.TPM_INIT_CLOSE_TPM_INSTANCE_ERROR.getErrorMessage(), ioException);
		}
		LOGGER.info(LoggerConstants.LOG_TPM_INITIALIZATION, RegistrationConstants.APPLICATION_ID, RegistrationConstants.APPLICATION_NAME,
				"Completed closing the instance of Platform TPM");
	}
}
