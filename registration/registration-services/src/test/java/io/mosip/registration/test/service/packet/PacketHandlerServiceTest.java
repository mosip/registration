package io.mosip.registration.test.service.packet;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import io.mosip.registration.audit.AuditManagerSerivceImpl;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SuccessResponseDTO;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.service.packet.PacketCreationService;
import io.mosip.registration.service.packet.PacketEncryptionService;
import io.mosip.registration.service.packet.impl.PacketHandlerServiceImpl;

public class PacketHandlerServiceTest {
	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	@InjectMocks
	private PacketHandlerServiceImpl packetHandlerServiceImpl;
	@Mock
	private PacketCreationService packetCreationService;
	@Mock
	private PacketEncryptionService packetEncryptionService;
	@Mock
	private AuditManagerSerivceImpl auditFactory;
	private ResponseDTO mockedSuccessResponse;

	@Before
	public void initialize() {
		mockedSuccessResponse = new ResponseDTO();
		mockedSuccessResponse.setSuccessResponseDTO(new SuccessResponseDTO());
	}

	/*
	 * @Test public void testHandle() throws RegBaseCheckedException {
	 * RegistrationDTO registrationDTO = new RegistrationDTO();
	 * registrationDTO.setRegistrationId("10010100100002420190805063005");
	 * 
	 * Mockito.when(packetCreationService.create(Mockito.any(RegistrationDTO.class))
	 * ).thenReturn("Packet Creation".getBytes()); Mockito.when(
	 * packetEncryptionService.encrypt(Mockito.any(RegistrationDTO.class),
	 * Mockito.anyString().getBytes())) .thenReturn(mockedSuccessResponse);
	 * 
	 * Assert.assertNotNull(packetHandlerServiceImpl.handle(registrationDTO).
	 * getSuccessResponseDTO()); }
	 */

	@Test
	public void testCreationException() throws RegBaseCheckedException {
		Mockito.when(packetCreationService.create(Mockito.any(RegistrationDTO.class))).thenReturn(null);

		ResponseDTO actualResponse = packetHandlerServiceImpl.handle(new RegistrationDTO());

		Assert.assertEquals(RegistrationExceptionConstants.REG_PACKET_CREATION_ERROR_CODE.getErrorCode(),
				actualResponse.getErrorResponseDTOs().get(0).getCode());
	}

	/*
	 * @Test public void testHandlerException() throws RegBaseCheckedException {
	 * RegBaseUncheckedException exception = new
	 * RegBaseUncheckedException("errorCode", "errorMsg"); RegistrationDTO
	 * registrationDTO = new RegistrationDTO();
	 * registrationDTO.setRegistrationId("");
	 * 
	 * Mockito.when(packetCreationService.create(Mockito.any(RegistrationDTO.class))
	 * ) .thenThrow(exception); Mockito.when(
	 * packetEncryptionService.encrypt(Mockito.any(RegistrationDTO.class),
	 * Mockito.anyString().getBytes())) .thenReturn(mockedSuccessResponse);
	 * 
	 * Assert.assertNotNull(packetHandlerServiceImpl.handle(registrationDTO).
	 * getErrorResponseDTOs()); }
	 */

	@Test
	public void testHandlerChkException() throws RegBaseCheckedException {
		RegBaseCheckedException exception = new RegBaseCheckedException("errorCode", "errorMsg");

		Mockito.when(packetCreationService.create(Mockito.any(RegistrationDTO.class))).thenThrow(exception);
		Mockito.when(
				packetEncryptionService.encrypt(Mockito.any(RegistrationDTO.class), Mockito.anyString().getBytes()))
				.thenReturn(mockedSuccessResponse);

		Assert.assertNotNull(packetHandlerServiceImpl.handle(null).getErrorResponseDTOs());
	}

	@Test
	public void testHandlerAuthenticationException() throws RegBaseCheckedException {
		RegBaseCheckedException exception = new RegBaseCheckedException(
				RegistrationExceptionConstants.AUTH_ADVICE_USR_ERROR.getErrorCode(),
				RegistrationExceptionConstants.AUTH_ADVICE_USR_ERROR.getErrorMessage());

		Mockito.when(packetCreationService.create(Mockito.any(RegistrationDTO.class))).thenThrow(exception);
		Mockito.when(
				packetEncryptionService.encrypt(Mockito.any(RegistrationDTO.class), Mockito.anyString().getBytes()))
				.thenReturn(mockedSuccessResponse);

		Assert.assertNotNull(packetHandlerServiceImpl.handle(null).getErrorResponseDTOs());
	}

	private RegistrationDTO getRegistrationDTO() {
		RegistrationDTO registrationDTO = new RegistrationDTO();

		BiometricsDto biometricsDto = new BiometricsDto("leftIndex", new byte[126], 20);

		List<BiometricsDto> biometricsList = Collections.singletonList(biometricsDto);

		
		Map<String, BiometricsDto> biometricsMap = new LinkedHashMap<>();
		biometricsMap.put("applicant", biometricsDto);
		registrationDTO.setBiometrics(biometricsMap);
		
		registrationDTO.setOfficerBiometrics(biometricsList);
		
		
		return registrationDTO;
	}

}
