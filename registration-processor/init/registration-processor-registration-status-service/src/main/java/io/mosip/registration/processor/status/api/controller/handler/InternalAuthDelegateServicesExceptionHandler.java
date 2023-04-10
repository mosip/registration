package io.mosip.registration.processor.status.api.controller.handler;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.auth.dto.AuthResponseDTO;
import io.mosip.registration.processor.core.auth.dto.ErrorDTO;
import io.mosip.registration.processor.core.auth.dto.ResponseDTO;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.token.validation.exception.InvalidTokenException;
import io.mosip.registration.processor.status.api.controller.InternalAuthDelegateServicesController;
import io.mosip.registration.processor.status.exception.InternalAuthDeligateAppException;
import io.mosip.registration.processor.status.sync.response.dto.RegTransactionResponseDTO;

@RestControllerAdvice(assignableTypes = InternalAuthDelegateServicesController.class)
public class InternalAuthDelegateServicesExceptionHandler {

	private static Logger regProcLogger = RegProcessorLogger
			.getLogger(InternalAuthDelegateServicesExceptionHandler.class);

	private static final String INTERNAL_AUTH_APPLICATION_REQUEST_ID = "mosip.identity.auth.internal.requestid";
	private static final String INTERNAL_AUTH_APPLICATION_VERSION = "mosip.registration.processor.application.version";
	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";
	
	@Autowired
	private Environment env;

	@ExceptionHandler(ApisResourceAccessException.class)
	public ResponseEntity<Object> ApisResourceAccessException(ApisResourceAccessException e) {
		regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
				e.getErrorCode(), e.getMessage());
		return buildInternalAuthDelegateServiceExceptionResponse((Exception) e);
	}
	
	@ExceptionHandler(InvalidTokenException.class)
	protected ResponseEntity<Object> handleInvalidTokenException(InvalidTokenException e, WebRequest request) {
		return buildInternalAuthDelegateServiceExceptionResponse((Exception)e);

	}
	
	@ExceptionHandler(InternalAuthDeligateAppException.class)
	protected ResponseEntity<Object> handleInternalAuthDeligateAppException(InternalAuthDeligateAppException e) {
		regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
				e.getErrorCode(), e.getMessage());
		return buildInternalAuthDelegateServiceExceptionResponse((Exception) e);
	}

	private ResponseEntity<Object> buildInternalAuthDelegateServiceExceptionResponse(Exception ex) {

		AuthResponseDTO response = new AuthResponseDTO();
		Throwable e = ex;

		response.setId(env.getProperty(INTERNAL_AUTH_APPLICATION_REQUEST_ID));
		if (e instanceof BaseCheckedException)

		{
			List<String> errorCodes = ((BaseCheckedException) e).getCodes();
			List<String> errorTexts = ((BaseCheckedException) e).getErrorTexts();

			List<ErrorDTO> errors = errorTexts.parallelStream()
					.map(errMsg -> new ErrorDTO(errorCodes.get(errorTexts.indexOf(errMsg)), errMsg)).distinct()
					.collect(Collectors.toList());

			response.setErrors(errors);
		}
		if (e instanceof BaseUncheckedException) {
			List<String> errorCodes = ((BaseUncheckedException) e).getCodes();
			List<String> errorTexts = ((BaseUncheckedException) e).getErrorTexts();

			List<ErrorDTO> errors = errorTexts.parallelStream()
					.map(errMsg -> new ErrorDTO(errorCodes.get(errorTexts.indexOf(errMsg)), errMsg)).distinct()
					.collect(Collectors.toList());

			response.setErrors(errors);
		}

		response.setResponseTime(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
		response.setVersion(env.getProperty(INTERNAL_AUTH_APPLICATION_VERSION));
		response.setResponse(null);

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

}
