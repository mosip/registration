package io.mosip.registration.processor.core.exception;

import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;

public class AdditionalInfoIdNotFoundException extends BaseUncheckedException {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    public AdditionalInfoIdNotFoundException() {
        super(PlatformErrorMessages.RPR_PKR_ADDITIONAL_INFOID_NOT_FOUND.getCode(),
                PlatformErrorMessages.RPR_PKR_ADDITIONAL_INFOID_NOT_FOUND.getMessage());
    }

    public AdditionalInfoIdNotFoundException(String message, Throwable cause) {
        super(PlatformErrorMessages.RPR_PKR_ADDITIONAL_INFOID_NOT_FOUND.getCode() + EMPTY_SPACE, message, cause);
    }
}
