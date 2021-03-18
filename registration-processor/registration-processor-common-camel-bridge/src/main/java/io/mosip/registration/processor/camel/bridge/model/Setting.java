package io.mosip.registration.processor.camel.bridge.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Setting {

	private String hotlistedReason;
	private long pauseFor;
	private String defaultResumeAction;
	private String fromAddress;
}
