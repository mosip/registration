package io.mosip.registration.processor.packet.storage.exception;

import io.mosip.kernel.core.exception.BaseUncheckedException;

/**
 * The Class PacketDateComputationException.
 */
public class PacketDateComputationException extends BaseUncheckedException {
    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new PacketCreationDate not found exception.
     *
     * @param errorMessage the error message
     */
    public PacketDateComputationException(String errorCode, String errorMessage) {
        super(errorCode+ EMPTY_SPACE, errorMessage);
    }
}
