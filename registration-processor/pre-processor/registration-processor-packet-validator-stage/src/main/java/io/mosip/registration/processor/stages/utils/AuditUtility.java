package io.mosip.registration.processor.stages.utils;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;

import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.PacketFiles;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.AuditDTO;
import io.mosip.registration.processor.core.packet.dto.AuditRespDTO;
import io.mosip.registration.processor.core.spi.filesystem.manager.PacketManager;
import io.mosip.registration.processor.stages.dto.AsyncRequestDTO;
import io.mosip.registration.processor.stages.helper.RestHelper;
import io.mosip.registration.processor.stages.packet.validator.PacketValidateProcessor;

/**
 * @author Tapaswini Behera M1043226
 *
 */

@Component
public class AuditUtility {

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(PacketValidateProcessor.class);

	@Autowired
	private RestHelper restHelper;

	@Autowired
	private Environment env;

	@Autowired
	private PacketManager fileSystemManager;

	@Autowired
	private ObjectMapper mapper;

	/**
	 * Save the audit Details.
	 *
	 * @param String
	 *            the registrationId
	 * 
	 *
	 */
	@Async
	public void saveAuditDetails(String registrationId) {
		try {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", "AuditUtility::saveAuditDetails()::entry");
			InputStream auditFileInputStream = fileSystemManager.getFile(registrationId, PacketFiles.AUDIT.name());
			CollectionType collectionType = mapper.getTypeFactory().constructCollectionType(List.class, AuditDTO.class);

			List<AuditDTO> regClientAuditDTOs = mapper.readValue(auditFileInputStream, collectionType);
			regClientAuditDTOs.parallelStream().forEach(audit -> {
				AsyncRequestDTO request = buildRequest(audit);
				Supplier<Object> dto = restHelper.requestAsync(request);
				dto.get();

			});
		} catch (RuntimeException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, "AuditUtility::saveAuditDetails()::error");

		} catch (Exception e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, "AuditUtility::saveAuditDetails()::error");
		}

	}

	/**
	 * Builds the request.
	 *
	 * @param restService
	 *            the rest service
	 * @param requestBody
	 *            the request body
	 * @param returnType
	 *            the return type
	 * @return the Async request DTO
	 *
	 */
	public AsyncRequestDTO buildRequest(Object requestBody) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"AuditUtility::buildRequest()::entry" + requestBody);
		RequestWrapper<AuditDTO> auditRequest = new RequestWrapper<>();
		auditRequest.setRequest((AuditDTO) requestBody);
		auditRequest.setId("String");
		auditRequest.setVersion("1.0");
		auditRequest.setRequesttime(LocalDateTime.now());
		AsyncRequestDTO request = new AsyncRequestDTO();

		request.setUri(env.getProperty(PacketFiles.AUDIT.name()));
		request.setHttpMethod(HttpMethod.POST);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		request.setHeaders(headers);
		

		request.setRequestBody(auditRequest);
		request.setResponseType(AuditRespDTO.class);

		return request;
	}
	 
}
