package io.mosip.registration.processor.stages.utils;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.List;

import io.mosip.kernel.packetmanager.exception.ApiNotAccessibleException;
import io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException;
import io.mosip.kernel.packetmanager.spi.PacketReaderService;
import org.apache.commons.io.IOUtils;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.PacketFiles;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.FieldValueArray;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;

/**
 * The Class CheckSumValidation.
 *
 * @author M1048358 Alok Ranjan
 */

public class CheckSumValidation {

	/** The adapter. */
	private PacketReaderService packetReaderService;

	/** The registration status dto. */
	private InternalRegistrationStatusDto registrationStatusDto;

	private String source;
	
	
	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(CheckSumValidation.class);

	/**
	 * Instantiates a new check sum validation.
	 *
	 * @param adapter
	 *            the adapter
	 * @param registrationStatusDto
	 *            the registration status dto
	 */
	public CheckSumValidation(PacketReaderService packetReaderService, InternalRegistrationStatusDto registrationStatusDto,
			String source) {
		this.registrationStatusDto = registrationStatusDto;
		this.packetReaderService = packetReaderService;
		this.source=source;

	}

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
	public boolean checksumvalidation(String registrationId, Identity identity) throws IOException, ApisResourceAccessException, io.mosip.kernel.core.exception.IOException, PacketDecryptionFailureException, ApiNotAccessibleException {
		List<FieldValueArray> hashSequence1 = identity.getHashSequence1();
		List<FieldValueArray> hashSequence2 = identity.getHashSequence2();
        regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "CheckSumValidation::checksumvalidation::entry");
		Boolean isValid = false;

		// Getting hash bytes from packet
		InputStream dataHashStream = packetReaderService.getFile(registrationId, PacketFiles.PACKET_DATA_HASH.name(),source);
		InputStream operationsHashStream = packetReaderService.getFile(registrationId, PacketFiles.PACKET_OPERATIONS_HASH.name(),source);

		byte[] dataHashByte = IOUtils.toByteArray(dataHashStream);
		byte[] operationsHashByte = IOUtils.toByteArray(operationsHashStream);

		// Generating hash bytes using files
		CheckSumGeneration checkSumGeneration = new CheckSumGeneration(packetReaderService,source);
		byte[] dataHash = checkSumGeneration.generateHash(hashSequence1, registrationId);
		
		byte[] operationsHash = checkSumGeneration.generateHash(hashSequence2, registrationId);

		Boolean isdataCheckSumEqual = MessageDigest.isEqual(dataHash, dataHashByte);
		Boolean isoperationsCheckSumEqual = MessageDigest.isEqual(operationsHash, operationsHashByte);

		if ((!isdataCheckSumEqual) || (!isoperationsCheckSumEqual)) {
			registrationStatusDto.setStatusComment(StatusMessage.PACKET_CHECKSUM_VALIDATION_FAILURE);
		}

		if (isdataCheckSumEqual && isoperationsCheckSumEqual) {
			isValid = true;
		}
	    regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, "CheckSumValidation::checksumvalidation::exit");
		return isValid;
	}

}
