package io.mosip.registration.processor.status.service;

import static org.mockito.ArgumentMatchers.anyString;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.registration.processor.status.entity.AnonymousProfileEntity;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.repositary.BaseRegProcRepository;
import io.mosip.registration.processor.status.service.impl.AnonymousProfileServiceImpl;
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest(JsonUtils.class)
public class AnonymousProfileServiceImplTest {

	@Mock
	private BaseRegProcRepository<AnonymousProfileEntity, String> anonymousProfileRepository;
	
	@InjectMocks
	private AnonymousProfileService AnonymousProfileService=new AnonymousProfileServiceImpl();
	
	@Before
	public void setup() {
		Mockito.when(anonymousProfileRepository.save(Mockito.any(AnonymousProfileEntity.class))).thenReturn(new AnonymousProfileEntity());
	}
	@Test
	public void saveAnonymousProfileTest() {

		AnonymousProfileService.saveAnonymousProfile("123", "SYNC", "aa");
	}

	@Test(expected = TablenotAccessibleException.class)
	public void saveAnonymousProfileDataAccessLayerExceptionTest() {

		Mockito.when(anonymousProfileRepository.save(Mockito.any()))
				.thenThrow(new DataAccessLayerException("", "", new TablenotAccessibleException()));

		AnonymousProfileService.saveAnonymousProfile("123", "SYNC", "aa");

	}
}
