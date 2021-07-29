package io.mosip.registration.processor.notification.dto;

import io.mosip.registration.processor.core.constant.IdType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;


@Data
@Getter
@Setter
public class MessageSenderDto {

	/** The is template available. */
	private boolean isTemplateAvailable = false;

	/** The sms template code. */
	private String smsTemplateCode = "";

	/** The email template code. */
	private String emailTemplateCode = "";

	/** The subject. */
	private String subjectCode = "";

	/** The id type. */
	private IdType idType = null;
}
