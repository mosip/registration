package io.mosip.registration.processor.status.api.controller;

import io.mosip.kernel.core.http.ResponseFilter;
import io.mosip.registration.processor.core.auth.dto.AuthRequestDTO;
import io.mosip.registration.processor.core.auth.dto.AuthResponseDTO;
import io.mosip.registration.processor.status.service.InternalAuthDelegateService;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * The Class InternalAuthDelegateServicesController - The controller exposing
 * the delegate services for internal authentication API calls
 * @author Loganathan Sekar
 */
@RefreshScope
@RestController
@Tag(name = "Internal Auth Delegate Services", description = "Internal Auth Delegate Services")
public class InternalAuthDelegateServicesController {

	/** The internal auth delegate service. */
	@Autowired
	private InternalAuthDelegateService internalAuthDelegateService;

	/**
	 * Authenticate.
	 *
	 * @param authRequestDTO the auth request DTO
	 * @return the AuthResponseDTO
	 * @throws Exception 
	 */
	@PreAuthorize("hasAnyRole('REGISTRATION_ADMIN','REGISTRATION_OFFICER','REGISTRATION_SUPERVISOR')")
	@PostMapping(path = "/auth", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Authenticate Internal Request", description = "Authenticate Internal Request", tags = { "Internal Auth Delegate Services" })
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Request authenticated successfully") })
	public ResponseEntity<AuthResponseDTO> authenticate(@Validated @RequestBody AuthRequestDTO authRequestDTO,
			@RequestHeader HttpHeaders headers) throws Exception {
		return ResponseEntity.status(HttpStatus.OK)
				.body(internalAuthDelegateService.authenticate(authRequestDTO, headers));
	}

	/**
	 * Gets the certificate.
	 *
	 * @param applicationId the application id
	 * @param referenceId   the reference id
	 * @return the certificate
	 * @throws Exception 
	 */
	@PreAuthorize("hasAnyRole('INDIVIDUAL','REGISTRATION_PROCESSOR','REGISTRATION_ADMIN','REGISTRATION_SUPERVISOR','REGISTRATION_OFFICER','PRE_REGISTRATION_ADMIN')")
	@ResponseFilter
	@GetMapping(value = "/getCertificate")
	@Operation(summary = "getCertificate", description = "getCertificate", tags = { "Internal Auth Delegate Services" })
	public Object getCertificate(@Parameter(description = "Id of application") @RequestParam("applicationId") String applicationId,
								 @Parameter(description="Reference Id as metadata") @RequestParam("referenceId") Optional<String> referenceId, @RequestHeader HttpHeaders headers) throws Exception {
		return internalAuthDelegateService.getCertificate(applicationId, referenceId, headers);
	}

}
