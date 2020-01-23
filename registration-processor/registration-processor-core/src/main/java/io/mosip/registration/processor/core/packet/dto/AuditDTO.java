package io.mosip.registration.processor.core.packet.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This class is to capture the time duration for each event
 * 
 * @author Dinesh Asokan
 * @since 1.0.0
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class AuditDTO extends BaseDTO {
	protected String uuid;
	protected LocalDateTime createdAt;
	protected String eventId;
	protected String eventName;
	protected String eventType;
	protected LocalDateTime actionTimeStamp;
	protected String hostName;
	protected String hostIp;
	protected String applicationId;
	protected String applicationName;
	protected String sessionUserId;
	protected String sessionUserName;
	protected String id;
	protected String idType;
	protected String createdBy;
	protected String moduleName;
	protected String moduleId;
	protected String description;

}