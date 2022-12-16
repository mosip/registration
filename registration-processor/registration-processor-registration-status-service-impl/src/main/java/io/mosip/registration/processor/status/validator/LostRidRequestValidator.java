package io.mosip.registration.processor.status.validator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.format.datetime.joda.DateTimeFormatterFactory;
import org.springframework.stereotype.Component;

import com.hazelcast.util.StringUtil;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.WorkFlowSearchException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.workflow.dto.SortInfo;
import io.mosip.registration.processor.status.dto.FilterInfo;
import io.mosip.registration.processor.status.dto.LostRidRequestDto;
import io.mosip.registration.processor.status.exception.LostRidValidationException;
import io.mosip.registration.processor.status.exception.RegStatusAppException;
import io.mosip.registration.processor.status.exception.RegStatusValidationException;

@Component
public class LostRidRequestValidator {

	/** The Constant DATETIME_TIMEZONE. */
	private static final String DATETIME_TIMEZONE = "mosip.registration.processor.timezone";

	/** The Constant DATETIME_PATTERN. */
	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";

	/** The mosip logger. */
	Logger regProcLogger = RegProcessorLogger.getLogger(RegistrationStatusRequestValidator.class);

	/** The Constant REG_STATUS_SERVICE. */
	private static final String REG_STATUS_SERVICE = "RegStatusService";

	@Value("${mosip.registration.processor.lostrid.max-registration-date-filter-interval:30}")
	private int maxRegistrationDateFilterInterval;

	/** The Constant REG_STATUS_APPLICATION_VERSION. */
	private static final String REG_LOSTRID_APPLICATION_VERSION = "mosip.registration.processor.lostrid.version";

	private static final String REG_LOSTRID_SERVICE_ID = "mosip.registration.processor.lostrid.id";

	/** The env. */
	@Autowired
	private Environment env;

	/** The grace period. */
	@Value("${mosip.registration.processor.grace.period}")
	private int gracePeriod;

	/** The registrationDate pattern. */
	@Value("${mosip.registration.processor.lostrid.registrationdate.pattern}")
	private String regDatePattern;


	/**
	 * Validate.
	 *
	 * @param registrationStatusRequestDTO the registration status request DTO
	 * @param serviceId                    the service id
	 * @throws RegStatusAppException the reg status app exception
	 */
	public void validate(LostRidRequestDto lostRidRequestDto)
			throws RegStatusAppException, WorkFlowSearchException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"LostRidRequestValidator::validate()::entry");

		validateId(lostRidRequestDto.getId());
		validateVersion(lostRidRequestDto.getVersion());
		validateReqTime(lostRidRequestDto.getRequesttime());
		validateFilter(lostRidRequestDto.getRequest().getFilters());
		validateSortField(lostRidRequestDto.getRequest().getSort());

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"LostRidRequestValidator::validate()::exit");

	}

	/**
	 * Validate id.
	 *
	 * @param id the id
	 * @throws RegStatusAppException the reg status app exception
	 */
	private void validateId(String id) throws RegStatusAppException {
		LostRidValidationException exception = new LostRidValidationException();
		String lostRidService=env.getProperty(REG_LOSTRID_SERVICE_ID);
		if (Objects.isNull(id)) {

			throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_MISSING_INPUT_PARAMETER_ID, exception);
			
		} else if (lostRidService!=null && !lostRidService.equals(id)) {

			throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_INVALID_INPUT_PARAMETER_ID, exception);

		}
	}


	/**
	 * Validate filter.
	 *
	 * @param version the version
	 * @param errors  the errors
	 * @return true, if successful
	 * @throws RegStatusAppException
	 * 
	 */
	private void validateFilter(List<FilterInfo> filterInfos) throws WorkFlowSearchException, RegStatusAppException {
		LostRidValidationException exception = new LostRidValidationException();
		if (Objects.isNull(filterInfos)) {
			throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_MISSING_INPUT_PARAMETER, exception);
		}
		for (FilterInfo filter : filterInfos) {
			if (filter.getColumnName().equals("name") || filter.getColumnName().equals("email")
					|| filter.getColumnName().equals("phone") || filter.getColumnName().equals("centerId")
					|| filter.getColumnName().equals("locationCode")||filter.getColumnName().equals("registrationDate")) {
				validateFilterType(filter);
			} else {
				throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_MISSING_INPUT_PARAMETER, exception);
			}
		}

	}

	/**
	 * Validate filter type.
	 *
	 * @param version the version
	 * @param errors  the errors
	 * @return true, if successful
	 * @throws RegStatusAppException
	 * 
	 */
	private void validateFilterType(FilterInfo filter)
			throws WorkFlowSearchException, RegStatusAppException {
		LostRidValidationException exception = new LostRidValidationException();
		if (filter.getColumnName().equals("registrationDate") && filter.getType().equalsIgnoreCase("between")) {
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern(regDatePattern);
			LocalDate dateForm = LocalDate.parse(filter.getFromValue(), dtf);
			LocalDate dateTo = LocalDate.parse(filter.getToValue(), dtf);
			long noOfDaysBetween = ChronoUnit.DAYS.between(dateForm, dateTo);
			if (noOfDaysBetween > maxRegistrationDateFilterInterval) {
				throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_DATE_VALIDATION_FAILED, exception);
			}
		} else if (!filter.getType().equalsIgnoreCase("equals")) {
			throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_MISSING_INPUT_PARAMETER, exception);
		}

	}

	/**
	 * Validate ver.
	 *
	 * @param ver the ver
	 * @throws RegStatusAppException the reg status app exception
	 */
	private void validateVersion(String ver) throws RegStatusAppException {
		String version = env.getProperty(REG_LOSTRID_APPLICATION_VERSION);
		LostRidValidationException exception = new LostRidValidationException();
		if (Objects.isNull(ver)) {
			throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_MISSING_INPUT_PARAMETER_VERSION, exception);

		} else if (version!=null && !version.equals(ver)) {
			throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_INVALID_INPUT_PARAMETER_VERSION, exception);
		}
	}

	/**
	 * Validate sortInfo.
	 *
	 * @param version the version
	 * @param errors  the errors
	 * @return true, if successful
	 * @throws RegStatusAppException
	 */
	private void validateSortField(List<SortInfo> sortInfos) throws WorkFlowSearchException, RegStatusAppException {
		LostRidValidationException exception = new LostRidValidationException();
		if (sortInfos.size() > 1) {
			throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_SORTING_VALIDATION_FAILED, exception);
		} else if (sortInfos.get(0).getSortField().equals("name") || sortInfos.get(0).getSortField().equals("email")
				|| sortInfos.get(0).getSortField().equals("phone") || sortInfos.get(0).getSortField().equals("centerId")
				|| sortInfos.get(0).getSortField().equals("registrationDate")
				|| sortInfos.get(0).getSortField().equals("locationCode")) {

		} else {
			throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_MISSING_INPUT_PARAMETER, exception);

		}

	}

	
	/**
	 * Validate req time.
	 *
	 * @param timestamp the timestamp
	 * @throws RegStatusAppException the reg status app exception
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
						regProcLogger.error(REG_STATUS_SERVICE, "LostRidRequestValidator", "validateReqTime",
								"\n" + PlatformErrorMessages.RPR_RGS_INVALID_INPUT_PARAMETER_TIMESTAMP.getMessage());

						throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_INVALID_INPUT_PARAMETER_TIMESTAMP,
								exception);
					}

				}
			} catch (IllegalArgumentException e) {
				regProcLogger.error(REG_STATUS_SERVICE, "IdRequestValidator", "validateReqTime",
						"\n" + ExceptionUtils.getStackTrace(e));
				throw new RegStatusAppException(PlatformErrorMessages.RPR_RGS_INVALID_INPUT_PARAMETER_TIMESTAMP,
						exception);
			}
		}
	}
}