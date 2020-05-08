package io.mosip.registration.processor.packet.utility.service;

import java.io.IOException;
import java.io.InputStream;

import io.mosip.registration.processor.packet.utility.exception.PacketDecryptionFailureException;



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
	 * @throws IOException                      Signals that an I/O exception has
	 *                                          occurred.
	 */
	public boolean checkFileExistence(String id, String fileName, String source)
			throws PacketDecryptionFailureException, IOException;

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
	 */
	public InputStream getFile(String id, String fileName, String source) throws IOException,
			PacketDecryptionFailureException, io.mosip.kernel.core.exception.IOException;

	/**
	 * Get the encrypted source packet from the parent zip
	 *
	 * @param rid : The rid
	 * @param inputStream : parent zip input stream
	 * @param source : the source (EX - id, evidence, optional)
	 * @return : source zip
	 * @throws IOException
	 */
	public InputStream getEncryptedSourcePacket(String rid, InputStream inputStream, String source) throws IOException;
}
