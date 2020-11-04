package io.mosip.registration.processor.status.api.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.mosip.kernel.core.http.ResponseFilter;
import io.mosip.registration.processor.status.service.InternalAuthDelegateService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * The Class InternalAuthDelegateServicesController - The controller exposing
 * the delegate services for internal authentication API calls
 * @author Loganathan Sekar
 */
@RefreshScope
@RestController
@Api(tags = "Internal Auth Delegate Services")
public class InternalAuthDelegateServicesController {

	/** The internal auth delegate service. */
	@Autowired
	private InternalAuthDelegateService internalAuthDelegateService;

	/**
	 * Authenticate.
	 *
	 * @param authRequestDTO the auth request DTO
	 * @return the object
	 * @throws Exception 
	 */
	@PreAuthorize("hasAnyRole('REGISTRATION_ADMIN','REGISTRATION_OFFICER','REGISTRATION_SUPERVISOR')")
	@PostMapping(path = "/auth", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Authenticate Internal Request")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Request authenticated successfully") })
	public Object authenticate(@Validated @RequestBody Object authRequestDTO, @RequestHeader HttpHeaders headers) throws Exception {
		return internalAuthDelegateService.authenticate(authRequestDTO, headers);
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
	public Object getCertificate(@ApiParam("Id of application") @RequestParam("applicationId") String applicationId,
			@ApiParam("Refrence Id as metadata") @RequestParam("referenceId") Optional<String> referenceId, @RequestHeader HttpHeaders headers) throws Exception {
		return internalAuthDelegateService.getCertificate(applicationId, referenceId, headers);
	}

}
