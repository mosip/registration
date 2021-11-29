package io.mosip.registration.processor.verification.service;

import io.mosip.registration.processor.core.queue.factory.MosipQueue;
import io.mosip.registration.processor.verification.response.dto.VerificationResponseDTO;
import org.springframework.stereotype.Service;

import io.mosip.registration.processor.core.abstractverticle.MessageDTO;

/**
 * Interface for Manual Verification Services.
 *
 * @author Pranav Kumar
 * @author Shuchita
 * @since 0.0.1
 */
@Service
public interface VerificationService {

	/**
	 * This method updates the Manual Verification status of a regId according to
	 * decision taken by manual verifier.
	 *
	 * @param resp
	 *            {@link VerificationResponseDTO}
	 * @return The updated {@link VerificationResponseDTO}
	 */
	public boolean updatePacketStatus(VerificationResponseDTO resp, String stageName, MosipQueue queue);

	public MessageDTO process(MessageDTO object, MosipQueue queue, String stageName);

	
	
}
