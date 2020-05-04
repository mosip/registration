package io.mosip.registration.dto.response;


import java.time.LocalDateTime;
import java.util.List;

import io.mosip.registration.dto.UiSchemaDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SchemaDto {

	private String id;
	private double idVersion;
	private List<UiSchemaDTO> schema;
	private String schemaJson;
	private LocalDateTime effectiveFrom;
}
