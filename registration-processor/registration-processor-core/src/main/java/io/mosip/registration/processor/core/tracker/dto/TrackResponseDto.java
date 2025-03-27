package io.mosip.registration.processor.core.tracker.dto;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "Model representing a tracking regid during process")
public class TrackResponseDto {
    private String regid;
    private String transactionId;
    private String transactionFlowId;
    private boolean isTransactionAllowed;
}
