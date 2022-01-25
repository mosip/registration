package io.mosip.registration.processor.packet.storage.utils;

import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.rest.client.utils.RestApiClient;
import org.assertj.core.util.Lists;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class IdSchemaUtil {

    private static Map<String, String> categorySubpacketMapping = new HashMap<>();
    private Map<Double, String> idschema = null;
    public static final String RESPONSE = "response";
    public static final String PROPERTIES = "properties";
    public static final String IDENTITY = "identity";
    public static final String SCHEMA_CATEGORY = "fieldCategory";
    public static final String SCHEMA_ID = "id";
    public static final String SCHEMA_TYPE = "type";
    public static final String SCHEMA_REF = "$ref";
    public static final String IDSCHEMA_URL = "IDSCHEMA";
    public static final String SCHEMA_JSON = "schemaJson";
    public static final String SCHEMA_VERSION_QUERY_PARAM = "schemaVersion";
    public static final String SCHEMA_REF_DEFINITIONS_PREFIX = "#/definitions/";

    static {
        categorySubpacketMapping.put("pvt", "id");
        categorySubpacketMapping.put("kyc", "id");
        categorySubpacketMapping.put("none", "id,evidence,optional");
        categorySubpacketMapping.put("evidence", "evidence");
        categorySubpacketMapping.put("optional", "optional");
    }

    @Autowired
    private Environment env;

    @Autowired
    RegistrationProcessorRestClientService<Object> registrationProcessorRestClientService;

    public List<String> getDefaultFields(Double schemaVersion) throws JSONException, ApisResourceAccessException, IOException {
        List<String> fieldList = new ArrayList<>();
        List<Map<String, String>> fieldMapList = loadDefaultFields(schemaVersion);
        fieldMapList.stream().forEach(f -> fieldList.add(f.get(SCHEMA_ID)));
        return fieldList;
    }

    public List<Map<String, String>> loadDefaultFields(Double schemaVersion) throws JSONException, ApisResourceAccessException, IOException {
        Map<String, List<Map<String, String>>> packetBasedMap = new HashMap<String, List<Map<String, String>>>();

        String schemaJson = getIdSchema(schemaVersion);

        JSONObject schema = getIdentityFieldsSchema(schemaJson);

        JSONArray fieldNames = schema.names();
        for(int i=0;i<fieldNames.length();i++) {
            String fieldName = fieldNames.getString(i);
            JSONObject fieldDetail = schema.getJSONObject(fieldName);
            String fieldCategory = fieldDetail.has(SCHEMA_CATEGORY) ?
                    fieldDetail.getString(SCHEMA_CATEGORY) : "none";
            String packets = categorySubpacketMapping.get(fieldCategory.toLowerCase());

            String[] packetNames = packets.split(",");
            for(String packetName : packetNames) {
                if(!packetBasedMap.containsKey(packetName)) {
                    packetBasedMap.put(packetName, new ArrayList<Map<String, String>>());
                }

                Map<String, String> attributes = new HashMap<>();
                attributes.put(SCHEMA_ID, fieldName);
                attributes.put(SCHEMA_TYPE, fieldDetail.has(SCHEMA_REF) ?
                        fieldDetail.getString(SCHEMA_REF) : fieldDetail.getString(SCHEMA_TYPE));
                packetBasedMap.get(packetName).add(attributes);
            }
        }
        return packetBasedMap.get("id");
    }

    public String getIdSchema(Double version) throws ApisResourceAccessException, JSONException, IOException {
        if (idschema != null && !idschema.isEmpty() && idschema.get(version) != null)
            return idschema.get(version);

        String response = (String) registrationProcessorRestClientService.getApi(ApiName.IDSCHEMAURL, null,
                Lists.newArrayList(SCHEMA_VERSION_QUERY_PARAM), Lists.newArrayList(version), String.class);

        if (response == null)
            throw new ApisResourceAccessException("Could not fetch idschema with version : " + version);

        JSONObject jsonObject = new JSONObject(response);
        JSONObject respObj = (JSONObject) jsonObject.get(RESPONSE);
        String responseString = respObj != null ? (String) respObj.get(SCHEMA_JSON) : null;

        if (responseString != null) {
            if (idschema == null) {
                idschema = new HashMap<>();
                idschema.put(version, responseString);
            } else
                idschema.put(version, responseString);
            return idschema.get(version);
        }else {
        	throw new RuntimeException("responseString is null");
        }   
    }

    private JSONObject getIdentityFieldsSchema(String schemaJson) throws JSONException {

        JSONObject schema = new JSONObject(schemaJson);
        schema =  schema.getJSONObject(PROPERTIES);
        schema =  schema.getJSONObject(IDENTITY);
        schema =  schema.getJSONObject(PROPERTIES);

        return schema;
    }

    public Map<String, String> getIdSchemaFieldTypes(Double schemaVersion) throws JSONException, ApisResourceAccessException, IOException {
        Map<String, String> fieldTypesMap = new HashMap<String, String>();

        String schemaJson = getIdSchema(schemaVersion);
        JSONObject schema = getIdentityFieldsSchema(schemaJson);

        JSONArray fieldNames = schema.names();
        for(int i=0;i<fieldNames.length();i++) {
            String fieldName = fieldNames.getString(i);
            JSONObject fieldDetail = schema.getJSONObject(fieldName);
            String fieldType = "";
            if(fieldDetail.has(SCHEMA_REF))
                fieldType = fieldDetail.getString(SCHEMA_REF).replace(SCHEMA_REF_DEFINITIONS_PREFIX, "");
            else
                fieldType = fieldDetail.getString(SCHEMA_TYPE);
            fieldTypesMap.put(fieldName, fieldType);
        }
        return fieldTypesMap;
    }
}
