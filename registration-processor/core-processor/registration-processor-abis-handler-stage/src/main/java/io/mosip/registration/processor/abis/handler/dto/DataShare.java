package io.mosip.registration.processor.abis.handler.dto;

import lombok.Data;

@Data
public class DataShare {
    /** The url. */
    private String url;

    /** The valid for in minutes. */
    private int validForInMinutes;

    /** The transactions allowed. */
    private int transactionsAllowed;


    /** The policy id. */
    private String policyId;

    /** The subscriber id. */
    private String subscriberId;

    /** The signature. */
    private String signature;
}
