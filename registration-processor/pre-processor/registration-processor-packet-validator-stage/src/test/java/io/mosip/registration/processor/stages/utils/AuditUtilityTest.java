package io.mosip.registration.processor.stages.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.utils.IOUtils;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.registration.processor.core.packet.dto.AuditDTO;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.stages.helper.RestHelperImpl;


/**
 * The Class AuditUtilityTest.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ IOUtils.class, Utilities.class })
public class AuditUtilityTest {

	@Mock
	RestHelperImpl restHelper;
	
	@InjectMocks
	AuditUtility auditUtility;
	
	@Mock
	Environment env;
	

	@Mock
	private ObjectMapper mapper;
	
	private InputStream auditStream;
	
	
	@Test
	public void saveAuditDetailSuccessTest() throws Exception {	
		ClassLoader classLoader = getClass().getClassLoader();
		File auditJson = new File(classLoader.getResource("audit.json").getFile());
		auditStream = new FileInputStream(auditJson);
		
		AuditDTO audit =  new AuditDTO();
		audit.setCreatedAt(LocalDateTime.now());
		audit.setActionTimeStamp(LocalDateTime.now());
		List<AuditDTO> regClientAuditDTOs= new ArrayList<>();
		regClientAuditDTOs.add(audit);
		// Mockito.when(fileSystemManager.getFile("2018701130000410092018110735",PacketFiles.AUDIT.name())).thenReturn(auditStream);
		
		
		auditUtility.saveAuditDetails("2018701130000410092018110735","id");
	}
	
	@Test
	public void saveAuditDetailFailureTest() throws Exception {	
		auditUtility.saveAuditDetails(null,null);
	}
	

}

