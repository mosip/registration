package io.mosip.registration.service.external.impl;

import static io.mosip.registration.constants.LoggerConstants.LOG_PKT_STORAGE;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;
import static io.mosip.registration.constants.RegistrationConstants.ZIP_FILE_EXTENSION;
import static io.mosip.registration.exception.RegistrationExceptionConstants.REG_IO_EXCEPTION;
import static java.io.File.separator;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.crypto.SecretKey;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.security.constants.MosipSecurityMethod;
import io.mosip.kernel.core.security.decryption.MosipDecryptor;
import io.mosip.kernel.core.security.encryption.MosipEncryptor;
import io.mosip.kernel.core.util.FileUtils;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.core.util.exception.JsonMappingException;
import io.mosip.kernel.core.util.exception.JsonParseException;
import io.mosip.kernel.keygenerator.bouncycastle.KeyGenerator;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.DocumentTypeDAO;
import io.mosip.registration.dao.MasterSyncDao;
import io.mosip.registration.dto.PreRegistrationDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.dto.demographic.DocumentDetailsDTO;
import io.mosip.registration.dto.demographic.IndividualIdentity;
import io.mosip.registration.dto.demographic.ValuesDTO;
import io.mosip.registration.dto.mastersync.DocumentCategoryDto;
import io.mosip.registration.entity.DocumentType;
import io.mosip.registration.entity.ValidDocument;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.kernel.packetmanager.dto.DocumentDto;
import io.mosip.kernel.packetmanager.dto.SimpleDto;
import io.mosip.registration.service.IdentitySchemaService;
import io.mosip.registration.service.external.PreRegZipHandlingService;
import io.mosip.registration.util.mastersync.MapperUtils;

/**
 * This implementation class to handle the pre-registration data
 * 
 * @author balamurugan ramamoorthy
 * @since 1.0.0
 *
 */
@Service
public class PreRegZipHandlingServiceImpl implements PreRegZipHandlingService {

	@Autowired
	private KeyGenerator keyGenerator;

	@Autowired
	private DocumentTypeDAO documentTypeDAO;

	@Autowired
	private MasterSyncDao masterSyncDao;
	
	@Autowired
	private IdentitySchemaService identitySchemaService;
	

	private static final Logger LOGGER = AppConfig.getLogger(PreRegZipHandlingServiceImpl.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.external.PreRegZipHandlingService#
	 * extractPreRegZipFile(byte[])
	 */
	@Override
	public RegistrationDTO extractPreRegZipFile(byte[] preRegZipFile) throws RegBaseCheckedException {
		LOGGER.debug("PRE_REG_ZIP_HANDLING_SERVICE_IMPL", RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
				"extractPreRegZipFile invoked");
		try{
			BufferedReader bufferedReader = null;
			try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(preRegZipFile))) {
				ZipEntry zipEntry;
				while ((zipEntry = zipInputStream.getNextEntry()) != null) {
					if (zipEntry.getName().equalsIgnoreCase("ID.json")) {
						bufferedReader = new BufferedReader(new InputStreamReader(zipInputStream, StandardCharsets.UTF_8));
						parseDemographicJson(bufferedReader, zipEntry);						
					}	
				}
			}finally {
				if(bufferedReader != null) {
					bufferedReader.close();
					bufferedReader = null;
				}				
			}
			
			try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(preRegZipFile))) {
				ZipEntry zipEntry;
				while ((zipEntry = zipInputStream.getNextEntry()) != null) {
					String fileName = zipEntry.getName();
					//if (zipEntry.getName().contains("_")) {
					LOGGER.debug("PRE_REG_ZIP_HANDLING_SERVICE_IMPL", RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
							"extractPreRegZipFile zipEntry >>>> " + fileName);				
						Optional<Map.Entry<String, DocumentDto>> result = getRegistrationDtoContent().getDocuments().entrySet().stream()
								.filter(e -> fileName.equals(e.getValue().getValue().concat(".").concat(e.getValue().getFormat()))).findFirst();
						if(result.isPresent()) {
							DocumentDto documentDto = result.get().getValue();
							documentDto.setDocument(IOUtils.toByteArray(zipInputStream));
							
							List<DocumentType> documentTypes = documentTypeDAO.getDocTypeByName(documentDto.getType());
							if(Objects.nonNull(documentTypes) && !documentTypes.isEmpty()) {
								LOGGER.debug("PRE_REG_ZIP_HANDLING_SERVICE_IMPL", RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
										documentDto.getType() + " >>>> documentTypes.get(0).getCode() >>>> " + documentTypes.get(0).getCode());	
								documentDto.setType(documentTypes.get(0).getCode());
								documentDto.setValue(documentDto.getCategory().concat("_").concat(documentDto.getType()));
							}							
							getRegistrationDtoContent().addDocument(result.get().getKey(), result.get().getValue());
							LOGGER.debug("PRE_REG_ZIP_HANDLING_SERVICE_IMPL", RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
									"Added zip entry as document for field >>>> " + result.get().getKey());	
						}
					//}	
				}
			}
			
			Set<Entry<String, DocumentDto>> entries = getRegistrationDtoContent().getDocuments().entrySet();
			entries.stream()
				.filter(e -> e.getValue().getDocument() == null || e.getValue().getDocument().length == 0)
				.forEach(e -> { getRegistrationDtoContent().removeDocument(e.getKey()); });
		
		} catch (IOException exception) {
			exception.printStackTrace();
			LOGGER.error("REGISTRATION - PRE_REG_ZIP_HANDLING_SERVICE_IMPL", RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					exception.getMessage() + ExceptionUtils.getStackTrace(exception));
			throw new RegBaseCheckedException(REG_IO_EXCEPTION.getErrorCode(), exception.getCause().getMessage());
		} catch (RuntimeException exception) {
			exception.printStackTrace();
			LOGGER.error("REGISTRATION - PRE_REG_ZIP_HANDLING_SERVICE_IMPL - PRE_REGISTRATION_DATA_SYNC_SERVICE_IMPL",
					RegistrationConstants.APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					exception.getMessage() + ExceptionUtils.getStackTrace(exception));
			throw new RegBaseUncheckedException(RegistrationConstants.PACKET_ZIP_CREATION, exception.getMessage());
		}
		return getRegistrationDtoContent();
	}
	
	
	private void attachDocument(DocumentDto documentDetailsDTO, ZipInputStream zipInputStream, String fileName,
			String docCatgory) throws IOException {
		documentDetailsDTO.setDocument(IOUtils.toByteArray(zipInputStream));
		documentDetailsDTO.setFormat(fileName.substring(fileName.lastIndexOf(RegistrationConstants.DOT) + 1));

		String docTypeName = getDocTypeName(fileName, docCatgory);

		/*
		 * checking and setting the doc type name based on the reg client primary
		 * language irrespective of pre reg language
		 */
		docTypeName = getDocTypeForPrimaryLanguage(docTypeName);
		documentDetailsDTO.setType(docTypeName);
		documentDetailsDTO.setValue(docCatgory.concat("_").concat(docTypeName));
	}

	private String getDocTypeForPrimaryLanguage(String docTypeName) {
		if (StringUtils.isNotEmpty(docTypeName)) {
			List<DocumentType> documentTypes = documentTypeDAO.getDocTypeByName(docTypeName);
			if (isListNotEmpty(documentTypes)
					&& !ApplicationContext.applicationLanguage().equalsIgnoreCase(documentTypes.get(0).getLangCode())) {
				List<DocumentType> docTypesForPrimaryLanguage = masterSyncDao.getDocumentTypes(
						Arrays.asList(documentTypes.get(0).getCode()), ApplicationContext.applicationLanguage());
				if (isListNotEmpty(docTypesForPrimaryLanguage)) {
					docTypeName = docTypesForPrimaryLanguage.get(0).getCode();
				}
			}
		}
		return docTypeName;
	}

	private String getDocTypeName(String fileName, String docCatgory) {
		String docTypeName;
		if (RegistrationConstants.POA_DOCUMENT.equalsIgnoreCase(docCatgory)
				&& null != getRegistrationDtoContent().getDocuments().get(RegistrationConstants.POA_DOCUMENT)) {

			docTypeName = getRegistrationDtoContent().getDocuments().get(RegistrationConstants.POA_DOCUMENT).getType();

		} else if (RegistrationConstants.POI_DOCUMENT.equalsIgnoreCase(docCatgory)
				&& null != getRegistrationDtoContent().getDocuments().get(RegistrationConstants.POI_DOCUMENT)) {
			docTypeName = getRegistrationDtoContent().getDocuments().get(RegistrationConstants.POI_DOCUMENT).getType();

		} else if (RegistrationConstants.POR_DOCUMENT.equalsIgnoreCase(docCatgory)
				&& null != getRegistrationDtoContent().getDocuments().get(RegistrationConstants.POR_DOCUMENT)) {
			docTypeName = getRegistrationDtoContent().getDocuments().get(RegistrationConstants.POR_DOCUMENT).getType();

		} else if (RegistrationConstants.DOB_DOCUMENT.equalsIgnoreCase(docCatgory)
				&& null != getRegistrationDtoContent().getDocuments().get(RegistrationConstants.DOB_DOCUMENT)) {
			docTypeName = getRegistrationDtoContent().getDocuments().get(RegistrationConstants.DOB_DOCUMENT).getType();

		} else {
			docTypeName = fileName.substring(fileName.indexOf("_") + 1, fileName.lastIndexOf("."));
		}
		return docTypeName;
	}

	/**
	 * This method is used to parse the demographic json and converts it into
	 * RegistrationDto
	 * 
	 * @param bufferedReader
	 *            - reader for text file
	 * @param zipEntry
	 *            - a file entry in zip
	 * @throws RegBaseCheckedException
	 *             - holds the cheked exceptions
	 */
	@SuppressWarnings("unchecked")
	private void parseDemographicJson(BufferedReader bufferedReader, ZipEntry zipEntry) throws RegBaseCheckedException {

		try {
			
			String value;
			StringBuilder jsonString = new StringBuilder();
			while ((value = bufferedReader.readLine()) != null) {
				jsonString.append(value);
			}
			
			if (!StringUtils.isEmpty(jsonString) && validateDemographicInfoObject()) {
				JSONObject jsonObject = (JSONObject) new JSONObject(jsonString.toString()).get("identity");
				
				LOGGER.debug("REGISTRATION - PRE_REG_ZIP_HANDLING_SERVICE_IMPL", RegistrationConstants.APPLICATION_NAME,
						RegistrationConstants.APPLICATION_ID, jsonString.toString());
				
				if(!jsonObject.has("IDSchemaVersion"))
					throw new RegBaseCheckedException("IDSchemaVersion not found", "IDSchemaVersion not found");
				
				List<UiSchemaDTO> fieldList = identitySchemaService.getUISchema(jsonObject.getDouble("IDSchemaVersion"));	
				getRegistrationDtoContent().setIdSchemaVersion(jsonObject.getDouble("IDSchemaVersion"));
							
				for(UiSchemaDTO field : fieldList) {
					if(field.getId().equalsIgnoreCase("IDSchemaVersion"))
						continue;
					
					switch (field.getType()) {
					case "documentType":
						DocumentDto documentDto = new DocumentDto();
						if(jsonObject.has(field.getId()) && jsonObject.get(field.getId()) != null) {
							JSONObject fieldValue = jsonObject.getJSONObject(field.getId());							
							documentDto.setCategory(field.getSubType());
							documentDto.setOwner("Applicant");
							documentDto.setFormat(fieldValue.getString("format"));
							documentDto.setType(fieldValue.getString("type"));
							documentDto.setValue(fieldValue.getString("value"));
							getRegistrationDtoContent().addDocument(field.getId(), documentDto);
						}
						break;
						
					case "biometricsType":						
						break;

					default:
						Object fieldValue = getValueFromJson(field.getId(), field.getType(), jsonObject);
						if(fieldValue != null) {
							if(field.getControlType().equalsIgnoreCase("ageDate"))
								getRegistrationDtoContent().setDateField(field.getId(), (String)fieldValue);
							else
								getRegistrationDtoContent().getDemographics().put(field.getId(), fieldValue);
						}
						break;
					}
					
					if(field.getType() != "documentType" && field.getType() != "biometricsType") {
						Object fieldValue = getValueFromJson(field.getId(), field.getType(), jsonObject);
						if(fieldValue != null) {
							if(field.getControlType().equalsIgnoreCase("ageDate"))
								getRegistrationDtoContent().setDateField(field.getId(), (String)fieldValue);
							else
								getRegistrationDtoContent().getDemographics().put(field.getId(), fieldValue);
						}
					}
				}
			}
		} catch (JSONException | IOException e) {
			LOGGER.error("REGISTRATION - PRE_REG_ZIP_HANDLING_SERVICE_IMPL", RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, ExceptionUtils.getStackTrace(e));
			throw new RegBaseCheckedException(REG_IO_EXCEPTION.getErrorCode(), e.getMessage());
		}
	}
	
	
	private Object getValueFromJson(String key, String fieldType, JSONObject jsonObject) throws IOException, JSONException {
		if(!jsonObject.has(key))
			return null;
			
		switch (fieldType) {
		case "string":	return jsonObject.getString(key);
		case "integer":	return jsonObject.getInt(key);
		case "number": return jsonObject.getLong(key);
		case "simpleType": 
			List<SimpleDto> list = new ArrayList<SimpleDto>(); 
			for(int i=0;i<jsonObject.getJSONArray(key).length();i++) {
				JSONObject object = jsonObject.getJSONArray(key).getJSONObject(i);
				list.add(new SimpleDto(object.getString("language"), object.getString("value")));
			}
			return list;
		}
		return null;
	}

	private boolean validateDemographicInfoObject() {
		return null != getRegistrationDtoContent() && getRegistrationDtoContent().getDemographics() != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.external.PreRegZipHandlingService#
	 * encryptAndSavePreRegPacket(java.lang.String, byte[])
	 */
	@Override
	public PreRegistrationDTO encryptAndSavePreRegPacket(String preRegistrationId, byte[] preRegPacket)
			throws RegBaseCheckedException {

		SecretKey symmetricKey = keyGenerator.getSymmetricKey();

		// Encrypt the Pre reg packet data using AES
		final byte[] encryptedData = MosipEncryptor.symmetricEncrypt(symmetricKey.getEncoded(), preRegPacket,
				MosipSecurityMethod.AES_WITH_CBC_AND_PKCS7PADDING);

		LOGGER.info(LOG_PKT_STORAGE, APPLICATION_NAME, APPLICATION_ID, "Pre Registration packet Encrypted");

		String filePath = storePreRegPacketToDisk(preRegistrationId, encryptedData);

		PreRegistrationDTO preRegistrationDTO = new PreRegistrationDTO();
		preRegistrationDTO.setPacketPath(filePath);
		preRegistrationDTO.setSymmetricKey(Base64.getEncoder().encodeToString(symmetricKey.getEncoded()));
		preRegistrationDTO.setEncryptedPacket(encryptedData);
		preRegistrationDTO.setPreRegId(preRegistrationId);
		return preRegistrationDTO;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.external.PreRegZipHandlingService#
	 * storePreRegPacketToDisk(java.lang.String, byte[])
	 */
	@Override
	public String storePreRegPacketToDisk(String preRegistrationId, byte[] encryptedPacket)
			throws RegBaseCheckedException {
		try {
			// Generate the file path for storing the Encrypted Packet
			String filePath = String
					.valueOf(ApplicationContext.map().get(RegistrationConstants.PRE_REG_PACKET_LOCATION))
					.concat(separator).concat(preRegistrationId).concat(ZIP_FILE_EXTENSION);
			// Storing the Encrypted Registration Packet as zip
			FileUtils.copyToFile(new ByteArrayInputStream(encryptedPacket),
					FileUtils.getFile(FilenameUtils.getFullPath(filePath) + FilenameUtils.getName(filePath)));

			LOGGER.info(LOG_PKT_STORAGE, APPLICATION_NAME, APPLICATION_ID, "Pre Registration Encrypted packet saved");

			return filePath;
		} catch (io.mosip.kernel.core.exception.IOException exception) {
			LOGGER.error("REGISTRATION - PRE_REG_ZIP_HANDLING_SERVICE_IMPL", RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					exception.getMessage() + ExceptionUtils.getStackTrace(exception));
			throw new RegBaseCheckedException(REG_IO_EXCEPTION.getErrorCode(),
					REG_IO_EXCEPTION.getErrorMessage() + ExceptionUtils.getStackTrace(exception));
		} catch (RuntimeException runtimeException) {
			LOGGER.error("REGISTRATION - PRE_REG_ZIP_HANDLING_SERVICE_IMPL", RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
			throw new RegBaseUncheckedException(RegistrationConstants.ENCRYPTED_PACKET_STORAGE,
					runtimeException.toString(), runtimeException);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.external.PreRegZipHandlingService#
	 * decryptPreRegPacket(java.lang.String, byte[])
	 */
	@Override
	public byte[] decryptPreRegPacket(String symmetricKey, byte[] encryptedPacket) {

		return MosipDecryptor.symmetricDecrypt(Base64.getDecoder().decode(symmetricKey), encryptedPacket,
				MosipSecurityMethod.AES_WITH_CBC_AND_PKCS7PADDING);
	}

	private RegistrationDTO getRegistrationDtoContent() {
		return (RegistrationDTO) SessionContext.map().get(RegistrationConstants.REGISTRATION_DATA);
	}

	private boolean isListNotEmpty(List<?> values) {
		return values != null && !values.isEmpty();
	}
}
