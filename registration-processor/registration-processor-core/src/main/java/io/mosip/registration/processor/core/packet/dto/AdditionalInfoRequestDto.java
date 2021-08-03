package io.mosip.registration.processor.core.packet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalInfoRequestDto {

    String additionalInfoReqId;
    String workflowInstanceId;
    String regId;
    String additionalInfoProcess;
    int additionalInfoIteration;
    LocalDateTime timestamp;

}

