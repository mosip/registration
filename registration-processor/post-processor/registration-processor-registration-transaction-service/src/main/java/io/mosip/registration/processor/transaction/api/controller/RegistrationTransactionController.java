package io.mosip.registration.processor.transaction.api.controller;

import java.util.List;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.token.validation.exception.AccessDeniedException;
import io.mosip.registration.processor.core.token.validation.exception.InvalidTokenException;
import io.mosip.registration.processor.core.util.DigitalSignatureUtility;
import io.mosip.registration.processor.status.dto.RegistrationTransactionDto;
import io.mosip.registration.processor.status.dto.TransactionDto;
import io.mosip.registration.processor.status.exception.RegTransactionAppException;
import io.mosip.registration.processor.status.exception.TransactionTableNotAccessibleException;
import io.mosip.registration.processor.status.exception.TransactionsUnavailableException;
import io.mosip.registration.processor.status.service.TransactionService;
import io.mosip.registration.processor.status.sync.response.dto.RegTransactionResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * RegistrationTransactionController class to retreive transaction details
 * @author Jyoti Prakash Nayak
 *
 */
@RefreshScope
@RestController
@Tag(name = "Registration Status", description = "Registration Transaction Controller")
public class RegistrationTransactionController {
	
	@Autowired
	TransactionService<TransactionDto> transactionService;
	
	@Autowired
	private Environment env;

	@Value("${registration.processor.signature.isEnabled}")
	private Boolean isEnabled;
	
	@Autowired
	private DigitalSignatureUtility digitalSignatureUtility;
	
	private static final String INVALIDTOKENMESSAGE = "Authorization Token Not Available In The Header";
	private static final String REG_TRANSACTION_SERVICE_ID = "mosip.registration.processor.registration.transaction.id";
	private static final String REG_TRANSACTION_APPLICATION_VERSION = "mosip.registration.processor.transaction.version";
	private static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";
	private static final String RESPONSE_SIGNATURE = "Response-Signature";
	
	/**
	 * get transaction details for the given registration id
	 * 
	 * @param rid registration id
	 * @param request servlet request
	 * @return list of RegTransactionResponseDTOs 
	 * @throws Exception
	 */
	@PreAuthorize("hasAnyRole(@authorizedTransactionRoles.getGetsearchrid())")
	//@PreAuthorize("hasAnyRole('REGISTRATION_PROCESSOR','REGISTRATION_ADMIN')")
	@GetMapping(path = "/search/{rid}")
	@Operation(summary = "Get the transaction entity/entities", description = "Get the transaction entity/entities", tags = { "Registration Status" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Transaction Entity/Entities successfully fetched"),
			@ApiResponse(responseCode = "400", description = "Unable to fetch Transaction Entity/Entities" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true)))})
	public ResponseEntity<RegTransactionResponseDTO> getTransactionsbyRid(@PathVariable("rid") String rid,
			HttpServletRequest request) throws Exception {
		List<RegistrationTransactionDto> dtoList;
		HttpHeaders headers = new HttpHeaders();
		try {
			dtoList = transactionService.getTransactionByRegId(rid);
			RegTransactionResponseDTO responseDTO=buildRegistrationTransactionResponse(dtoList);
			if (isEnabled) {		 
				headers.add(RESPONSE_SIGNATURE,
						digitalSignatureUtility.getDigitalSignature(buildSignatureRegistrationTransactionResponse(responseDTO)));	
				return ResponseEntity.status(HttpStatus.OK).headers(headers).body(responseDTO);
			}
				return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
		}catch (Exception e) {
			if( e instanceof InvalidTokenException |e instanceof AccessDeniedException | e instanceof RegTransactionAppException
				| e instanceof TransactionsUnavailableException | e instanceof TransactionTableNotAccessibleException ) {
				throw e;
			}
			else {
				throw new RegTransactionAppException(PlatformErrorMessages.RPR_RTS_UNKNOWN_EXCEPTION.getCode(), 
						PlatformErrorMessages.RPR_RTS_UNKNOWN_EXCEPTION.getMessage()+" -->"+e.getMessage());
			}
		}
	}

	/**
	 * build the registration transaction response
	 * @param dtoList registration transaction dtos
	 * @return registration transaction response
	 */
	private RegTransactionResponseDTO buildRegistrationTransactionResponse(List<RegistrationTransactionDto> dtoList) {
		RegTransactionResponseDTO regTransactionResponseDTO= new RegTransactionResponseDTO();
		if (Objects.isNull(regTransactionResponseDTO.getId())) {
			regTransactionResponseDTO.setId(env.getProperty(REG_TRANSACTION_SERVICE_ID));
		}
		regTransactionResponseDTO.setResponsetime(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
		regTransactionResponseDTO.setVersion(env.getProperty(REG_TRANSACTION_APPLICATION_VERSION));
		regTransactionResponseDTO.setErrors(null);
		regTransactionResponseDTO.setResponse(dtoList);
		return regTransactionResponseDTO;
	}

	/**
	 * convert registration transaction response dto to json string
	 * @param dto registration transaction response dto
	 * @return
	 */
	private String buildSignatureRegistrationTransactionResponse(RegTransactionResponseDTO dto) {
		Gson gson = new GsonBuilder().serializeNulls().create();
		return gson.toJson(dto);
	}
}
