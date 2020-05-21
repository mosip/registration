/**
 * 
 */
package io.mosip.registration.processor.stages.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.idjson.Document;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.exception.IdentityNotFoundException;
import io.mosip.registration.processor.packet.storage.utils.Utilities;

/**
 * @author M1022006
 * @author Girish Yarru
 */
@Component
public class DocumentUtility {

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(DocumentUtility.class);


	@Autowired
	private Utilities utility;

	public List<Document> getDocumentList(byte[] bytes) throws IOException {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"DocumentUtility::getDocumentList()::entry");
		List<Document> documentList = new ArrayList<>();
		JSONObject documentPOAnode;
		JSONObject documentPOInode;
		JSONObject documentPORnode;
		JSONObject documentPOBnode;

		String demographicJsonString = new String(bytes);
		JSONObject mappingJson = utility.getRegistrationProcessorMappingJson();
		JSONObject demographicJson = (JSONObject) JsonUtil.objectMapperReadValue(demographicJsonString,
				JSONObject.class);
		String poAValue = JsonUtil.getJSONValue(JsonUtil.getJSONObject(mappingJson, MappingJsonConstants.POA),MappingJsonConstants.VALUE);
		String poIValue = JsonUtil.getJSONValue(JsonUtil.getJSONObject(mappingJson, MappingJsonConstants.POI),MappingJsonConstants.VALUE);
		String poRValue = JsonUtil.getJSONValue(JsonUtil.getJSONObject(mappingJson, MappingJsonConstants.POR),MappingJsonConstants.VALUE);
		String poBValue = JsonUtil.getJSONValue(JsonUtil.getJSONObject(mappingJson, MappingJsonConstants.POB),MappingJsonConstants.VALUE);
		JSONObject demographicIdentity = JsonUtil.getJSONObject(demographicJson,
				utility.getGetRegProcessorDemographicIdentity());
		if (demographicIdentity == null)
			throw new IdentityNotFoundException(PlatformErrorMessages.RPR_PIS_IDENTITY_NOT_FOUND.getMessage());
		documentPOAnode = JsonUtil.getJSONObject(demographicIdentity, poAValue);
		documentPOInode = JsonUtil.getJSONObject(demographicIdentity, poIValue);
		documentPORnode = JsonUtil.getJSONObject(demographicIdentity, poRValue);
		documentPOBnode = JsonUtil.getJSONObject(demographicIdentity, poBValue);

		if (documentPOAnode != null) {
			documentList.add(getDocument(documentPOAnode, poAValue));
		}
		if (documentPOInode != null) {
			documentList.add(getDocument(documentPOInode, poIValue));
		}
		if (documentPORnode != null) {
			documentList.add(getDocument(documentPORnode, poRValue));
		}
		if (documentPOBnode != null) {
			documentList.add(getDocument(documentPOBnode, poBValue));
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"DocumentUtility::getDocumentList()::end()");
		return documentList;

	}

	private Document getDocument(JSONObject jsonNode, String category) {
		Document document = new Document();
		document.setDocumentCategory(category);
		document.setDocumentType((String) jsonNode.get(MappingJsonConstants.TYPE));
		document.setFormat((String) jsonNode.get(MappingJsonConstants.FORMAT));
		document.setDocumentName((String) jsonNode.get(MappingJsonConstants.VALUE));
		return document;
	}

}
