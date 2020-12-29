package io.mosip.registration.processor.manual.verification.service;

import java.io.IOException;

import org.springframework.stereotype.Service;

import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.manual.verification.dto.ManualVerificationDTO;
import io.mosip.registration.processor.manual.verification.dto.ManualVerificationDecisionDto;
import io.mosip.registration.processor.manual.verification.dto.UserDto;
import io.mosip.registration.processor.manual.verification.response.dto.ManualAdjudicationResponseDTO;

/**
 * Interface for Manual Verification Services.
 *
 * @author Pranav Kumar
 * @author Shuchita
 * @since 0.0.1
 */
@Service
public interface ManualVerificationService {

	/**
	 * This method assigns earliest created Reg Id to a manual verification user.
	 *
	 * @param dto
	 *            The {@link UserDto} to whom a regId needs to be assigned
	 * @return {@link ManualVerificationDTO}
	 */

	public ManualVerificationDTO assignApplicant(UserDto dto);

	/**
	 * This method returns a file related to a regId.
	 *
	 * @param regId
	 *            The registration ID
	 * @param fileName
	 *            The file required
	 * @return The file as bytes
	 * @throws java.io.IOException 
	 * @throws IOException 
	 * @throws ApisResourceAccessException 
	 */
	public byte[] getApplicantFile(String regId, String fileName, String source) throws IOException, ApisResourceAccessException, PacketManagerException, JsonProcessingException;

	/**
	 * This method updates the Manual Verification status of a regId according to
	 * decision taken by manual verifier.
	 *
	 * @param manualVerificationDTO
	 *            {@link ManualVerificationDTO}
	 * @return The updated {@link ManualVerificationDTO}
	 */
	public ManualVerificationDecisionDto updatePacketStatus(ManualVerificationDecisionDto manualVerificationDTO, String stageName);

	   /**
     * This method receives refId from Bio dedupe ,finds matched refeID's forms request and 
     * sends to queue
     * 
     * @Param refId
     *          reference Id 
     */
	public void pushRequestToQueue(String refId) throws Exception;
	
	
	/**
	 * This method receives response from the queue and saves to DB
	 * 
	 * @param res
	 *        {@link ManualAdjudicationResponseDTO}
	 * 
	 */
	public  void saveToDB(ManualAdjudicationResponseDTO res);

	public MessageDTO process(MessageDTO object);

	
	
}
