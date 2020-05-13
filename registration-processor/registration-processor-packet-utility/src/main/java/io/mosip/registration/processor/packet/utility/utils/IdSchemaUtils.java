/**
 * 
 */
package io.mosip.registration.processor.packet.utility.utils;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.registration.processor.packet.utility.exception.ApiNotAccessibleException;
import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import io.mosip.registration.processor.packet.utility.constants.IDschemaConstants;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;


/**
 * The Class IdSchemaUtils.
 *
 * @author Sowmya
 */

/**
 * Instantiates a new id schema utils.
 */
@Component
public class IdSchemaUtils {

	@Autowired
	private RestUtil restUtil;

	@Autowired
	private Environment env;

	@Autowired
	ObjectMapper mapper;

	private String idschema = null;

	/**
	 * Gets the source.
	 *
	 * @param id the id
	 * @return the source
	 * @throws IOException 
	 */
	public String getSource(String id) throws IOException, ApiNotAccessibleException {
		String fieldCategory=null;
		String idSchema = getIdSchema();

		JSONObject idSchemaJsonObject;
		idSchemaJsonObject = JsonUtil.objectMapperReadValue(idSchema, JSONObject.class);
		JSONArray schemaArray = JsonUtil.getJSONArray(idSchemaJsonObject, IDschemaConstants.SCHEMA);

		for (Object jsonObject : schemaArray) {

			JSONObject json = new JSONObject((Map) jsonObject);
			fieldCategory=IdSchemaUtils.getFieldCategory(json, id);
			if(fieldCategory!=null)
			 break;

		}

		return fieldCategory;
	}

	public String getIdSchema() throws ApiNotAccessibleException, IOException {
		if (idschema != null && !idschema.isEmpty())
			return idschema;
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(env.getProperty("IDSCHEMA"));
		UriComponents uriComponents = builder.build(false).encode();
		String response = restUtil.getApi(uriComponents.toUri(), String.class);
		String responseString = null;
		try {
			org.json.JSONObject jsonObject = new org.json.JSONObject(response);
			org.json.JSONObject respObj = (org.json.JSONObject) jsonObject.get("response");
			responseString = respObj != null ? (String) respObj.get("schemaJson") : null;
		} catch (JSONException e) {
			throw new IOException(e);
		}

		if (responseString != null)
			idschema = responseString;
		else
			throw new ApiNotAccessibleException("Could not get id schema");

		return idschema;
	}

	/**
	 * Gets the json.
	 *
	 * @param configServerFileStorageURL
	 *            the config server file storage URL
	 * @param uri
	 *            the uri
	 * @return the json
	 */
	public static String getJson(String configServerFileStorageURL, String uri) {
		RestTemplate restTemplate = new RestTemplate();
		return restTemplate.getForObject(configServerFileStorageURL + uri, String.class);
	}

	/**
	 * Gets the field category.
	 *
	 * @param jsonObject the json object
	 * @param id the id
	 * @return the field category
	 */
	private  static String getFieldCategory(JSONObject jsonObject,String id){
		String fieldCategory=null;
		String idvalue = JsonUtil.getJSONValue(jsonObject, IDschemaConstants.ID);
		if(idvalue!=null && id.equalsIgnoreCase(idvalue)) {
		  fieldCategory=JsonUtil.getJSONValue(jsonObject, IDschemaConstants.FIELDCATEGORY);
		   if(fieldCategory.equalsIgnoreCase("pvt")) {
				fieldCategory=IDschemaConstants.ID;
		  }
		}
		return fieldCategory;

	}
}
