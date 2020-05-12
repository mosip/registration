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
import io.mosip.registration.processor.packet.utility.exception.ApiNotAccessibleException;
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
	private PacketReaderService packetReaderService;

	private String source;

	/**
	 * Instantiates a new check sum generation.
	 *
	 * @param adapter
	 *            the adapter
	 */
	public CheckSumGeneration(PacketReaderService packetReaderService,String source) {
		this.packetReaderService = packetReaderService;
		this.source=source;
	}

	

	public byte[] generateHash(List<FieldValueArray> hashSequence, String registrationId) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		for (FieldValueArray fieldValueArray : hashSequence) {
			List<String> hashValues = fieldValueArray.getValue();
			hashValues.forEach(value -> {
				byte[] valuebyte = null;
				try {
					InputStream fileStream = packetReaderService.getFile(registrationId, value.toUpperCase(),source);

					valuebyte = IOUtils.toByteArray(fileStream);
					outputStream.write(valuebyte);
				} catch (IOException | io.mosip.kernel.core.exception.IOException | io.mosip.registration.processor.packet.utility.exception.PacketDecryptionFailureException | ApiNotAccessibleException e) {
					regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.APPLICATIONID.toString(), StatusMessage.INPUTSTREAM_NOT_READABLE,
							e.getMessage() + ExceptionUtils.getStackTrace(e));
				}
			});
		}
		byte[] hashByte = HMACUtils.generateHash(outputStream.toByteArray());

		return HMACUtils.digestAsPlainText(hashByte).getBytes();

	}
		

}
