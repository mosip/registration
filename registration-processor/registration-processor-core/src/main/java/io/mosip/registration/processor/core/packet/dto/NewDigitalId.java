package io.mosip.registration.processor.core.packet.dto;

import lombok.Data;

/**
 * Instantiates a new digital id dto.
 */
@Data
public class NewDigitalId {

    private String serialNo;
    private String make;
    private String model;
    private String type;
    private String deviceSubType;
    private String deviceProviderId;
    private String deviceProvider;
    private String dateTime;

}
