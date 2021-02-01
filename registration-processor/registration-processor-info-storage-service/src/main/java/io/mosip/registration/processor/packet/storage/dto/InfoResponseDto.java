package io.mosip.registration.processor.packet.storage.dto;

import lombok.Data;

import java.util.List;

@Data
public class InfoResponseDto {
    private String applicationId;
    private String packetId;
    private String requestToken;
    private List<ContainerInfoDto> info;
}
