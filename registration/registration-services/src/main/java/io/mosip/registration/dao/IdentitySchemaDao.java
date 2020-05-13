package io.mosip.registration.dao;

import java.io.IOException;
import java.util.List;

import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.dto.response.SchemaDto;
import io.mosip.registration.entity.IdentitySchema;
import io.mosip.registration.exception.RegBaseCheckedException;

public interface IdentitySchemaDao {
	
	public Double getLatestEffectiveSchemaVersion() throws RegBaseCheckedException;
	
	public IdentitySchema getLatestEffectiveIdentitySchema();
	
	public List<UiSchemaDTO> getLatestEffectiveUISchema() throws RegBaseCheckedException;
	
	public String getLatestEffectiveIDSchema() throws RegBaseCheckedException;
	
	public List<UiSchemaDTO> getUISchema(double idVersion) throws RegBaseCheckedException;
	
	public String getIDSchema(double idVersion) throws RegBaseCheckedException;
	
	public void createIdentitySchema(SchemaDto schemaDto) throws IOException;
	
	public SchemaDto getIdentitySchema(double idVersion) throws RegBaseCheckedException;

}
