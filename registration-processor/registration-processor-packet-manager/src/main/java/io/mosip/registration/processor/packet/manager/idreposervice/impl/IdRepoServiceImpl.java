package io.mosip.registration.processor.packet.manager.idreposervice.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.mosip.registration.processor.core.constant.IdType;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.constant.AbisConstant;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.idrepo.dto.IdResponseDTO;
import io.mosip.registration.processor.core.idrepo.dto.ResponseDTO;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService;

/**
 * The Class IdRepoServiceImpl.
 * 
 * @author Nagalakshmi
 * @author Horteppa
 */
@RefreshScope
@Service
public class IdRepoServiceImpl implements IdRepoService {

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(IdRepoServiceImpl.class);

	/** The rest client service. */
	@Autowired
	private RegistrationProcessorRestClientService<Object> restClientService;
	
	@Autowired
	private ObjectMapper mapper;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService#
	 * getUinByRid(java.lang.String, java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public String getUinByRid(String rid, String regProcessorDemographicIdentity)
			throws IOException, ApisResourceAccessException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"IdRepoServiceImpl::getUinByRid()::entry");
		List<String> pathSegments = new ArrayList<>();
		pathSegments.add(rid);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), rid,
				"IdRepoServiceImpl::getUinByRid()::exit");
		return getUin(pathSegments, regProcessorDemographicIdentity, IdType.RID);

	}

	/**
	 * Gets the uin.
	 *
	 * @param pathSegments
	 *            the path segments
	 * @param regProcessorDemographicIdentity
	 *            the reg processor demographic identity
	 * @return the uin
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 */
	@SuppressWarnings("unchecked")
	private String getUin(List<String> pathSegments, String regProcessorDemographicIdentity, IdType idType)
			throws IOException, ApisResourceAccessException {
		@SuppressWarnings("unchecked")
		ResponseWrapper<IdResponseDTO> response;
		ApiName apiName;
		if (idType.equals(IdType.RID))
			apiName = ApiName.RETRIEVEIDENTITYFROMRID;
		else if (idType.equals(IdType.UIN))
			apiName = ApiName.IDREPOGETIDBYUIN;
		else if (idType.equals(IdType.VID))
			apiName = ApiName.GETUINBYVID;
		else
			apiName = ApiName.RETRIEVEIDENTITY;

		response = (ResponseWrapper<IdResponseDTO>) restClientService.getApi(apiName, pathSegments, "", "",
				ResponseWrapper.class);

		if (response.getResponse() != null) {
			Gson gsonObj = new Gson();
			String jsonString = gsonObj.toJson(response.getResponse());
			JSONObject identityJson = JsonUtil.objectMapperReadValue(jsonString, JSONObject.class);
			JSONObject demographicIdentity = JsonUtil.getJSONObject(identityJson, regProcessorDemographicIdentity);
			return JsonUtil.getJSONValue(demographicIdentity, AbisConstant.UIN);
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService#
	 * findUinFromIdrepo(java.lang.String, java.lang.String)
	 */
	@Override
	public String findUinFromIdrepo(String uin, String regProcessorDemographicIdentity)
			throws IOException, ApisResourceAccessException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), "",
				"IdRepoServiceImpl::findUinFromIdrepo()::entry");

		List<String> pathSegments = new ArrayList<>();
		pathSegments.add(uin);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.UIN.toString(), "",
				"IdRepoServiceImpl::findUinFromIdrepo()::exit");

		return getUin(pathSegments, regProcessorDemographicIdentity, IdType.UIN);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService#
	 * getIdJsonFromIDRepo(java.lang.String, java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public JSONObject getIdJsonFromIDRepo(String machedRegId, String regProcessorDemographicIdentity)
			throws IOException, ApisResourceAccessException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				machedRegId, "IdRepoServiceImpl::getIdJsonFromIDRepo()::entry");

		List<String> pathSegments = new ArrayList<>();
		pathSegments.add(machedRegId);
		JSONObject demographicJsonObj = null;

		@SuppressWarnings("unchecked")
		ResponseWrapper<IdResponseDTO> response;

		response = (ResponseWrapper<IdResponseDTO>) restClientService.getApi(ApiName.RETRIEVEIDENTITYFROMRID, pathSegments, "", "",
				ResponseWrapper.class);

		if (response.getResponse() != null) {
			Gson gsonObj = new Gson();
			String jsonString = gsonObj.toJson(response.getResponse());
			JSONObject identityJson = JsonUtil.objectMapperReadValue(jsonString, JSONObject.class);
			demographicJsonObj = JsonUtil.getJSONObject(identityJson, regProcessorDemographicIdentity);

		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				machedRegId, "IdRepoServiceImpl::getIdJsonFromIDRepo()::exit");

		return demographicJsonObj;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.processor.packet.manager.idreposervice.IdRepoService#
	 * getIdResponseFromIDRepo(java.lang.String, java.lang.String)
	 */
	@Override
	public ResponseDTO getIdResponseFromIDRepo(String machedRegId) throws IOException, ApisResourceAccessException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				machedRegId, "IdRepoServiceImpl::getIdResponseFromIDRepo()::entry");
		ResponseDTO responseDTO=null;
		List<String> pathSegments1 = new ArrayList<>();
		pathSegments1.add(machedRegId);
		@SuppressWarnings("unchecked")
		ResponseWrapper<ResponseDTO> response=(ResponseWrapper<ResponseDTO>) restClientService.getApi(ApiName.IDREPOGETIDBYUIN, pathSegments1, "type", "ALL",
				ResponseWrapper.class);

		if (response.getResponse() != null) {
			responseDTO=mapper.readValue(mapper.writeValueAsString(response.getResponse()), ResponseDTO.class);

		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				machedRegId, "IdRepoServiceImpl::getIdResponseFromIDRepo()::exit");

		return responseDTO;
	}
}
