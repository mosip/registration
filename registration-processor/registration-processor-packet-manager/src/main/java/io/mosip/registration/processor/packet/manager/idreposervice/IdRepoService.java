package io.mosip.registration.processor.packet.manager.idreposervice;

import java.io.IOException;

import org.json.simple.JSONObject;

import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.idrepo.dto.ResponseDTO;

/**
 * The Interface IdRepoService.
 * 
 * @author Nagalakshmi
 */
public interface IdRepoService {

	/**
	 * Gets the uin by rid.
	 *
	 * @param machedRegId
	 *            the mached reg id
	 * @param regProcessorDemographicIdentity
	 *            the reg processor demographic identity
	 * @return the uin by rid
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 */
	String getUinByRid(String machedRegId, String regProcessorDemographicIdentity)
			throws IOException, ApisResourceAccessException;

	/**
	 * Find uin from idrepo.
	 *
	 * @param uin
	 *            the uin
	 * @param regProcessorDemographicIdentity
	 *            the reg processor demographic identity
	 * @return the number
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 */
	public String findUinFromIdrepo(String uin, String regProcessorDemographicIdentity)
			throws IOException, ApisResourceAccessException;

	/**
	 * Gets the id json from ID repo.
	 *
	 * @param machedRegId
	 *            the mached reg id
	 * @param regProcessorDemographicIdentity
	 *            the reg processor demographic identity
	 * @return the id json from ID repo
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 */
	JSONObject getIdJsonFromIDRepo(String machedRegId, String regProcessorDemographicIdentity)
			throws IOException, ApisResourceAccessException;

	/**
	 * Gets the id response from ID repo.
	 *
	 * @param machedRegId
	 *            the mached reg id
	 * 
	 * @return the id response from ID repo
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 */
	ResponseDTO getIdResponseFromIDRepo(String machedRegId) throws IOException, ApisResourceAccessException; 

}
