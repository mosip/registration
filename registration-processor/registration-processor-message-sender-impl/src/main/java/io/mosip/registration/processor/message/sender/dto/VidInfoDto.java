package io.mosip.registration.processor.message.sender.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VidInfoDto {

    private String vid;
    private String vidType;
    private LocalDateTime expiryTimestamp;
    private Integer transactionLimit;
    private Map<String, String> hashAttributes;

}
