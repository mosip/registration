package io.mosip.registration.processor.core.token.validation;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.constant.APIAuthorityList;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.token.validation.dto.TokenResponseDTO;
import io.mosip.registration.processor.core.token.validation.exception.AccessDeniedException;
import io.mosip.registration.processor.core.token.validation.exception.InvalidTokenException;
import io.mosip.registration.processor.core.tracing.ContextualData;
import io.mosip.registration.processor.core.tracing.TracingConstant;
import io.mosip.registration.processor.core.util.JsonUtil;

@Service
public class TokenValidator {
	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(TokenValidator.class);

	/** If token is not valid */
	private static final String INVALIDTOKENMESSAGE = "No Token Available In The Header";

	/** If token is valid */
	private static final String VALIDATEDMESSAGE = "Token Validation Successful For Role: ";

	/**  */
	private static final String ACCESSDENIEDMESSAGE = "Access Denied For Role: ";

	@Autowired
	Environment env;


	public String validate(String token, String url) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"TokenValidator::validate()::entry");
		String userId = "";
		if (token == null)
			throw new InvalidTokenException(INVALIDTOKENMESSAGE);
		try {
			URL obj = new URL(env.getProperty("TOKENVALIDATE"));
			URLConnection urlConnection = obj.openConnection();
			HttpURLConnection con;
			if (urlConnection instanceof HttpsURLConnection) {
				con = (HttpsURLConnection) urlConnection;
			} else {
				con = (HttpURLConnection) urlConnection;
			}
			con.setRequestProperty("Cookie", token);
			con.setRequestProperty(TracingConstant.TRACE_HEADER, (String) ContextualData.getOrDefault(TracingConstant.TRACE_ID_KEY));
			con.setRequestMethod("GET");

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			InputStream responseStream = new ByteArrayInputStream(response.toString().getBytes());
			TokenResponseDTO tokenResponseDTO = (TokenResponseDTO) JsonUtil.inputStreamtoJavaObject(responseStream,
					TokenResponseDTO.class);

			if (tokenResponseDTO.getErrors() != null) {
				throw new InvalidTokenException(tokenResponseDTO.getErrors()[0].getErrorCode(),tokenResponseDTO.getErrors()[0].getMessage());
			} else {
				if (!validateAccess(url, tokenResponseDTO.getResponse().getRole())) {
					regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
							LoggerFileConstant.REGISTRATIONID.toString(), "",
							ACCESSDENIEDMESSAGE + tokenResponseDTO.getResponse().getRole());
					throw new AccessDeniedException(ACCESSDENIEDMESSAGE + tokenResponseDTO.getResponse().getRole());
				}
				userId = tokenResponseDTO.getResponse().getUserId();
				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), VALIDATEDMESSAGE,
						tokenResponseDTO.getResponse().getRole());
			}

		} catch (IOException e) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"", e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw new InvalidTokenException(PlatformErrorMessages.RPR_AUT_INVALID_TOKEN.getCode(),e.getMessage());
		}

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"TokenValidator::validate()::exit");
		return userId;
	}

	public String getRole(String url) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"TokenValidator::validateAccess()::entry");
		if (url.contains("receiver"))
			return String.join(",", APIAuthorityList.PACKETRECEIVER.getList());
		else if (url.contains("securezone"))
			return String.join(",", APIAuthorityList.SECUREZONENOTIFICATION.getList());
		else if (url.contains("workflowaction"))
			return String.join(",", APIAuthorityList.WORKFLOWACTION.getList());
		else if (url.contains("workflow/search"))
			return String.join(",", APIAuthorityList.WORKFLOWSEARCH.getList());
		return null;
	}

	public boolean validateAccess(String url, String role) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"TokenValidator::validateAccess()::entry");
		if (url.contains("receiver")) {
			for (String assignedRole : APIAuthorityList.PACKETRECEIVER.getList()) {
				if (role.contains(assignedRole))
					return true;
			}
		} else if (url.contains("sync")) {
			for (String assignedRole : APIAuthorityList.PACKETSYNC.getList()) {
				if (role.contains(assignedRole))
					return true;
			}
		} else if (url.contains("status")) {
			for (String assignedRole : APIAuthorityList.REGISTRATIONSTATUS.getList()) {
				if (role.contains(assignedRole))
					return true;
			}
		}else if (url.contains("transaction")) {
			for (String assignedRole : APIAuthorityList.REGISTRATIONTRANSACTION.getList()) {
				if (role.contains(assignedRole))
					return true;
			}
		} else if (url.contains("manual")) {
			for (String assignedRole : APIAuthorityList.MANUALVERIFICTION.getList()) {
				if (role.contains(assignedRole))
					return true;
			}
		} else if (url.contains("print")) {
			for (String assignedRole : APIAuthorityList.PRINTSTAGE.getList()) {
				if (role.contains(assignedRole))
					return true;
			}
		} else if (url.contains("biodedupe")) {
			for (String assignedRole : APIAuthorityList.BIODEDUPE.getList()) {
				if (role.contains(assignedRole))
					return true;
			}
		} else if (url.contains("abis")) {
			for (String assignedRole : APIAuthorityList.ABIS.getList()) {
				if (role.contains(assignedRole))
					return true;
			}
		} else if (url.contains("bio")) {
			for (String assignedRole : APIAuthorityList.BIO.getList()) {
				if (role.contains(assignedRole))
					return true;
			}
		} else if (url.contains("securezone")) {
			for (String assignedRole : APIAuthorityList.SECUREZONENOTIFICATION.getList()) {
				if (role.contains(assignedRole))
					return true;
			}
		} else if (url.contains("requesthandler")) {
			for (String assignedRole : APIAuthorityList.REQUESTHANDLER.getList()) {
				if (role.contains(assignedRole))
					return true;
			}
		}
		else if (url.contains("workflowaction")) {
			for (String assignedRole : APIAuthorityList.WORKFLOWACTION.getList()) {
				if (role.contains(assignedRole))
					return true;
			}
		} else if (url.contains("workflow/search")) {
			for (String assignedRole : APIAuthorityList.WORKFLOWSEARCH.getList()) {
				if (role.contains(assignedRole))
					return true;
			}
		} else if (url.contains("packetexternalstatus")) {
			for (String assignedRole : APIAuthorityList.PACKETEXTERNALSTATUS.getList()) {
				if (role.contains(assignedRole))
					return true;
			}
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"TokenValidator::validateAccess()::exit");
	
		return false;
	}
}
