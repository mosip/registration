package io.mosip.registration.processor.core.packet.dto.abis;

import java.io.Serializable;
import java.util.Arrays;

import lombok.Data;

@Data
public class Modalities implements Serializable{
/**
	 * 
	 */
	private static final long serialVersionUID = 785216354868291150L;
    private String biometricType;
    private String scaledScore;
    private Analytics analytics;

}
