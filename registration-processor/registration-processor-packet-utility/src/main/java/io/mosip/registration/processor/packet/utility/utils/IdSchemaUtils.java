/**
 * 
 */
package io.mosip.registration.processor.packet.utility.utils;

import java.io.IOException;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;


import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.util.JsonUtil;

import io.mosip.registration.processor.packet.utility.constants.IDschemaConstants;
import lombok.Data;


/**
 * The Class IdSchemaUtils.
 *
 * @author Sowmya
 */

/**
 * Instantiates a new id schema utils.
 */
@Data
public class IdSchemaUtils {
	
	/** The config server file storage URL. */
	@Value("${config.server.file.storage.uri}")
	private String configServerFileStorageURL;
	
	/** The registration processor abis json. */
	@Value("${idschema.json}")
	private String idSchemaJson;

	/**
	 * Gets the source.
	 *
	 * @param id the id
	 * @return the source
	 * @throws RegistrationProcessorCheckedException the registration processor checked exception
	 */
	public String getSource(String id) throws RegistrationProcessorCheckedException {
		String fieldCategory=null;
		String idSchema =IdSchemaUtils.getJson(configServerFileStorageURL, idSchemaJson);
		
		JSONObject idSchemaJsonObject;
		try {
			idSchemaJsonObject = JsonUtil.objectMapperReadValue(idSchema, JSONObject.class);
			JSONArray schemaArray = JsonUtil.getJSONArray(idSchemaJsonObject, IDschemaConstants.SCHEMA);
			

			for (Object jsonObject : schemaArray) {
				
				JSONObject json = new JSONObject((Map) jsonObject);
				fieldCategory=IdSchemaUtils.getFieldCategory(json, id);
				if(fieldCategory!=null)
				 break;
			  
			}
		}
		 catch (IOException e) {
			 throw new RegistrationProcessorCheckedException(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getCode(),
						PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage(), e);
		}

		
		return fieldCategory;
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
	public  static String getFieldCategory(JSONObject jsonObject,String id){
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
