package io.mosip.registration.processor.packet.storage.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class DeleteTagRequestDTO {
    private String id;
    private List<String> tagNames;
}
