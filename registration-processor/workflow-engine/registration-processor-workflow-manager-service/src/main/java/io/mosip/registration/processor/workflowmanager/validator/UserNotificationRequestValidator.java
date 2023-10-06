package io.mosip.registration.processor.workflowmanager.validator;

import java.util.Objects;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.format.datetime.joda.DateTimeFormatterFactory;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.exception.UserNotificationRequestValidationException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.user.notification.dto.UserNotificationDTO;
import io.mosip.registration.processor.core.user.notification.dto.UserNotificationRequestDTO;


@Component
public class UserNotificationRequestValidator {
	/** The Constant VER. */
	private static final String VER = "version";

	/** The Constant TIMESTAMP. */
	private static final String TIMESTAMP = "requesttime";

	/** The Constant ID_FIELD. */
	private static final String ID_FIELD = "id";

	/** The Constant ID_FIELD. */
	private static final String RID = "rid";

	/** The Constant ID_FIELD. */
	private static final String REGTYPE = "regType";


	private static final String USER_NOTIFICATION_ID = "mosip.regproc.user.notification.api.id";


	private static final String MESSAGE_SENDER_VERSION = "mosip.regproc.user.notification.api.version";

	/** The Constant DATETIME_TIMEZONE. */
	private static final String DATETIME_TIMEZONE = "mosip.registration.processor.timezone";

	/** The Constant DATETIME_PATTERN. */
	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";

	/** The grace period. */
	@Value("${mosip.registration.processor.grace.period}")
	private int gracePeriod;

	Logger regProcLogger = RegProcessorLogger.getLogger(UserNotificationRequestValidator.class);
	/** The env. */
	@Autowired
	private Environment env;

	private static final String USER_NOTIFICATION = "userNotification";


	public void validate(UserNotificationDTO userNotificationDTO)
			throws UserNotificationRequestValidationException {
		regProcLogger.debug("userNotificationRequestValidator  validate entry");

		validateId(userNotificationDTO.getId());
		validateVersion(userNotificationDTO.getVersion());
		validateReqTime(userNotificationDTO.getRequesttime());
		validateRid(userNotificationDTO.getRequest());
		validateRegType(userNotificationDTO.getRequest());
		
		regProcLogger.debug("userNotificationRequestValidator  validate exit");

	}


	private void validateRegType(UserNotificationRequestDTO request) throws UserNotificationRequestValidationException {
		if (StringUtils.isEmpty(request.getRegType())) {
			throw new UserNotificationRequestValidationException(
					PlatformErrorMessages.RPR_UNA_MISSING_INPUT_PARAMETER.getCode(),
					String.format(PlatformErrorMessages.RPR_UNA_MISSING_INPUT_PARAMETER.getMessage(), REGTYPE));

		}
	}

	private void validateRid(UserNotificationRequestDTO request) throws UserNotificationRequestValidationException {
		if (StringUtils.isEmpty(request.getRid())) {
			throw new UserNotificationRequestValidationException(
					PlatformErrorMessages.RPR_UNA_MISSING_INPUT_PARAMETER.getCode(),
					String.format(PlatformErrorMessages.RPR_UNA_MISSING_INPUT_PARAMETER.getMessage(), RID));

		}
	}

	private void validateVersion(String version)
			throws UserNotificationRequestValidationException {
		if (Objects.isNull(version)) {
			throw new UserNotificationRequestValidationException(
					PlatformErrorMessages.RPR_UNA_MISSING_INPUT_PARAMETER.getCode(),
					String.format(PlatformErrorMessages.RPR_UNA_MISSING_INPUT_PARAMETER.getMessage(), VER));


		} else if (!version.equalsIgnoreCase(env.getProperty(MESSAGE_SENDER_VERSION))) {
			throw new UserNotificationRequestValidationException(
					PlatformErrorMessages.RPR_UNA_INVALID_INPUT_PARAMETER.getCode(),
					String.format(PlatformErrorMessages.RPR_UNA_INVALID_INPUT_PARAMETER.getMessage(), VER));


		}
	}


	private void validateId(String id) throws UserNotificationRequestValidationException {
		if (Objects.isNull(id)) {
			throw new UserNotificationRequestValidationException(
					PlatformErrorMessages.RPR_UNA_MISSING_INPUT_PARAMETER.getCode(),
					String.format(PlatformErrorMessages.RPR_UNA_MISSING_INPUT_PARAMETER.getMessage(), ID_FIELD));

		} else if (!id.equalsIgnoreCase(env.getProperty(USER_NOTIFICATION_ID))) {
			throw new UserNotificationRequestValidationException(
					PlatformErrorMessages.RPR_UNA_INVALID_INPUT_PARAMETER.getCode(),
					String.format(PlatformErrorMessages.RPR_UNA_INVALID_INPUT_PARAMETER.getMessage(), ID_FIELD));

		}
	}


	private void validateReqTime(String requesttime)
			throws UserNotificationRequestValidationException {

		if (Objects.isNull(requesttime)) {
			throw new UserNotificationRequestValidationException(
					PlatformErrorMessages.RPR_UNA_MISSING_INPUT_PARAMETER.getCode(),
					String.format(PlatformErrorMessages.RPR_UNA_MISSING_INPUT_PARAMETER.getMessage(), TIMESTAMP));

		} else {
			try {
				if (Objects.nonNull(env.getProperty(DATETIME_PATTERN))) {
					DateTimeFormatterFactory timestampFormat = new DateTimeFormatterFactory(
							env.getProperty(DATETIME_PATTERN));
					timestampFormat.setTimeZone(TimeZone.getTimeZone(env.getProperty(DATETIME_TIMEZONE)));
					if (!(DateTime.parse(requesttime, timestampFormat.createDateTimeFormatter())
							.isAfter(new DateTime().minusSeconds(gracePeriod))
							&& DateTime.parse(requesttime, timestampFormat.createDateTimeFormatter())
									.isBefore(new DateTime().plusSeconds(gracePeriod)))) {
						regProcLogger.error(USER_NOTIFICATION, "userNotificationRequestValidator", "validateReqTime",
								"\n" + PlatformErrorMessages.RPR_UNA_INVALID_INPUT_PARAMETER.getMessage());

						throw new UserNotificationRequestValidationException(
								PlatformErrorMessages.RPR_UNA_INVALID_INPUT_PARAMETER.getCode(), String.format(
										PlatformErrorMessages.RPR_UNA_INVALID_INPUT_PARAMETER.getMessage(), TIMESTAMP));
					}

				}
			} catch (IllegalArgumentException e) {
				regProcLogger.error(USER_NOTIFICATION, "userNotificationRequestValidator", "validateReqTime",
						"\n" + ExceptionUtils.getStackTrace(e));
				throw new UserNotificationRequestValidationException(
						PlatformErrorMessages.RPR_UNA_INVALID_INPUT_PARAMETER.getCode(),
						String.format(PlatformErrorMessages.RPR_UNA_INVALID_INPUT_PARAMETER.getMessage(), TIMESTAMP));
			}
		}
	}

}
