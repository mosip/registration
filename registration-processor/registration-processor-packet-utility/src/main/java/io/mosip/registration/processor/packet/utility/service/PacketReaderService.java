package io.mosip.registration.processor.packet.utility.service;

import java.io.IOException;
import java.io.InputStream;

import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketDecryptionFailureException;

/**
 * The Interface PacketReaderService.
 * 
 * @author Sowmya
 */
public interface PacketReaderService {

	/**
	 * Check file existence.
	 *
	 * @param id       the id
	 * @param fileName the file name
	 * @param source   the source
	 * @return true, if successful
	 * @throws PacketDecryptionFailureException the packet decryption failure
	 *                                          exception
	 * @throws ApisResourceAccessException      the apis resource access exception
	 * @throws IOException                      Signals that an I/O exception has
	 *                                          occurred.
	 */
	public boolean checkFileExistence(String id, String fileName, String source)
			throws PacketDecryptionFailureException, ApisResourceAccessException, IOException;

	/**
	 * Gets the file.
	 *
	 * @param id       the id
	 * @param fileName the file name
	 * @param source   the source
	 * @return the file
	 * @throws IOException                      Signals that an I/O exception has
	 *                                          occurred.
	 * @throws IOException                      Signals that an I/O exception has
	 *                                          occurred.
	 * @throws PacketDecryptionFailureException the packet decryption failure
	 *                                          exception
	 * @throws ApisResourceAccessException      the apis resource access exception
	 */
	public InputStream getFile(String id, String fileName, String source) throws IOException,
			PacketDecryptionFailureException, ApisResourceAccessException, io.mosip.kernel.core.exception.IOException;
}
