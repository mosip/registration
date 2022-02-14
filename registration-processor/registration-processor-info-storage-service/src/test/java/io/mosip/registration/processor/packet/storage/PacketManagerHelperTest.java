package io.mosip.registration.processor.packet.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.packet.storage.dto.BiometricsDto;
import io.mosip.registration.processor.packet.storage.dto.ContainerInfoDto;
import io.mosip.registration.processor.packet.storage.dto.InfoResponseDto;
import io.mosip.registration.processor.packet.storage.helper.PacketManagerHelper;
import io.mosip.registration.processor.packet.storage.utils.Utilities;

@RunWith(MockitoJUnitRunner.class)
public class PacketManagerHelperTest {

	@InjectMocks
	PacketManagerHelper packetManagerHelper;
	@Mock
	private Utilities utilities;
	
	@Before
	public void setup() throws IOException {
		ReflectionTestUtils.setField(packetManagerHelper, "defaultSource", "REGISTRATION_CLIENT");
		LinkedHashMap<String,Object> obj=new LinkedHashMap<>();
		obj.put("value", "individualBiometrics");
		LinkedHashMap<String,Object> obj1=new LinkedHashMap<>();
		obj1.put("individualBiometrics", obj);
		
		Mockito.when(utilities.getRegistrationProcessorMappingJson(
                Mockito.anyString())).thenReturn(new JSONObject(obj1));
	}
	@Test
	public void isBIOMETRICFieldPresentTest() throws IOException {
		Map<String,String> providerConfiguration=new HashMap<>();
		providerConfiguration.put("uingenerator.individualBiometrics[Finger]", "source:REGISTRATION_CLIENT"+"/process:NEW|UPDATE|LOST");
		assertTrue(packetManagerHelper.isFieldPresent("individualBiometrics",ProviderStageName.UIN_GENERATOR, providerConfiguration));
	}
	
	@Test
	public void isFieldPresentTest() throws IOException {
		Map<String,String> providerConfiguration=new HashMap<>();
		providerConfiguration.put("uingenerator.postalCode", "source:REGISTRATION_CLIENT"+"/process:NEW|UPDATE|LOST");
		assertTrue(packetManagerHelper.isFieldPresent("postalCode",ProviderStageName.UIN_GENERATOR, providerConfiguration));
	}
	@Test
	public void getTypeSubtypeModalitiesTest() {
		ContainerInfoDto containerInfoDto=new ContainerInfoDto();
		BiometricsDto biometricsDto=new BiometricsDto();
		biometricsDto.setType("IRIS");
		biometricsDto.setSubtypes(List.of("Left"));
		containerInfoDto.setBiometrics(List.of(biometricsDto));
		assertTrue(PacketManagerHelper.getTypeSubtypeModalities(containerInfoDto).contains("Left"));
	}
	
	@Test
	public void getBiometricContainerInfoTest() {
		InfoResponseDto infoResponseDto=new InfoResponseDto();
		ContainerInfoDto containerInfoDto=new ContainerInfoDto();
		BiometricsDto biometricsDto=new BiometricsDto();
		biometricsDto.setType("Face");
		containerInfoDto.setBiometrics(List.of(biometricsDto));
		containerInfoDto.setProcess("NEW");
		containerInfoDto.setSource("REGISTRATION_CLIENT");
		infoResponseDto.setInfo(List.of(containerInfoDto));
		Map<String,String> keymap=new HashMap<>();
		keymap.put("individualBiometrics.Face", "source:REGISTRATION_CLIENT"+"/process:NEW|UPDATE|LOST");
		
		assertTrue(PacketManagerHelper.getBiometricContainerInfo(keymap,"individualBiometrics","individualBiometrics.Face",infoResponseDto).getSource().equals("REGISTRATION_CLIENT"));
	}
	
	@Test
	public void getContainerInfoTest() {
		InfoResponseDto infoResponseDto=new InfoResponseDto();
		ContainerInfoDto containerInfoDto=new ContainerInfoDto();
		BiometricsDto biometricsDto=new BiometricsDto();
		biometricsDto.setType("Face");
		containerInfoDto.setBiometrics(List.of(biometricsDto));
		containerInfoDto.setDemographics(Set.of("province"));
		containerInfoDto.setProcess("NEW");
		containerInfoDto.setSource("REGISTRATION_CLIENT");
		infoResponseDto.setInfo(List.of(containerInfoDto));
		Map<String,String> keymap=new HashMap<>();
		keymap.put("province", "source:REGISTRATION_CLIENT"+"/process:NEW|UPDATE|LOST");
		
		assertTrue(PacketManagerHelper.getContainerInfo(keymap,"province",infoResponseDto).getSource().equals("REGISTRATION_CLIENT"));
	}
	
	@Test
	public void getBiometricSourceAndProcessTest() throws IOException {
		ContainerInfoDto ContainerInfoDto=new ContainerInfoDto();
		BiometricsDto biometricsDto=new BiometricsDto();
		biometricsDto.setType("IRIS");
		biometricsDto.setSubtypes(List.of("Left"));
		ContainerInfoDto.setBiometrics(List.of(biometricsDto));
		ContainerInfoDto.setProcess("NEW");
		ContainerInfoDto.setSource("REGISTRATION_CLIENT");
		Map<String,String> keymap=new HashMap<>();
		keymap.put("individualBiometrics[Iris]", "source:REGISTRATION_CLIENT"+"/process:NEW|UPDATE|LOST");
		
		assertTrue(packetManagerHelper.getBiometricSourceAndProcess(List.of("individualBiometrics"),keymap,List.of(ContainerInfoDto)).getSource().equals("REGISTRATION_CLIENT"));
	}
}
