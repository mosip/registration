package io.mosip.registration.processor.message.sender.validator;

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
import io.mosip.registration.processor.core.exception.MessageSenderRequestValidationException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.message.sender.dto.MessageSenderDTO;
import io.mosip.registration.processor.core.message.sender.dto.MessageSenderRequestDTO;


@Component
public class MessageSenderRequestValidator {
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


	private static final String MESSAGE_SENDER_ID = "mosip.regproc.message.sender.api.id";


	private static final String MESSAGE_SENDER_VERSION = "mosip.regproc.message.sender.api.version";

	/** The Constant DATETIME_TIMEZONE. */
	private static final String DATETIME_TIMEZONE = "mosip.registration.processor.timezone";

	/** The Constant DATETIME_PATTERN. */
	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";

	/** The grace period. */
	@Value("${mosip.registration.processor.grace.period}")
	private int gracePeriod;

	Logger regProcLogger = RegProcessorLogger.getLogger(MessageSenderRequestValidator.class);
	/** The env. */
	@Autowired
	private Environment env;

	private static final String MESSAGE_SENDER = "MessageSender";


	public void validate(MessageSenderDTO messageSenderDTO)
			throws MessageSenderRequestValidationException {
		regProcLogger.debug("MessageSenderRequestValidator  validate entry");

		validateId(messageSenderDTO.getId());
		validateVersion(messageSenderDTO.getVersion());
		validateReqTime(messageSenderDTO.getRequesttime());
		validateRid(messageSenderDTO.getRequest());
		validateRegType(messageSenderDTO.getRequest());

		regProcLogger.debug("MessageSenderRequestValidator  validate exit");

	}

	
	private void validateRegType(MessageSenderRequestDTO request) throws MessageSenderRequestValidationException {
		if (StringUtils.isEmpty(request.getRegType())) {
			throw new MessageSenderRequestValidationException(
					PlatformErrorMessages.RPR_MAS_MISSING_INPUT_PARAMETER.getCode(),
					String.format(PlatformErrorMessages.RPR_MAS_MISSING_INPUT_PARAMETER.getMessage(), REGTYPE));

		}
	}

	private void validateRid(MessageSenderRequestDTO request) throws MessageSenderRequestValidationException {
		if (StringUtils.isEmpty(request.getRid())) {
			throw new MessageSenderRequestValidationException(
					PlatformErrorMessages.RPR_MAS_MISSING_INPUT_PARAMETER.getCode(),
					String.format(PlatformErrorMessages.RPR_MAS_MISSING_INPUT_PARAMETER.getMessage(), RID));

		}
	}

	private void validateVersion(String version)
			throws MessageSenderRequestValidationException {
		if (Objects.isNull(version)) {
			throw new MessageSenderRequestValidationException(
					PlatformErrorMessages.RPR_MAS_MISSING_INPUT_PARAMETER.getCode(),
					String.format(PlatformErrorMessages.RPR_MAS_MISSING_INPUT_PARAMETER.getMessage(), VER));


		} else if (!version.equalsIgnoreCase(env.getProperty(MESSAGE_SENDER_VERSION))) {
			throw new MessageSenderRequestValidationException(
					PlatformErrorMessages.RPR_MAS_INVALID_INPUT_PARAMETER.getCode(),
					String.format(PlatformErrorMessages.RPR_MAS_INVALID_INPUT_PARAMETER.getMessage(), VER));


		}
	}

	
	private void validateId(String id) throws MessageSenderRequestValidationException {
		if (Objects.isNull(id)) {
			throw new MessageSenderRequestValidationException(
					PlatformErrorMessages.RPR_MAS_MISSING_INPUT_PARAMETER.getCode(),
					String.format(PlatformErrorMessages.RPR_MAS_MISSING_INPUT_PARAMETER.getMessage(), ID_FIELD));

		} else if (!id.equalsIgnoreCase(env.getProperty(MESSAGE_SENDER_ID))) {
			throw new MessageSenderRequestValidationException(
					PlatformErrorMessages.RPR_MAS_INVALID_INPUT_PARAMETER.getCode(),
					String.format(PlatformErrorMessages.RPR_MAS_INVALID_INPUT_PARAMETER.getMessage(), ID_FIELD));

		}
	}

	
	private void validateReqTime(String requesttime)
			throws MessageSenderRequestValidationException {

		if (Objects.isNull(requesttime)) {
			throw new MessageSenderRequestValidationException(
					PlatformErrorMessages.RPR_MAS_MISSING_INPUT_PARAMETER.getCode(),
					String.format(PlatformErrorMessages.RPR_MAS_MISSING_INPUT_PARAMETER.getMessage(), TIMESTAMP));

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
						regProcLogger.error(MESSAGE_SENDER, "MessageSenderRequestValidator", "validateReqTime",
								"\n" + PlatformErrorMessages.RPR_MAS_INVALID_INPUT_PARAMETER.getMessage());

						throw new MessageSenderRequestValidationException(
								PlatformErrorMessages.RPR_MAS_INVALID_INPUT_PARAMETER.getCode(), String.format(
										PlatformErrorMessages.RPR_MAS_INVALID_INPUT_PARAMETER.getMessage(), TIMESTAMP));
					}

				}
			} catch (IllegalArgumentException e) {
				regProcLogger.error(MESSAGE_SENDER, "MessageSenderRequestValidator", "validateReqTime",
						"\n" + ExceptionUtils.getStackTrace(e));
				throw new MessageSenderRequestValidationException(
						PlatformErrorMessages.RPR_MAS_INVALID_INPUT_PARAMETER.getCode(),
						String.format(PlatformErrorMessages.RPR_MAS_INVALID_INPUT_PARAMETER.getMessage(), TIMESTAMP));
			}
		}
	}

}
