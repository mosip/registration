package io.mosip.registration.processor.stages.dto;

import io.mosip.registration.processor.stages.utils.NotificationSubjectCode;
import io.mosip.registration.processor.stages.utils.NotificationTemplateTypeCode;
import lombok.Data;

@Data
public class MessageSenderDTO {
	
	/** The sms template code. */
	private NotificationTemplateTypeCode smsTemplateCode = null;

	/** The email template code. */
	private NotificationTemplateTypeCode emailTemplateCode = null;

	/** The subject. */
	private NotificationSubjectCode subjectTemplateCode = null;

}
