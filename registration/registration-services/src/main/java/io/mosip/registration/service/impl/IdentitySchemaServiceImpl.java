package io.mosip.registration.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.dao.IdentitySchemaDao;
import io.mosip.registration.dto.UiSchemaDTO;
import io.mosip.registration.dto.response.SchemaDto;
import io.mosip.registration.entity.IdentitySchema;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.IdentitySchemaService;

@Service
public class IdentitySchemaServiceImpl implements IdentitySchemaService {
	
	private static final Logger LOGGER = AppConfig.getLogger(IdentitySchemaServiceImpl.class);

	@Autowired
	private IdentitySchemaDao identitySchemaDao;
	
	@Override
	public Double getLatestEffectiveSchemaVersion() throws RegBaseCheckedException {
		return identitySchemaDao.getLatestEffectiveSchemaVersion();
	}

	@Override
	public List<UiSchemaDTO> getLatestEffectiveUISchema() throws RegBaseCheckedException {
		return identitySchemaDao.getLatestEffectiveUISchema();
	}

	@Override
	public String getLatestEffectiveIDSchema() throws RegBaseCheckedException {
		return identitySchemaDao.getLatestEffectiveIDSchema();
	}

	@Override
	public List<UiSchemaDTO> getUISchema(double idVersion) throws RegBaseCheckedException {
		return identitySchemaDao.getUISchema(idVersion);
	}

	@Override
	public String getIDSchema(double idVersion) throws RegBaseCheckedException {
		return identitySchemaDao.getIDSchema(idVersion);
	}

	@Override
	public SchemaDto getIdentitySchema(double idVersion) throws RegBaseCheckedException {
		return identitySchemaDao.getIdentitySchema(idVersion);
	}

}
