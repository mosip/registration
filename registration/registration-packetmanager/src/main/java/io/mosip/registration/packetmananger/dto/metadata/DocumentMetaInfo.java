package io.mosip.registration.packetmananger.dto.metadata;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocumentMetaInfo {
	private String documentName;
	private String documentCategory;
	private String documentOwner;
	private String documentType;
}
