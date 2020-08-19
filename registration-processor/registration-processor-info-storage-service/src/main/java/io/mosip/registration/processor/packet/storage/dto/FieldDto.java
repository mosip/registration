package io.mosip.registration.processor.packet.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class FieldDto {

    private String id;
    private String field;
    private String source;
    private String process;
    private Boolean bypassCache;
}
