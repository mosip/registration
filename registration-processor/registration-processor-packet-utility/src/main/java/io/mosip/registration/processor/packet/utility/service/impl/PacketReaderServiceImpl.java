package io.mosip.registration.processor.packet.utility.service.impl;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Autowired;

import io.mosip.kernel.core.fsadapter.spi.FileSystemAdapter;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.packet.utility.constants.LoggerFileConstant;
import io.mosip.registration.processor.packet.utility.exception.PacketDecryptionFailureException;
import io.mosip.registration.processor.packet.utility.exception.PacketDecryptionFailureExceptionConstant;
import io.mosip.registration.processor.packet.utility.logger.PacketUtilityLogger;
import io.mosip.registration.processor.packet.utility.service.PacketDecryptor;
import io.mosip.registration.processor.packet.utility.service.PacketReaderService;
import io.mosip.registration.processor.packet.utility.utils.ZipUtils;


/**
 * The Class PacketReaderServiceImpl.
 *
 * @author Sowmya
 */
public class PacketReaderServiceImpl implements PacketReaderService {

	/** The file system adapter. */
	@Autowired
	private FileSystemAdapter fileSystemAdapter;

	/** The decryptor. */
	@Autowired
	private PacketDecryptor decryptor;

	/** The reg proc logger. */
	private static Logger packetUtilityLogger = PacketUtilityLogger.getLogger(PacketReaderServiceImpl.class);

	/** The Constant PACKET_NOTAVAILABLE_ERROR_DESC. */
	private static final String PACKET_NOTAVAILABLE_ERROR_DESC = "the requested file {} in the destination is not found";

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.processor.packet.utility.service.PacketReaderService#
	 * checkFileExistence(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public boolean checkFileExistence(String id, String fileName, String source)
			throws PacketDecryptionFailureException, IOException {
		packetUtilityLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), id,
				"PacketReaderServiceImpl::checkFileExistence()::entry");
		InputStream decryptedData = getFile(id, source);
		packetUtilityLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), id,
				"PacketReaderServiceImpl::checkFileExistence()::extractZip");
		return ZipUtils.unzipAndCheckIsFileExist(decryptedData, fileName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.processor.packet.utility.service.PacketReaderService#
	 * getFile(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public InputStream getFile(String id, String fileName, String source) throws IOException,
			PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException {
		packetUtilityLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), id,
				"PacketReaderServiceImpl::getFile()::entry");
		InputStream decryptedData = getFile(id, source);
		packetUtilityLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), id,
				"PacketReaderServiceImpl::getFile()::extractZip");
		return ZipUtils.unzipAndGetFile(decryptedData, fileName);
	}

	/**
	 * Gets the file.
	 *
	 * @param id     the id
	 * @param source the source
	 * @return the file
	 * @throws PacketDecryptionFailureException the packet decryption failure
	 *                                          exception
	 * @throws ApisResourceAccessException      the apis resource access exception
	 * @throws IOException                      Signals that an I/O exception has
	 *                                          occurred.
	 */
	private InputStream getFile(String id, String source)
			throws PacketDecryptionFailureException, IOException {
		packetUtilityLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), id,
				"PacketReaderServiceImpl::fileSystemAdapter.getPacket()");
		InputStream data = fileSystemAdapter.getPacket(id);
		if (data == null) {
			throw new io.mosip.registration.processor.packet.utility.exception.FileNotFoundInDestinationException(
					PACKET_NOTAVAILABLE_ERROR_DESC + id);
		}
		packetUtilityLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), id,
				"PacketReaderServiceImpl::getFile()::extractSubfolderZip");

		InputStream sourceFolderInputStream = ZipUtils.unzipAndGetFile(data, id + "_" + source);

		packetUtilityLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), id,
				"PacketReaderServiceImpl::getFile(regid)::decryptor");
		InputStream decryptedData = decryptor.decrypt(sourceFolderInputStream, id);
		if (decryptedData == null) {
			throw new PacketDecryptionFailureException(
					PacketDecryptionFailureExceptionConstant.MOSIP_PACKET_DECRYPTION_FAILURE_ERROR_CODE.getErrorCode(),
					PacketDecryptionFailureExceptionConstant.MOSIP_PACKET_DECRYPTION_FAILURE_ERROR_CODE
							.getErrorMessage());
		}
		return decryptedData;
	}
}
