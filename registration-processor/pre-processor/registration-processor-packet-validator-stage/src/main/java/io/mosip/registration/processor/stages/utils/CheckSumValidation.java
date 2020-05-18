package io.mosip.registration.processor.stages.utils;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.List;

import io.mosip.kernel.packetmanager.exception.ApiNotAccessibleException;
import io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException;
import io.mosip.kernel.packetmanager.spi.PacketReaderService;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.PacketValidationDto;
import org.apache.commons.io.IOUtils;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.PacketFiles;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.FieldValueArray;
import io.mosip.registration.processor.core.packet.dto.Identity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The Class CheckSumValidation.
 *
 * @author M1048358 Alok Ranjan
 */
@Component
public class CheckSumValidation {

	/** The PacketReaderService. */
	@Autowired
	private PacketReaderService packetReaderService;

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(CheckSumValidation.class);

	/**
	 * Checksumvalidation.
	 *
	 * @param registrationId
	 *            the registration id
	 * @param identity
	 *            the identity
	 * @return true, if successful
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws io.mosip.kernel.core.exception.IOException 
	 * @throws ApisResourceAccessException 
	 * @throws PacketDecryptionFailureException 
	 * @throws ApiNotAccessibleException 
	 */
	public boolean checksumvalidation(String registrationId, Identity identity, String source, PacketValidationDto packetValidationDto) throws IOException, io.mosip.kernel.core.exception.IOException, PacketDecryptionFailureException, ApiNotAccessibleException {
		List<FieldValueArray> hashSequence1 = identity.getHashSequence1();
		List<FieldValueArray> hashSequence2 = identity.getHashSequence2();
        regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "CheckSumValidation::checksumvalidation::entry");
		Boolean isValid = false;

		// Getting hash bytes from packet
		InputStream dataHashStream = packetReaderService.getFile(registrationId, PacketFiles.PACKET_DATA_HASH.name(), source);
		InputStream operationsHashStream = packetReaderService.getFile(registrationId, PacketFiles.PACKET_OPERATIONS_HASH.name(), source);

		byte[] dataHashByte = IOUtils.toByteArray(dataHashStream);
		byte[] operationsHashByte = IOUtils.toByteArray(operationsHashStream);

		// Generating hash bytes using files
		CheckSumGeneration checkSumGeneration = new CheckSumGeneration(packetReaderService,source);
		byte[] dataHash = checkSumGeneration.generateHash(hashSequence1, registrationId);
		
		byte[] operationsHash = checkSumGeneration.generateHash(hashSequence2, registrationId);

		Boolean isdataCheckSumEqual = MessageDigest.isEqual(dataHash, dataHashByte);
		Boolean isoperationsCheckSumEqual = MessageDigest.isEqual(operationsHash, operationsHashByte);

		if ((!isdataCheckSumEqual) || (!isoperationsCheckSumEqual)) {
			packetValidationDto.setPacketValidaionFailureMessage(StatusMessage.PACKET_CHECKSUM_VALIDATION_FAILURE);
		}

		if (isdataCheckSumEqual && isoperationsCheckSumEqual) {
			isValid = true;
		}
	    regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, "CheckSumValidation::checksumvalidation::exit");
		return isValid;
	}

}
