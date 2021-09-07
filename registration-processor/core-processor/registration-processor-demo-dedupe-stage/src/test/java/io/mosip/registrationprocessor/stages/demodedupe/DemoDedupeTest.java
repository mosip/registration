package io.mosip.registrationprocessor.stages.demodedupe;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import io.mosip.registration.processor.core.packet.dto.abis.RegBioRefDto;
import org.apache.commons.io.IOUtils;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.env.Environment;

import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.registration.processor.core.auth.dto.AuthResponseDTO;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.demographicinfo.DemographicInfoDto;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.packet.storage.dao.PacketInfoDao;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.stages.demodedupe.DemoDedupe;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.service.RegistrationStatusService;

/**
 * The Class DemoDedupeTest.
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*","javax.management.*", "javax.net.ssl.*" })
@PrepareForTest({ IOUtils.class, HMACUtils2.class })
public class DemoDedupeTest {

	/** The packet info manager. */
	@Mock
	private PacketInfoManager<Identity, ApplicantInfoDto> packetInfoManager;

	/** The packet info dao. */
	@Mock
	private PacketInfoDao packetInfoDao;

	/** The input stream. */
	@Mock
	private InputStream inputStream;



	/** The registration status service. */
	@Mock
	private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;

	/** The auth response DTO. */
	@Mock
	AuthResponseDTO authResponseDTO = new AuthResponseDTO();

	/** The rest client service. */
	@Mock
	RegistrationProcessorRestClientService<Object> restClientService;

	/** The env. */
	@Mock
	Environment env;

	/** The demo dedupe. */
	@InjectMocks
	private DemoDedupe demoDedupe;

	/**
	 * Sets the up.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Before
	public void setUp() throws Exception {

		List<String> fingers = new ArrayList<>();
		fingers.add("LEFTTHUMB");
		fingers.add("LEFTINDEX");
		fingers.add("LEFTMIDDLE");
		fingers.add("LEFTLITTLE");
		fingers.add("LEFTRING");
		fingers.add("RIGHTTHUMB");
		fingers.add("RIGHTINDEX");
		fingers.add("RIGHTMIDDLE");
		fingers.add("RIGHTLITTLE");
		fingers.add("RIGHTRING");

		List<String> iris = new ArrayList<>();
		iris.add("LEFTEYE");
		iris.add("RIGHTEYE");
		Mockito.when(env.getProperty("fingerType")).thenReturn("LeftThumb");
		// Mockito.when(packetInfoManager.getApplicantFingerPrintImageNameById(anyString())).thenReturn(fingers);
		// Mockito.when(packetInfoManager.getApplicantIrisImageNameById(anyString())).thenReturn(iris);


		Mockito.when(registrationStatusService.checkUinAvailabilityForRid(any())).thenReturn(true);

		byte[] data = "1234567890".getBytes();
		PowerMockito.mockStatic(IOUtils.class);
		PowerMockito.when(IOUtils.class, "toByteArray", inputStream).thenReturn(data);

		// authResponseDTO.setStatus("y");
		Mockito.when(restClientService.postApi(any(), any(), any(), any(), any()))
				.thenReturn(authResponseDTO);
	}

	/**
	 * Test dedupe duplicate found.
	 */
	@Test
	public void testDedupeDuplicateFound() {
		String regId = "1234567890";

		DemographicInfoDto dto1 = new DemographicInfoDto();
		dto1.setRegId("1234");
		DemographicInfoDto dto2 = new DemographicInfoDto();
		dto2.setRegId("5678");
		List<DemographicInfoDto> Dtos = new ArrayList<>();
		Dtos.add(dto1);
		Dtos.add(dto2);

		RegBioRefDto regBioRefDto1 = new RegBioRefDto();
		regBioRefDto1.setRegId("1234");
		RegBioRefDto regBioRefDto2 = new RegBioRefDto();
		regBioRefDto2.setRegId("5678");
		List<RegBioRefDto> regBioRefDtos = new ArrayList<>();
		regBioRefDtos.add(regBioRefDto1);
		regBioRefDtos.add(regBioRefDto2);

		Mockito.when(packetInfoDao.findDemoById(regId)).thenReturn(Dtos);

		Mockito.when(packetInfoDao.getAllDemographicInfoDtos(any(), any(), any(), any())).thenReturn(Dtos);
		Mockito.when(packetInfoManager.getBioRefIdsByRegIds(anyList())).thenReturn(regBioRefDtos);

		List<DemographicInfoDto> duplicates = demoDedupe.performDedupe(regId);
		assertEquals("Test for Dedupe Duplicate found", false, duplicates.isEmpty());
	}

	/**
	 * Test demodedupe empty.
	 */
	@Test
	public void testDemodedupeEmpty() {

		String regId = "1234567890";
		List<DemographicInfoDto> Dtos = new ArrayList<>();

		Mockito.when(packetInfoDao.findDemoById(regId)).thenReturn(Dtos);

		List<DemographicInfoDto> duplicates = demoDedupe.performDedupe(regId);
		assertEquals("Test for Demo Dedupe Empty", true, duplicates.isEmpty());
	}

	@Test
	public void testFilterByRefIds() {
		String regId = "1234567890";

		// 2 ids have uin
		DemographicInfoDto dto1 = new DemographicInfoDto();
		dto1.setRegId("1234");
		DemographicInfoDto dto2 = new DemographicInfoDto();
		dto2.setRegId("5678");
		List<DemographicInfoDto> Dtos = Lists.newArrayList(dto1, dto2);

		// only one id has ref id
		RegBioRefDto regBioRefDto1 = new RegBioRefDto();
		regBioRefDto1.setRegId("1234");
		List<RegBioRefDto> regBioRefDtos = Lists.newArrayList(regBioRefDto1);

		Mockito.when(packetInfoDao.findDemoById(regId)).thenReturn(Dtos);

		Mockito.when(packetInfoDao.getAllDemographicInfoDtos(any(), any(), any(), any())).thenReturn(Dtos);
		Mockito.when(packetInfoManager.getBioRefIdsByRegIds(anyList())).thenReturn(regBioRefDtos);

		List<DemographicInfoDto> duplicates = demoDedupe.performDedupe(regId);
		// expected to return only one id
		assertEquals("Test for Dedupe Duplicate found", 1, duplicates.size());
	}

}
