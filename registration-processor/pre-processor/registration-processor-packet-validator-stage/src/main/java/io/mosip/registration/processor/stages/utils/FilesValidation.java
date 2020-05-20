package io.mosip.registration.processor.stages.utils;

import java.io.IOException;
import java.util.List;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.packetmanager.exception.ApiNotAccessibleException;
import io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException;
import io.mosip.kernel.packetmanager.spi.PacketReaderService;
import io.mosip.registration.processor.core.constant.PacketFiles;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.FieldValueArray;
import io.mosip.registration.processor.core.packet.dto.PacketMetaInfo;

import io.mosip.registration.processor.core.packet.dto.packetvalidator.PacketValidationDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The Class FilesValidation.
 */
@Component
public class FilesValidation {

	/** The Constant FILE_SEPARATOR. */
	public static final String FILE_SEPARATOR = "\\";

	/** The Constant BIOMETRIC_APPLICANT. */
	public static final String BIOMETRIC = PacketFiles.BIOMETRIC.name() + FILE_SEPARATOR;

	/** The PacketReaderService. */
	@Autowired
	private PacketReaderService packetReaderService;

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(FilesValidation.class);


	/**
	 * Files validation.
	 *
	 * @param registrationId
	 *            the registration id
	 * @param packetMetaInfo
	 *            the packetMetaInfo
	 * @return true, if successful
	 * @throws IOException 
	 * @throws ApisResourceAccessException 
	 * @throws PacketDecryptionFailureException 
	 * @throws ApiNotAccessibleException 
	 */
	public boolean filesValidation(String registrationId, PacketMetaInfo packetMetaInfo, String source,
								   PacketValidationDto packetValidationDto) throws PacketDecryptionFailureException, IOException, ApiNotAccessibleException {
		if(packetMetaInfo == null){
			packetValidationDto.setPacketValidaionFailureMessage(StatusMessage.PACKET_FILES_VALIDATION_FAILURE);
			return false;
		}

		boolean filesValidated = false;

		List<FieldValueArray> hashSequence = packetMetaInfo.getIdentity().getHashSequence1();
		boolean isSequence1Validated = validateHashSequence(registrationId, hashSequence, source);

		List<FieldValueArray> hashSequence2 = packetMetaInfo.getIdentity().getHashSequence2();
		boolean isSequence2Validated = validateHashSequence(registrationId, hashSequence2, source);

		if ((!isSequence1Validated) || (!isSequence2Validated)) {
			packetValidationDto.setPacketValidaionFailureMessage(StatusMessage.PACKET_FILES_VALIDATION_FAILURE);
		}

		if (isSequence1Validated && isSequence2Validated) {
			filesValidated = true;
		}

		return filesValidated;
	}
	
	/**
	 * Validate hash sequence.
	 *
	 * @param registrationId
	 *            the registration id
	 * @param hashSequence
	 *            the hash sequence
	 * @return true, if successful
	 * @throws IOException 
	 * @throws ApisResourceAccessException 
	 * @throws PacketDecryptionFailureException 
	 * @throws ApiNotAccessibleException 
	 */
	private boolean validateHashSequence(String registrationId, List<FieldValueArray> hashSequence, String source) throws  IOException, PacketDecryptionFailureException, ApiNotAccessibleException {
		boolean isHashSequenceValidated = false;

		for (FieldValueArray fieldValueArray : hashSequence) {
			
				isHashSequenceValidated = validateFilesExistance(registrationId, fieldValueArray.getValue(), source);
			}

			if (!isHashSequenceValidated) {
				return false;
		}

		return isHashSequenceValidated;
	}


	private boolean validateFilesExistance(String registrationId, List<String> values, String source) throws  IOException, PacketDecryptionFailureException, ApiNotAccessibleException {
		boolean areFilesValidated = true;
		for(String file : values){
			String fileName = file.toUpperCase();
			areFilesValidated = packetReaderService.checkFileExistence(registrationId, fileName, source);
			if (!areFilesValidated) {
				break;
			}
		}

		return areFilesValidated;
	}

}
