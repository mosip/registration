package io.mosip.registration.processor.stages.packetclassifier.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * This DTO class will contain data type of the field and value of the field in strigified JSON format
 */
@Data
@AllArgsConstructor
public class FieldDTO {

    /** Id schema type of the field */
    String type;

    /** Value of the field in a Stringified JSON format */
    String value;
    
}
