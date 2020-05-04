package io.mosip.registration.processor.packet.utility.service.impl;

import java.io.IOException;
import java.io.InputStream;

import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketDecryptionFailureException;
import io.mosip.registration.processor.packet.utility.service.PacketReaderService;

/**
 * The Class PacketReaderServiceImpl.
 */
public class PacketReaderServiceImpl implements PacketReaderService {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.processor.packet.utility.service.PacketReaderService#
	 * checkFileExistence(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public boolean checkFileExistence(String id, String fileName, String source)
			throws PacketDecryptionFailureException, ApisResourceAccessException, IOException {
		// TODO Auto-generated method stub
		return false;
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
			PacketDecryptionFailureException, ApisResourceAccessException, io.mosip.kernel.core.exception.IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
