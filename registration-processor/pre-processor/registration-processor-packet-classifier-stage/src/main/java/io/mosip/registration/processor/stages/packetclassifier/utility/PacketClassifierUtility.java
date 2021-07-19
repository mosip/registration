package io.mosip.registration.processor.stages.packetclassifier.utility;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;

@Component
public class PacketClassifierUtility {

	/**
	 * The mandatory languages that should be used when dealing with field type that
	 * has values in multiple languages
	 */
	@Value("#{'${mosip.mandatory-languages}'.split(',')}")
	private List<String> mandatoryLanguages;

	/**
	 * The optional languages that should be used when dealing with field type that
	 * has values in multiple languages
	 */
	@Value("#{'${mosip.optional-languages}'.split(',')}")
	private List<String> optionalLanguages;

	/** The constant for language label in JSON parsing */
	private static final String LANGUAGE_LABEL = "language";

	public String getTagLanguageBasedOnFieldDTO(String value) throws JSONException, BaseCheckedException {
		if (!mandatoryLanguages.isEmpty()) {
			for (String language : mandatoryLanguages) {
				JSONArray jsonArray = new JSONArray(value);
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject jsonObject = jsonArray.getJSONObject(i);
					if (jsonObject.getString(LANGUAGE_LABEL).equals(language)) {
						return jsonObject.getString(LANGUAGE_LABEL);
					}
				}
			}
		} else {
			for (String language : optionalLanguages) {
				JSONArray jsonArray = new JSONArray(value);
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject jsonObject = jsonArray.getJSONObject(i);
					if (jsonObject.getString(LANGUAGE_LABEL).equals(language)) {
						return jsonObject.getString(LANGUAGE_LABEL);
					}
				}
			}
		}
		throw new BaseCheckedException(
				PlatformErrorMessages.RPR_PCM_MANDATORY_OR_OPTIONAL_LANGUAGE_NOT_AVAILABLE.getCode(),
				PlatformErrorMessages.RPR_PCM_MANDATORY_OR_OPTIONAL_LANGUAGE_NOT_AVAILABLE.getMessage());
	}
}
