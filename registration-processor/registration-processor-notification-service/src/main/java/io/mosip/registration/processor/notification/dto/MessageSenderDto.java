package io.mosip.registration.processor.notification.dto;

import io.mosip.registration.processor.core.constant.IdType;
import io.mosip.registration.processor.message.sender.utility.NotificationTemplateCode;
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
	private NotificationTemplateCode smsTemplateCode = null;

	/** The email template code. */
	private NotificationTemplateCode emailTemplateCode = null;

	/** The subject. */
	private String subject = "";

	/** The id type. */
	private IdType idType = null;
}
