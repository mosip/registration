package io.mosip.registration.processor.stages.utils;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.packet.storage.dto.FieldResponseDto;
import io.mosip.registration.processor.packet.storage.utils.PacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;
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
	private ObjectMapper mapper;

	@Autowired
	private PriorityBasedPacketManagerService packetManagerService;

	/**
	 * Save the audit Details.
	 *
	 * @param registrationId
	 *            the registrationId
	 * 
	 *
	 */
	@Async
	public void saveAuditDetails(String registrationId, String process) {
		try {
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", "AuditUtility::saveAuditDetails()::entry");
			List<FieldResponseDto> audits = packetManagerService.getAudits(registrationId, process, ProviderStageName.PACKET_VALIDATOR);
			if (CollectionUtils.isNotEmpty(audits)) {
				audits.parallelStream().forEach(audit -> {
					AsyncRequestDTO request = buildRequest(audit);
					Supplier<Object> dto = restHelper.requestAsync(request);
					dto.get();
				});
			}
		} catch (RuntimeException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, "AuditUtility::saveAuditDetails::Runtime exception occurred " + ExceptionUtils.getStackTrace(e));

		} catch (Exception e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					registrationId, "AuditUtility::saveAuditDetails::Exception occurred " + ExceptionUtils.getStackTrace(e));
		}

	}

	/**
	 * Builds the request.
	 *
	 * @param req
	 *            the request body
	 * @return the Async request DTO
	 *
	 */
	public AsyncRequestDTO buildRequest(FieldResponseDto req) {
		RequestWrapper<Map<String, String>> auditRequest = new RequestWrapper<>();

		auditRequest.setRequest(req.getFields());
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
