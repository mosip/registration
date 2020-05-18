package io.mosip.registration.processor.stages.config;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;

import javax.annotation.PostConstruct;

import io.mosip.kernel.packetmanager.impl.PacketReaderServiceImpl;
import io.mosip.kernel.packetmanager.spi.PacketReaderService;
import io.mosip.registration.processor.stages.utils.CheckSumValidation;
import io.mosip.registration.processor.stages.utils.FilesValidation;
import io.mosip.registration.processor.stages.utils.MandatoryValidation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

import io.mosip.kernel.core.idobjectvalidator.constant.IdObjectValidatorSupportedOperations;
import io.mosip.kernel.core.idobjectvalidator.exception.IdObjectIOException;
import io.mosip.kernel.core.idobjectvalidator.exception.IdObjectValidationFailedException;
import io.mosip.kernel.core.idobjectvalidator.spi.IdObjectValidator;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.registration.processor.core.packet.dto.applicantcategory.ApplicantTypeDocument;

import io.mosip.registration.processor.rest.client.utils.RestApiClient;
import io.mosip.registration.processor.stages.helper.RestHelper;
import io.mosip.registration.processor.stages.helper.RestHelperImpl;
import io.mosip.registration.processor.stages.packet.validator.PacketValidateProcessor;
import io.mosip.registration.processor.stages.packet.validator.PacketValidatorStage;
import io.mosip.registration.processor.stages.utils.AuditUtility;
import io.mosip.registration.processor.stages.utils.DocumentUtility;
import io.mosip.registration.processor.stages.utils.IdObjectsSchemaValidationOperationMapper;
import io.mosip.registration.processor.stages.utils.RestTemplateInterceptor;
import io.mosip.registration.processor.core.spi.packet.validator.PacketValidator;
import io.mosip.registration.processor.stages.validator.impl.PacketValidatorImpl;

@Configuration
public class ValidatorConfig {

	public static final String IDOBJECT_PROVIDER = "idobject.validator.provider";
	public static final String PACKET_VALIDATOR_PROVIDER = "packet.validator.provider";

	@Autowired
	private RestApiClient restApiClient;

	@Autowired
	private Environment env;

	@Bean
	public PacketValidatorStage getPacketValidatorStage() {
		return new PacketValidatorStage();
	}

	@Bean
	public FilesValidation filesValidation() {
		return new FilesValidation();
	}

	@Bean
	public MandatoryValidation mandatoryValidation() {
		return new MandatoryValidation();
	}

	@Bean
	public CheckSumValidation checkSumValidation() {
		return new CheckSumValidation();
	}

	@Bean
	public DocumentUtility getDocumentUtility() {
		return new DocumentUtility();
	}

	@Bean
	public PacketValidateProcessor getPacketValidateProcessor() {
		return new PacketValidateProcessor();
	}

	@Bean
	public IdObjectsSchemaValidationOperationMapper getIdObjectsSchemaValidationOperationMapper() {
		return new IdObjectsSchemaValidationOperationMapper();
	}

	@Bean
	public RestApiClient getRestApiClient() {
		return new RestApiClient();
	}

	@Bean
	public ApplicantTypeDocument getApplicantTypeDocument() {
		return new ApplicantTypeDocument();
	}

	@Bean
	public RestTemplate restTemplate() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setInterceptors(Collections.singletonList(new RestTemplateInterceptor(restApiClient)));
		return restTemplate;
	}
	
	@Bean
	public RestHelper getRestHelper() {
		return new RestHelperImpl();
	}
	
	@Bean
	public AuditUtility getAuditUtility() {
		return new AuditUtility();
	}

	@PostConstruct
	public void validateReferenceValidator() throws ClassNotFoundException {
		if (StringUtils.isNotBlank(env.getProperty(IDOBJECT_PROVIDER))) {
			Class.forName(env.getProperty(IDOBJECT_PROVIDER));
		}
	}
	
	@Bean
	public PacketReaderService getPacketReaderService() {
		return new PacketReaderServiceImpl();
	}
	
	@Bean
	public PacketValidator referencePacketValidator() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		if (StringUtils.isNotBlank(env.getProperty(PACKET_VALIDATOR_PROVIDER))) {
			return (PacketValidator) Class.forName(env.getProperty(PACKET_VALIDATOR_PROVIDER)).newInstance();
		} else {
			return new PacketValidatorImpl();
		}
	}

	@Bean
	@Lazy
	public IdObjectValidator referenceValidator()
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		if (StringUtils.isNotBlank(env.getProperty(IDOBJECT_PROVIDER))) {
			return (IdObjectValidator) Class.forName(env.getProperty(IDOBJECT_PROVIDER)).newInstance();
		} else {

			return new IdObjectValidator() {

				@Override
				public boolean validateIdObject(Object identityObject, IdObjectValidatorSupportedOperations operation)
						throws IdObjectValidationFailedException, IdObjectIOException {
					// TODO Auto-generated method stub
					return true;
				}
			};
		}
	}
}
