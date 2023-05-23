package io.mosip.registration.processor.packet.uploader.service;

import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import org.springframework.stereotype.Service;

/**
 * This service is used to store the registration packets to virus scanner zone,
 * check duplicate packets etc.
 *
 * @param <U>
 *            Return type of operations
 */
@Service
public interface PacketUploaderService<U> {

	/**
	 * Stores registration packets to virus scanner zone.
	 *
	 * @param regId
	 *            the registration id
	 * @param stageName
	 * 		      The name of the current stage
	 * @return the u
	 */
	public U validateAndUploadPacket(MessageDTO messageDTO, String stageName);
	
}
