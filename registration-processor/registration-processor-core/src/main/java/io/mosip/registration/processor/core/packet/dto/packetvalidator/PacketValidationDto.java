package io.mosip.registration.processor.core.packet.dto.packetvalidator;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class PacketValidationDto {
	private boolean isSchemaValidated = false;
	private boolean isCheckSumValidated = false;
	private boolean isApplicantDocumentValidation = false;
	private boolean isFilesValidated = false;
	private boolean isMasterDataValidation = false;
	private boolean isMandatoryValidation = false;
	private boolean isRIdAndTypeSynched = false;
	private boolean isTransactionSuccessful;
	private String packetValidatonStatusCode="";

	// Should be set true/false by country when reference validation is implemented
	// by default set as true i,e if no reference validation is provided then its ignored.
	// the packetValidaionFailureMessage should also be updated in case of validation failure
	private boolean isReferenceValidated=true;
	private String packetValidaionFailureMessage = "";
}
