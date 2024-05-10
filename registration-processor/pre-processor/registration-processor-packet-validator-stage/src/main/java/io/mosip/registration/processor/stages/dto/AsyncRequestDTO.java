package io.mosip.registration.processor.stages.dto;

import lombok.Data;

import java.util.Map;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;

@Data
public class AsyncRequestDTO {

	@Pattern(regexp = "<\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]>", message = "{mosip.rest.request.uri.message}")
	@NotNull
	private String uri;

	MultiValueMap<String, String> params;

	Map<String, String> pathVariables;

	@NotNull
	private HttpMethod httpMethod;

	private Object requestBody;

	@NotNull
	private Class<?> responseType;

	@NotNull
	private HttpHeaders headers;

	@Pattern(regexp = "^[0-9]*$", message = "{mosip.rest.request.timeout.message}")
	private Integer timeout;
}
