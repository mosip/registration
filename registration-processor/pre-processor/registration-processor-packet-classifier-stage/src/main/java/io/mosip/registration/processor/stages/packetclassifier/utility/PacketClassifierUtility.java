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
	@Value("#{T(java.util.Arrays).asList('${mosip.mandatory-languages:}')}")
	private List<String> mandatoryLanguages;

	/**
	 * The optional languages that should be used when dealing with field type that
	 * has values in multiple languages
	 */
	@Value("#{T(java.util.Arrays).asList('${mosip.optional-languages:}')}")
	private List<String> optionalLanguages;

	/** The constant for language label in JSON parsing */
	private static final String LANGUAGE_LABEL = "language";

	/** The constant for value label in JSON parsing */
	private static final String VALUE_LABEL = "value";

	public String getLanguageBasedValueForSimpleType(String value) throws JSONException, BaseCheckedException {

		if (!mandatoryLanguages.isEmpty()) {
			JSONArray jsonArray = new JSONArray(value);
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				if (jsonObject.getString(LANGUAGE_LABEL).equals(mandatoryLanguages.get(0))) {
					if (jsonObject.isNull(VALUE_LABEL))
						return null;
					return jsonObject.getString(VALUE_LABEL);
				}
			}
		} else {
			for (String language : optionalLanguages) {
				JSONArray jsonArray = new JSONArray(value);
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject jsonObject = jsonArray.getJSONObject(i);
					if (jsonObject.getString(LANGUAGE_LABEL).equals(language)) {
						if (jsonObject.isNull(VALUE_LABEL))
							return null;
						return jsonObject.getString(VALUE_LABEL);
					}
				}
			}
		}
		throw new BaseCheckedException(
				PlatformErrorMessages.RPR_PCM_VALUE_NOT_AVAILABLE_IN_CONFIGURED_LANGUAGE.getCode(),
				PlatformErrorMessages.RPR_PCM_VALUE_NOT_AVAILABLE_IN_CONFIGURED_LANGUAGE.getMessage());
	}
}
