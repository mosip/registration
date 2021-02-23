package io.mosip.registration.processor.core.packet.dto.packetmanager;

import lombok.Data;

import java.util.List;

@Data
public class BiometricsDto {

    private String type;
    private List<String> subtypes;
}
