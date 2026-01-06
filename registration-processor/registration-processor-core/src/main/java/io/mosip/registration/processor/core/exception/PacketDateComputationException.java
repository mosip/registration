package io.mosip.registration.processor.core.exception;

import io.mosip.kernel.core.exception.BaseUncheckedException;

/**
 * The Class PacketDateComputationException.
 */
public class PacketDateComputationException extends BaseUncheckedException {
    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new PacketDateComputation exception.
     *
     * @param errorCode    the error code
     * @param errorMessage the error message
     */
    public PacketDateComputationException(String errorCode, String errorMessage) {
        super(errorCode+ EMPTY_SPACE, errorMessage);
    }
}
