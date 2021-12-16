package io.mosip.registration.processor.adjudication.service;

import io.mosip.registration.processor.core.queue.factory.MosipQueue;
import org.springframework.stereotype.Service;

import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.adjudication.response.dto.ManualAdjudicationResponseDTO;

/**
 * Interface for Manual Verification Services.
 *
 * @author Pranav Kumar
 * @author Shuchita
 * @since 0.0.1
 */
@Service
public interface ManualAdjudicationService {

	/**
	 * This method updates the Manual Verification status of a regId according to
	 * decision taken by manual verifier.
	 *
	 * @param resp
	 *            {@link ManualAdjudicationResponseDTO}
	 * @return The updated {@link ManualAdjudicationResponseDTO}
	 */
	public boolean updatePacketStatus(ManualAdjudicationResponseDTO resp, String stageName, MosipQueue queue);
	
	/**
	 * This method receives response from the queue and saves to DB
	 *
	 * @param res
	 *        {@link ManualAdjudicationResponseDTO}
	 *
	 */
	public  void saveToDB(ManualAdjudicationResponseDTO res);

	public MessageDTO process(MessageDTO object, MosipQueue queue);

	
	
}
