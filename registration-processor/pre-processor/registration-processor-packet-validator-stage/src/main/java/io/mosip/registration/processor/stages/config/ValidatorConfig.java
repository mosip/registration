package io.mosip.registration.processor.stages.config;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;

import javax.annotation.PostConstruct;

import io.mosip.registration.processor.stages.utils.ApplicantDocumentValidation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.applicantcategory.ApplicantTypeDocument;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.PacketValidationDto;
import io.mosip.registration.processor.core.spi.packet.validator.PacketValidator;
import io.mosip.registration.processor.message.sender.template.TemplateGenerator;
import io.mosip.registration.processor.rest.client.utils.RestApiClient;
import io.mosip.registration.processor.stages.helper.RestHelper;
import io.mosip.registration.processor.stages.helper.RestHelperImpl;
import io.mosip.registration.processor.stages.packet.validator.PacketValidateProcessor;
import io.mosip.registration.processor.stages.packet.validator.PacketValidatorStage;
import io.mosip.registration.processor.stages.utils.AuditUtility;
import io.mosip.registration.processor.stages.utils.MandatoryValidation;
import io.mosip.registration.processor.stages.utils.MasterDataValidation;
import io.mosip.registration.processor.stages.utils.NotificationUtility;
import io.mosip.registration.processor.stages.utils.RestTemplateInterceptor;
import io.mosip.registration.processor.stages.validator.impl.CompositePacketValidator;
import io.mosip.registration.processor.stages.validator.impl.PacketValidatorImpl;

@Configuration
public class ValidatorConfig {

	public static final String PACKET_VALIDATOR_PROVIDER = "packet.validator.provider";
	
    private static Logger logger = RegProcessorLogger.getLogger(ValidatorConfig.class);

	@Autowired
	private RestApiClient restApiClient;

	@Autowired
	private Environment env;

	@Bean
	public PacketValidatorStage getPacketValidatorStage() {
		return new PacketValidatorStage();
	}

	@Bean
	public MandatoryValidation mandatoryValidation() {
		return new MandatoryValidation();
	}

	@Bean
	public MasterDataValidation masterDataValidation() {
		return new MasterDataValidation();
	}

	@Bean
	public ApplicantDocumentValidation applicantDocumentValidation() {
		return new ApplicantDocumentValidation();
	}

	@Bean
	public PacketValidateProcessor getPacketValidateProcessor() {
		return new PacketValidateProcessor();
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

	@Bean
	public CompositePacketValidator compositePacketValidator() {
		return new CompositePacketValidator();
	}

	@Bean
	public NotificationUtility notificationUtility() {
		return new NotificationUtility();
	}
	
	@Bean
	public TemplateGenerator getTemplateGenerator() {
		return new TemplateGenerator();
	}
	
/*	@Bean
	@Primary
	public IdObjectValidator idObjectCompositeValidator() {
		return new IdObjectCompositeValidator();
	}

	@Bean
	public IdObjectPatternValidator idObjectPatternValidator() {
		IdObjectPatternValidator idObjectPatternValidator = new IdObjectPatternValidator();
		idObjectPatternValidator.setValidation(getValidationMap());
		return  idObjectPatternValidator;
	}*/

	@Bean
	public PacketValidatorImpl packetValidatorImpl() {
		return new PacketValidatorImpl();
	}

	@PostConstruct
	public void validateReferenceValidatorImpl() throws ClassNotFoundException {
		if (org.apache.commons.lang3.StringUtils.isNotBlank(env.getProperty(PACKET_VALIDATOR_PROVIDER))) {
			logger.debug("validating referenceValidatorImpl Class is present or not", env.getProperty(PACKET_VALIDATOR_PROVIDER),
					"loading reference validator", "");
			Class.forName(env.getProperty(PACKET_VALIDATOR_PROVIDER));
		}
		logger.debug("referenceValidatorImpl: referenceValidator Class is not provided", env.getProperty(PACKET_VALIDATOR_PROVIDER),
				"failed to load reference validator", "");
	}

	@Bean
	@Lazy
	public PacketValidator referenceValidatorImpl()
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		if (StringUtils.isNotBlank(env.getProperty(PACKET_VALIDATOR_PROVIDER))) {
			logger.debug("instance of referenceValidator is created", env.getProperty(PACKET_VALIDATOR_PROVIDER),
					"loading reference validator", "");
			return (PacketValidator) Class.forName(env.getProperty(PACKET_VALIDATOR_PROVIDER)).newInstance();
		} else {
			logger.debug("no reference validator is provided", env.getProperty(PACKET_VALIDATOR_PROVIDER),
					"loading reference validator", "");
			return new PacketValidator() {
				@Override
				public boolean validate(String registrationId, String process, PacketValidationDto packetValidationDto) throws ApisResourceAccessException, RegistrationProcessorCheckedException, IOException, JsonProcessingException {
					return true;
				}
			};
		}
	}
	
/*	public Map<String, String> getValidationMap(){
		Map<String, String> map=new HashMap<String, String>();
		return map;
		
	}*/
}
