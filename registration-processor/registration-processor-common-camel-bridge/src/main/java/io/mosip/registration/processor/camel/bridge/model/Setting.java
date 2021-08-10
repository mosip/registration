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

	private String ruleId;
	private String matchExpression;
	private long pauseFor;
	private String defaultResumeAction;
	private String fromAddress;
	private String ruleDescription;
}
