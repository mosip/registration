package io.mosip.registration.processor.stages.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.HMACUtils;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.PacketFiles;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketDecryptionFailureException;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.FieldValueArray;
import io.mosip.registration.processor.packet.utility.service.PacketReaderService;

/**
 * The Class CheckSumGeneration.
 *
 * @author M1048358 Alok Ranjan
 */

public class CheckSumGeneration {

	/** The Constant FILE_SEPARATOR. */
	public static final String FILE_SEPARATOR = "\\";

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(CheckSumGeneration.class);

	/** The adapter. */
	private PacketReaderService adapter;

	private String source;

	/**
	 * Instantiates a new check sum generation.
	 *
	 * @param adapter
	 *            the adapter
	 */
	public CheckSumGeneration(PacketReaderService adapter,String source) {
		this.adapter = adapter;
		this.source=source;
	}

	/**
	 * Generate packet info hash.
	 *
	 * @param hashSequence
	 *            the hash sequence
	 * @param registrationId
	 *            the registration id
	 * @return the byte[]
	 */
	public byte[] generateIdentityHash(List<FieldValueArray> hashSequence, String registrationId) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "CheckSumGeneration::generateIdentityHash::entry");
	    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        for (FieldValueArray fieldValueArray : hashSequence) {

			if (PacketFiles.APPLICANTBIOMETRICSEQUENCE.name().equalsIgnoreCase(fieldValueArray.getLabel())) {

				generateHash(fieldValueArray.getValue(), registrationId, outputStream);

			} else if (PacketFiles.INTRODUCERBIOMETRICSEQUENCE.name().equalsIgnoreCase(fieldValueArray.getLabel())) {

				generateHash(fieldValueArray.getValue(), registrationId, outputStream);

			} else if (PacketFiles.APPLICANTDEMOGRAPHICSEQUENCE.name().equalsIgnoreCase(fieldValueArray.getLabel())) {

				generateHash(fieldValueArray.getValue(), registrationId, outputStream);
			}
		}
        byte[] dataByte = HMACUtils.generateHash(outputStream.toByteArray());
        regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				registrationId, "CheckSumGeneration::generateIdentityHash::exit");
		return HMACUtils.digestAsPlainText(dataByte).getBytes();

	}

	

	/**
	 * Generate demographic hash.
	 *
	 * @param fieldValueArray
	 *            the field value array
	 * @param registrationId
	 *            the registration id
	 */
	private void generateHash(List<String> hashOrder, String registrationId, ByteArrayOutputStream outputStream ) {
		hashOrder.forEach(document -> {
			byte[] fileByte = null;
			try {
				InputStream fileStream = adapter.getFile(registrationId, document.toUpperCase(),source);

                fileByte = IOUtils.toByteArray(fileStream);
				outputStream.write(fileByte);
			} catch (IOException | PacketDecryptionFailureException | ApisResourceAccessException | io.mosip.kernel.core.exception.IOException e) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.APPLICATIONID.toString(), StatusMessage.INPUTSTREAM_NOT_READABLE,
						e.getMessage() + ExceptionUtils.getStackTrace(e));
			}
		});
	}

	public byte[] generatePacketOSIHash(List<FieldValueArray> hashSequence2, String registrationId) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		for (FieldValueArray fieldValueArray : hashSequence2) {
			List<String> hashValues = fieldValueArray.getValue();
			hashValues.forEach(value -> {
				byte[] valuebyte = null;
				try {
					InputStream fileStream = adapter.getFile(registrationId, value.toUpperCase(),source);

					valuebyte = IOUtils.toByteArray(fileStream);
					outputStream.write(valuebyte);
				} catch (IOException | PacketDecryptionFailureException | ApisResourceAccessException | io.mosip.kernel.core.exception.IOException e) {
					regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.APPLICATIONID.toString(), StatusMessage.INPUTSTREAM_NOT_READABLE,
							e.getMessage() + ExceptionUtils.getStackTrace(e));
				}
			});
		}
		byte[] osiByte = HMACUtils.generateHash(outputStream.toByteArray());

		return HMACUtils.digestAsPlainText(osiByte).getBytes();

	}
		

}
