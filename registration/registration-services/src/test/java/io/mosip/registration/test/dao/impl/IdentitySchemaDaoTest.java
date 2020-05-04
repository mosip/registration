package io.mosip.registration.test.dao.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;




import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;








import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;







import io.mosip.kernel.core.util.DateUtils;

import io.mosip.registration.dao.IdentitySchemaDao;
import io.mosip.registration.dto.response.SchemaDto;
import io.mosip.registration.repositories.IdentitySchemaRepository;
import io.mosip.registration.test.config.TestDaoConfig;

/**
 * @author anusha
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes= {TestDaoConfig.class})
public class IdentitySchemaDaoTest {
	
	@Autowired
	private IdentitySchemaDao identitySchemaDao;
	
	@Autowired
	private IdentitySchemaRepository identitySchemaRepository;
	
	private ObjectMapper mapper = new ObjectMapper();
	
	@Before
	public void setup() {
		mapper.registerModule(new JavaTimeModule());
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
	}
		

	@Test
	public void testSuccessSchemaSync() throws IOException {
		




		SchemaDto dto = getSchemaDto("response_1587846312621.json");		
		
		Double version = identitySchemaRepository.findLatestEffectiveIdVersion(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
		 
		assertNull(version);
		
		identitySchemaDao.createIdentitySchema(dto);		





		
		version = identitySchemaRepository.findLatestEffectiveIdVersion(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));












		assertNotNull(version);
	}
	
	private SchemaDto getSchemaDto(String fileName) {
		SchemaDto schemaDto = null;
		
		try {
			schemaDto = mapper.readValue(new File(getClass().getClassLoader().getResource(fileName).getFile()), 
					SchemaDto.class);
					
		} catch (Exception e) {
			//it could throw exception for invalid json which is part of negative test case
			e.printStackTrace();
		}		
		return schemaDto;
	}

}
