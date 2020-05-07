package io.mosip.registration.processor.packet.utility.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class provides JSON utilites.
 *
 * @author Pranav Kumar
 * @since 0.0.1
 */
public class JsonUtil {

	/** The Constant LANGUAGE. */
	private static final String LANGUAGE = "language";

	/** The Constant VALUE. */
	private static final String VALUE = "value";

	/**
	 * Instantiates a new json util.
	 */
	private JsonUtil() {

	}


	/**
	 * This method returns the Json Object as value from identity.json
	 * object(JSONObject). jsonObject -> then identity demographic json object key
	 * -> demographic json label name EX:- demographicIdentity : { "identity" : {
	 * "fullName" : [ { "language" : "eng", "value" : "Taleev Aalam" }, {
	 * "language": "ara", "value" : "Taleev Aalam" } ] }
	 *
	 * method call :- getJSONObject(demographicIdentity,identity)
	 *
	 * @param jsonObject
	 *            the json object
	 * @param key
	 *            the key
	 * @return the JSON object
	 */
	public static JSONObject getJSONObject(JSONObject jsonObject, Object key) {
		if(jsonObject == null)
			return null;
		LinkedHashMap identity = (LinkedHashMap) jsonObject.get(key);
		return identity != null ? new JSONObject(identity) : null;
	}

	/**
	 * This method returns JSONArray from JSONObject. argument 'jsonObject' ->
	 * demographic identity json as JSONObject. argument key -> label name of
	 * demographic identity json. Ex:- "identity" : { "fullName" : [ { "language" :
	 * "eng", "value" : "Taleev Aalam" }, { "language" : "ara", "value" : "Taleev
	 * Aalam" } ] }
	 *
	 * @param jsonObject
	 *            the json object
	 * @param key
	 *            the key
	 * @return the JSON array
	 */
	public static JSONArray getJSONArray(JSONObject jsonObject, Object key) {
		ArrayList value = (ArrayList) jsonObject.get(key);
		if (value == null)
			return null;
		JSONArray jsonArray = new JSONArray();
		jsonArray.addAll(value);

		return jsonArray;

	}

	/**
	 * Gets the JSON value.
	 *
	 * @param <T>
	 *            the generic type
	 * @param jsonObject
	 *            the json object
	 * @param key
	 *            the key
	 * @return the JSON value
	 */
	public static <T> T getJSONValue(JSONObject jsonObject, String key) {
		if(jsonObject == null)
			return null;
		T value = (T) jsonObject.get(key);
		return value;
	}

	/**
	 * Iterates the JSONArray and returns JSONObject for given index.
	 *
	 * @param jsonObject
	 *            the json object
	 * @param key
	 *            the key
	 * @return the JSON object
	 */
	public static JSONObject getJSONObjectFromArray(JSONArray jsonObject, int key) {
		Object object = jsonObject.get(key);
		if(object instanceof LinkedHashMap) {
			LinkedHashMap identity = (LinkedHashMap) jsonObject.get(key);
			return identity != null ? new JSONObject(identity) : null;
		}else {
			return (JSONObject)object;
		}
	}
	/**
	 * Object mapper read value. This method maps the jsonString to particular type
	 * 
	 * @param <T>
	 *            the generic type
	 * @param jsonString
	 *            the json string
	 * @param clazz
	 *            the clazz
	 * @return the t
	 * @throws JsonParseException
	 *             the json parse exception
	 * @throws JsonMappingException
	 *             the json mapping exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T objectMapperReadValue(String jsonString, Class<?> clazz) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		return (T) objectMapper.readValue(jsonString, clazz);
	}



	
	
	public static String objectMapperObjectToJson(Object obj) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.writeValueAsString(obj);
	} 

}
