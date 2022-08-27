package io.mosip.registration.processor.packet.storage.utils;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.mosip.kernel.biometrics.commons.CbeffValidator;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biosdk.provider.factory.BioAPIFactory;
import io.mosip.kernel.biosdk.provider.spi.iBioProviderApi;
import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.idrepo.dto.Documents;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Utilities.class, CbeffValidator.class })
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*","com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
public class BioSdkUtilTest {
	
	
	@InjectMocks
	private BioSdkUtil bioSdkUtil;

	@Mock
	private Utilities utilities;
	
	@Mock
	private BioAPIFactory bio;
	
	@Mock 
	private iBioProviderApi bioProvider;
	
	@Mock
	RegistrationExceptionMapperUtil registrationStatusMapperUtil;
	
	private InternalRegistrationStatusDto regist=new InternalRegistrationStatusDto();
	
	
	List<BIR> l=new ArrayList<>();
	String data="adjfgJHDFKjhsdfjksdbf";
	@Before
	public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		
		List<Documents> ld=new ArrayList<>();
		Documents d=new Documents(MappingJsonConstants.INDIVIDUAL_BIOMETRICS, data);
		ld.add(d);
		when(utilities.retrieveIdrepoDocument(Mockito.anyString())).thenReturn(ld);
		when(bio.getBioProvider(Mockito.any(), Mockito.any())).thenReturn(bioProvider);
		when(bioProvider.verify(Mockito.anyList(), Mockito.anyList(), Mockito.any(), Mockito.any())).thenReturn(true);
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.BIOMETRIC_EXTRACTION_FAILED)).thenReturn("FAILED");
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.VALIDATION_FAILED_EXCEPTION)).thenReturn("FAILED");
		Mockito.when(registrationStatusMapperUtil
				.getStatusCode(RegistrationExceptionTypeCode.CONNECTION_UNAVAILABLE_EXCEPTION)).thenReturn("FAILED");
		
	}

	
	@Test(expected = Exception.class)
	public void authenticateBiometricsTest() throws Exception   {
		when(utilities.retrieveIdrepoDocument(Mockito.anyString())).thenReturn(null);
		PowerMockito.mockStatic(CbeffValidator.class);
		when(CbeffValidator.getBIRFromXML(Mockito.any())).thenReturn(new BIR());
		bioSdkUtil.authenticateBiometrics("123456", "UIN", l,regist,"test","test");
		
	}
	
	
	@Test(expected = BaseCheckedException.class)
	public void authenticateBiometricsTest1() throws Exception   {
		
		PowerMockito.mockStatic(CbeffValidator.class);
		when(CbeffValidator.getBIRFromXML(Mockito.any())).thenReturn(new BIR());
		bioSdkUtil.authenticateBiometrics("123456", "UIN", l,regist,"test","test");
		
	}
}
