package io.mosip.registration.processor.request.handler.service.exception.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.common.rest.dto.ErrorDTO;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.token.validation.exception.AccessDeniedException;
import io.mosip.registration.processor.core.token.validation.exception.InvalidTokenException;
import io.mosip.registration.processor.request.handler.service.controller.UinCardRePrintController;
import io.mosip.registration.processor.request.handler.service.dto.PacketGeneratorResponseDto;
import io.mosip.registration.processor.request.handler.service.exception.RegBaseCheckedException;
import io.mosip.registration.processor.request.handler.service.exception.RegBaseUnCheckedException;

/**
 * The Class PacketGeneratorExceptionHandler.
 */
@RestControllerAdvice(assignableTypes=UinCardRePrintController.class)
public class UinCardRePrintExceptionHandler extends ResponseEntityExceptionHandler {

	/** The env. */
	@Autowired
	private Environment env;

	/** The Constant REG_UINCARD_REPRINT_SERVICE_ID. */
	private static final String REG_UINCARD_REPRINT_SERVICE_ID = "mosip.registration.processor.uincard.reprint.id";

	/** The Constant REG_PACKET_GENERATOR_APPLICATION_VERSION. */
	private static final String REG_PACKET_GENERATOR_APPLICATION_VERSION = "mosip.registration.processor.application.version";

	/** The Constant DATETIME_PATTERN. */
	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(UinCardRePrintExceptionHandler.class);

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<Object> accessDenied(AccessDeniedException e) {
		regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
				e.getErrorCode(), e.getMessage());
		return packetGenExceptionResponse((Exception) e);
	}

	/**
	 * Badrequest.
	 *
	 * @param e
	 *            the e
	 * @return the string
	 */
	@ExceptionHandler(RegBaseCheckedException.class)
	public ResponseEntity<Object> badrequest(RegBaseCheckedException e) {
		regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
				e.getErrorCode(), String.valueOf(e.getCause()));

		return packetGenExceptionResponse(e);
	}

	/**
	 * Badrequest.
	 *
	 * @param e
	 *            the e
	 * @return the string
	 */
	@ExceptionHandler(RegBaseUnCheckedException.class)
	public ResponseEntity<Object> badrequest(RegBaseUnCheckedException e) {
		regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
				e.getErrorCode(), e.getCause().toString());

		return packetGenExceptionResponse(e);
	}

	@ExceptionHandler(InvalidTokenException.class)
	protected ResponseEntity<Object> handleInvalidTokenException(InvalidTokenException e, WebRequest request) {
		return packetGenExceptionResponse((Exception) e);

	}

	/**
	 * Packet gen exception response.
	 *
	 * @param ex
	 *            the ex
	 * @return the string
	 */
	public ResponseEntity<Object> packetGenExceptionResponse(Exception ex) {
		PacketGeneratorResponseDto response = new PacketGeneratorResponseDto();

		if (Objects.isNull(response.getId())) {
			response.setId(env.getProperty(REG_UINCARD_REPRINT_SERVICE_ID));
		}
		if (ex instanceof BaseCheckedException)

		{
			List<String> errorCodes = ((BaseCheckedException) ex).getCodes();
			List<String> errorTexts = ((BaseCheckedException) ex).getErrorTexts();

			List<ErrorDTO> errors = errorTexts.parallelStream()
					.map(errMsg -> new ErrorDTO(errorCodes.get(errorTexts.indexOf(errMsg)), errMsg)).distinct()
					.collect(Collectors.toList());

			response.setErrors(errors);
		}
		if (ex instanceof BaseUncheckedException) {
			List<String> errorCodes = ((BaseUncheckedException) ex).getCodes();
			List<String> errorTexts = ((BaseUncheckedException) ex).getErrorTexts();

			List<ErrorDTO> errors = errorTexts.parallelStream()
					.map(errMsg -> new ErrorDTO(errorCodes.get(errorTexts.indexOf(errMsg)), errMsg)).distinct()
					.collect(Collectors.toList());

			response.setErrors(errors);
		}
		response.setResponsetime(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
		response.setVersion(env.getProperty(REG_PACKET_GENERATOR_APPLICATION_VERSION));
		response.setResponse(null);
		return ResponseEntity.status(HttpStatus.OK).body(response);

	}
	
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {
		List<ErrorDTO> details = new ArrayList<>();
		PacketGeneratorResponseDto response = new PacketGeneratorResponseDto();
		ErrorDTO errorDto;
		response.setId(env.getProperty(REG_PACKET_GENERATOR_APPLICATION_VERSION));
		for (ObjectError error : ex.getBindingResult().getAllErrors()) {
			errorDto = new ErrorDTO();
			errorDto.setErrorCode(StatusUtil.INVALID_REQUEST.getCode());
			errorDto.setMessage(StatusUtil.INVALID_REQUEST.getMessage() + error.getDefaultMessage());

			details.add(errorDto);
		}
		response.setErrors(details);
		response.setResponsetime(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
		response.setVersion(env.getProperty(REG_PACKET_GENERATOR_APPLICATION_VERSION));
		response.setResponse(null);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

}
