package io.mosip.registration.processor.core.abstractverticle;

import lombok.Data;

@Data
public class HealthCheckDTO {

	boolean eventBusConnected;

	String failureReason;

}
