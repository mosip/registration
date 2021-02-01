package io.mosip.registration.processor.packet.storage.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@EqualsAndHashCode
public class ContainerInfoDto {

    private String source;
    private String process;
    private Date lastModified;
    private Set<String> demographics;
    private List<BiometricsDto> biometrics;
    private Map<String, String> documents;
    private Map<String, String> tags;

}
