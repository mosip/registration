package io.mosip.registration.processor.status.validator;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.format.datetime.joda.DateTimeFormatterFactory;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.status.dto.RegistrationExternalStatusRequestDTO;
import io.mosip.registration.processor.status.exception.RegStatusAppException;
import io.mosip.registration.processor.status.exception.RegStatusValidationException;

/**
 * The Class RegistrationExternalStatusRequestValidator.
 * 
 * @author Satish Gohil
 */
@Component
public class RegistrationExternalStatusRequestValidator {

	/** The Constant DATETIME_TIMEZONE. */
	private static final String DATETIME_TIMEZONE = "mosip.registration.processor.timezone";

	/** The Constant DATETIME_PATTERN. */
	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";

	/** The mosip logger. */
	Logger regProcLogger = RegProcessorLogger.getLogger(RegistrationExternalStatusRequestValidator.class);

	/** The Constant REG_EXTERNAL_STATUS_SERVICE. */
	private static final String REG_EXTERNAL_STATUS_SERVICE = "RegExternalStatusService";

	/** The Constant REG_EXTERNAL_STATUS_APPLICATION_VERSION. */
	private static final String REG_EXTERNAL_STATUS_APPLICATION_VERSION = "mosip.registration.processor.registration.external.status.version";

	/** The env. */
	@Autowired
	private Environment env;


	/** The grace period. */
	@Value("${mosip.registration.processor.grace.period}")
	private int gracePeriod;

	/** The id. */
	private Map<String, String> id = new HashMap<>();

	/**
	 * Validate.
	 *
	 * @param registrationExternalStatusRequestDTO
	 *            the registration external status request DTO
	 * @param serviceId
	 *            the service id
	 * @throws RegStatusAppException
	 *             the reg status app exception
	 */
	public void validate(RegistrationExternalStatusRequestDTO registrationExternalStatusRequestDTO, String serviceId)
			throws RegStatusAppException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"RegistrationExternalStatusRequestValidator::validate()::entry");

		id.put("status", serviceId);
		validateId(registrationExternalStatusRequestDTO.getId());
		validateVersion(registrationExternalStatusRequestDTO.getVersion());
		validateReqTime(registrationExternalStatusRequestDTO.getRequesttime());

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"RegistrationExternalStatusRequestValidator::validate()::exit");

	}

	/**
	 * Validate id.
	 *
	 * @param id
	 *            the id
	 * @throws RegStatusAppException
	 *             the reg status app exception
	 */
	private void validateId(String id) throws RegStatusAppException {
		RegStatusValidationException exception = new RegStatusValidationException();

		if (Objects.isNull(id)) {

			throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_MISSING_INPUT_PARAMETER_ID, exception);
		} else if (!this.id.containsValue(id)) {

			throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_INVALID_INPUT_PARAMETER_ID, exception);

		}
	}

	/**
	 * Validate ver.
	 *
	 * @param ver
	 *            the ver
	 * @throws RegStatusAppException
	 *             the reg status app exception
	 */
	private void validateVersion(String ver) throws RegStatusAppException {
		String version = env.getProperty(REG_EXTERNAL_STATUS_APPLICATION_VERSION);
		RegStatusValidationException exception = new RegStatusValidationException();
		if (Objects.isNull(ver)) {
			throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_MISSING_INPUT_PARAMETER_VERSION, exception);

		} else if (version!=null && !version.equals(ver)) {

			throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_INVALID_INPUT_PARAMETER_VERSION, exception);
		}
	}

	/**
	 * Validate req time.
	 *
	 * @param timestamp
	 *            the timestamp
	 * @throws RegStatusAppException
	 *             the reg status app exception
	 */
	private void validateReqTime(String timestamp) throws RegStatusAppException {
		RegStatusValidationException exception = new RegStatusValidationException();

		if (Objects.isNull(timestamp)) {
			throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_MISSING_INPUT_PARAMETER_TIMESTAMP, exception);

		} else {
			try {
				if (Objects.nonNull(env.getProperty(DATETIME_PATTERN))) {
					DateTimeFormatterFactory timestampFormat = new DateTimeFormatterFactory(
							env.getProperty(DATETIME_PATTERN));
					timestampFormat.setTimeZone(TimeZone.getTimeZone(env.getProperty(DATETIME_TIMEZONE)));
					if (!(DateTime.parse(timestamp, timestampFormat.createDateTimeFormatter())
							.isAfter(new DateTime().minusSeconds(gracePeriod))
							&& DateTime.parse(timestamp, timestampFormat.createDateTimeFormatter())
									.isBefore(new DateTime().plusSeconds(gracePeriod)))) {
						regProcLogger.error(REG_EXTERNAL_STATUS_SERVICE, "RegistrationExternalStatusRequestValidator", "validateReqTime",
								"\n" + PlatformErrorMessages.RPR_RGS_INVALID_INPUT_PARAMETER_TIMESTAMP.getMessage());

						throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_INVALID_INPUT_PARAMETER_TIMESTAMP,
								exception);
					}

				}
			} catch (IllegalArgumentException e) {
				regProcLogger.error(REG_EXTERNAL_STATUS_SERVICE, "IdRequestValidator", "validateReqTime",
						"\n" + ExceptionUtils.getStackTrace(e));
				throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_INVALID_INPUT_PARAMETER_TIMESTAMP,
						exception);
			}
		}
	}

}
