package io.mosip.registration.processor.core.exception;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;

public class BiometricRecordValidationException extends BaseCheckedException {

	public BiometricRecordValidationException(String message) {
		super(PlatformErrorMessages.RPR_BIOMETRIC_RECORD_VALIDATION_FAILED.getCode(), message);
	}

	public BiometricRecordValidationException(String code, String message) {
		super(code, message);
	}
}
