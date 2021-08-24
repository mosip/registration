package io.mosip.registration.processor.stages.dto;

import lombok.Data;

@Data
public class MessageSenderDTO {
	
	/** The sms template code. */
	private String smsTemplateCode = "";

	/** The email template code. */
	private String emailTemplateCode = "";

	/** The subject. */
	private String subjectTemplateCode = "";

}
