package io.mosip.registration.dao.impl;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_SCHEMA_SYNC;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dao.IdentitySchemaDao;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.dto.response.SchemaDto;
import io.mosip.registration.entity.IdentitySchema;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.repositories.IdentitySchemaRepository;
import io.mosip.registration.util.mastersync.MapperUtils;


@Repository
public class IdentitySchemaDaoImpl implements IdentitySchemaDao {
	
	private static final Logger LOGGER = AppConfig.getLogger(IdentitySchemaDaoImpl.class);
		
	private static final String USER_DIR = "user.dir";	
	private static final String FILE_NAME_PREFIX = "SCHEMA_%s.json";
	
		
	@Autowired
	private IdentitySchemaRepository identitySchemaRepository;
	
	@Override
	public Double getLatestEffectiveSchemaVersion() throws RegBaseCheckedException {
		Double idVersion = identitySchemaRepository.findLatestEffectiveIdVersion(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
		
		if(idVersion == null)
			throw new RegBaseCheckedException(SchemaMessage.SCHEMA_NOT_SYNCED.getCode(), 
					SchemaMessage.SCHEMA_NOT_SYNCED.getMessage());
		
		return idVersion;
	}

	@Override
	public IdentitySchema getLatestEffectiveIdentitySchema() {
		return identitySchemaRepository.findLatestEffectiveIdentitySchema(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
	}

	@Override
	public List<UiSchemaDTO> getLatestEffectiveUISchema() throws RegBaseCheckedException {
		IdentitySchema identitySchema = getLatestEffectiveIdentitySchema();
		
		if(identitySchema == null)
			throw new RegBaseCheckedException(SchemaMessage.SCHEMA_NOT_SYNCED.getCode(), 
					SchemaMessage.SCHEMA_NOT_SYNCED.getMessage());
		
		SchemaDto dto = getSchemaFromFile(identitySchema.getIdVersion(), identitySchema.getFileHash());
		return dto.getSchema();
	}

	@Override
	public String getLatestEffectiveIDSchema() throws RegBaseCheckedException {
		IdentitySchema identitySchema = getLatestEffectiveIdentitySchema();	
		
		if(identitySchema == null)
			throw new RegBaseCheckedException(SchemaMessage.SCHEMA_NOT_SYNCED.getCode(), 
					SchemaMessage.SCHEMA_NOT_SYNCED.getMessage());
		
		SchemaDto dto = getSchemaFromFile(identitySchema.getIdVersion(), identitySchema.getFileHash());
		return dto.getSchemaJson();
	}

	@Override
	public List<UiSchemaDTO> getUISchema(double idVersion) throws RegBaseCheckedException {
		IdentitySchema identitySchema = identitySchemaRepository.findByIdVersion(idVersion);
		
		if(identitySchema == null)
			throw new RegBaseCheckedException(SchemaMessage.SCHEMA_FILE_NOT_FOUND.getCode(), 
					SchemaMessage.SCHEMA_FILE_NOT_FOUND.getMessage());
		
		SchemaDto dto = getSchemaFromFile(identitySchema.getIdVersion(), identitySchema.getFileHash());
		return dto.getSchema();
	}

	@Override
	public String getIDSchema(double idVersion) throws RegBaseCheckedException {
		IdentitySchema identitySchema = identitySchemaRepository.findByIdVersion(idVersion);
		
		if(identitySchema == null)
			throw new RegBaseCheckedException(SchemaMessage.SCHEMA_FILE_NOT_FOUND.getCode(), 
					SchemaMessage.SCHEMA_FILE_NOT_FOUND.getMessage());
		
		SchemaDto dto = getSchemaFromFile(identitySchema.getIdVersion(), identitySchema.getFileHash());
		return dto.getSchemaJson();
	}

	@Override
	public void createIdentitySchema(SchemaDto schemaReponseDto) throws IOException {
		String filePath = getFilePath(schemaReponseDto.getIdVersion());
		String content = MapperUtils.convertObjectToJsonString(schemaReponseDto);
		
		try(FileWriter writer = new FileWriter(filePath)) {
			writer.write(content);
		} 		
		
		IdentitySchema identitySchema = new IdentitySchema();
		identitySchema.setId(schemaReponseDto.getId());
		identitySchema.setEffectiveFrom(Timestamp.valueOf(schemaReponseDto.getEffectiveFrom()));
		identitySchema.setFileName(getFileName(schemaReponseDto.getIdVersion()));
		identitySchema.setIdVersion(schemaReponseDto.getIdVersion());
		identitySchema.setFileHash(CryptoUtil.computeFingerPrint(content, null).toLowerCase());
		
		identitySchemaRepository.save(identitySchema);
	}
	
	private SchemaDto getSchemaFromFile(double idVersion, String originalChecksum) throws RegBaseCheckedException {
		String filePath = getFilePath(idVersion);
		String content = RegistrationConstants.EMPTY;
		
		try {
			content = FileUtils.readFileToString(new File(filePath));
		} catch (IOException e) {
			throw new RegBaseCheckedException(SchemaMessage.SCHEMA_FILE_NOT_FOUND.getCode(), 
					filePath + " : " +ExceptionUtils.getStackTrace(e));
		}
		
		if(!isValidFile(content, originalChecksum))
			throw new RegBaseCheckedException(SchemaMessage.SCHEMA_TAMPERED.getCode(), 
					filePath + " : " +SchemaMessage.SCHEMA_TAMPERED.getMessage());
		
		try {
			SchemaDto dto = MapperUtils.convertJSONStringToDto(content, 
					new TypeReference<SchemaDto>() {});
			return dto;
			
		} catch (IOException e) {
			throw new RegBaseCheckedException(SchemaMessage.SCHEMA_TAMPERED.getCode(), 
					filePath + " : " +SchemaMessage.SCHEMA_TAMPERED.getMessage());
		}
	}
	
	private String getFilePath(double idVersion) {
		String path = System.getProperty(USER_DIR) + File.separator + getFileName(idVersion);
		LOGGER.info(LOG_REG_SCHEMA_SYNC, APPLICATION_NAME, APPLICATION_ID, "SCHEMA :: " + path);
		return path;
	}
	
	private String getFileName(double idVersion) {
		return String.format(FILE_NAME_PREFIX, idVersion);
	}
	
	private boolean isValidFile(String content, String checksum) {
		return checksum.equals(CryptoUtil.computeFingerPrint(content, null).toLowerCase());
	}

	@Override
	public SchemaDto getIdentitySchema(double idVersion) throws RegBaseCheckedException {
		IdentitySchema identitySchema = identitySchemaRepository.findByIdVersion(idVersion);
		
		if(identitySchema == null)
			throw new RegBaseCheckedException(SchemaMessage.SCHEMA_FILE_NOT_FOUND.getCode(), 
					SchemaMessage.SCHEMA_FILE_NOT_FOUND.getMessage());
		
		return getSchemaFromFile(identitySchema.getIdVersion(), identitySchema.getFileHash());
	}

}

enum SchemaMessage {
	
	SCHEMA_NOT_SYNCED("REG-SCHEMA-001", "No Schema Found"),
	SCHEMA_SYNC_FAILED("REG-SCHEMA-002", "Schema sync failed"),
	SCHEMA_FILE_NOT_FOUND("REG-SCHEMA-003", "Synced Schema file not found"),
	SCHEMA_TAMPERED("REG-SCHEMA-004", "Schema is tampered");
	
	SchemaMessage(String code, String message) {
		this.setCode(code);
		this.setMessage(message);
	}
	

	private String code;
	private String message;
	

	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
