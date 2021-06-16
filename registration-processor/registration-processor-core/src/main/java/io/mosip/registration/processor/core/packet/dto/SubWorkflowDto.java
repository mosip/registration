package io.mosip.registration.processor.core.packet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubWorkflowDto {

    String regId;
    String additionalInfoReqId;
    String process;
    int iteration;
    LocalDateTime timestamp;
    String parentProcess;
    int parentIteration;


}

