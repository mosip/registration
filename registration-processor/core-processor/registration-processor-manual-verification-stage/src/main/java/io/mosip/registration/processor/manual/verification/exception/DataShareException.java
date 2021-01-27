package io.mosip.registration.processor.manual.verification.exception;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;

public class DataShareException extends BaseCheckedException {

    public DataShareException(String message) {
        super(PlatformErrorMessages.DATASHARE_ERROR.getCode(), message);
    }
}
