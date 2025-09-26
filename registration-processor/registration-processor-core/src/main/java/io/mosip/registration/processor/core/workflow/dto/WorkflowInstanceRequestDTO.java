package io.mosip.registration.processor.core.workflow.dto;

import org.json.JSONObject;

import lombok.Data;
@Data
public class WorkflowInstanceRequestDTO {

    private String registrationId;

    private String process;

    private String source;

    private String additionalInfoReqId;

    private NotificationInfoDTO notificationInfo;

}