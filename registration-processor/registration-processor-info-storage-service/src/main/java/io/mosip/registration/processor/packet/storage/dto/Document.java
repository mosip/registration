package io.mosip.registration.processor.packet.storage.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class Document {

    private byte[] document;
    private String value;
    private String type;
    private String format;
}
