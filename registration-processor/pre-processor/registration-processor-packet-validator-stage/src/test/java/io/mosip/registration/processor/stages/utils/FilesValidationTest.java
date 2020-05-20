package io.mosip.registration.processor.stages.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import io.mosip.kernel.packetmanager.exception.ApiNotAccessibleException;
import io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException;
import io.mosip.kernel.packetmanager.spi.PacketReaderService;
import io.mosip.registration.processor.core.constant.PacketFiles;
import io.mosip.registration.processor.core.packet.dto.FieldValueArray;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.PacketMetaInfo;
import io.mosip.registration.processor.core.packet.dto.packetvalidator.PacketValidationDto;

@RunWith(PowerMockRunner.class)
public class FilesValidationTest {

	@InjectMocks
	private FilesValidation filesValidation;
	@Mock
	private PacketReaderService packetReaderService;
	private PacketMetaInfo packetMetaInfo=new PacketMetaInfo();
	private PacketValidationDto packetValidationDto=new PacketValidationDto();
	@Before
	public void setup() throws PacketDecryptionFailureException, ApiNotAccessibleException, IOException {
		Identity identity = new Identity();

		

		List<FieldValueArray> fieldValueArrayList = new ArrayList<FieldValueArray>();

		FieldValueArray applicantBiometric = new FieldValueArray();
		applicantBiometric.setLabel(PacketFiles.APPLICANTBIOMETRICSEQUENCE.name());
		List<String> applicantBiometricValues = new ArrayList<String>();
		applicantBiometricValues.add(PacketFiles.BOTHTHUMBS.name());
		applicantBiometric.setValue(applicantBiometricValues);
		fieldValueArrayList.add(applicantBiometric);
		FieldValueArray introducerBiometric = new FieldValueArray();
		introducerBiometric.setLabel(PacketFiles.INTRODUCERBIOMETRICSEQUENCE.name());
		List<String> introducerBiometricValues = new ArrayList<String>();
		introducerBiometricValues.add("introducerLeftThumb");
		introducerBiometric.setValue(introducerBiometricValues);
		fieldValueArrayList.add(introducerBiometric);
		FieldValueArray applicantDemographic = new FieldValueArray();
		applicantDemographic.setLabel(PacketFiles.APPLICANTDEMOGRAPHICSEQUENCE.name());
		List<String> applicantDemographicValues = new ArrayList<String>();
		applicantDemographicValues.add(PacketFiles.APPLICANTPHOTO.name());
		applicantDemographicValues.add("ProofOfBirth");
		applicantDemographicValues.add("ProofOfAddress");
		applicantDemographic.setValue(applicantDemographicValues);
		fieldValueArrayList.add(applicantDemographic);
		identity.setHashSequence(fieldValueArrayList);
		List<String> sequence2 = new ArrayList<>();
		sequence2.add("audit");
		List<FieldValueArray> fieldValueArrayListSequence = new ArrayList<FieldValueArray>();
		FieldValueArray hashsequence2 = new FieldValueArray();
		hashsequence2.setLabel(PacketFiles.OTHERFILES.name());
		hashsequence2.setValue(sequence2);
		fieldValueArrayListSequence.add(hashsequence2);
		identity.setHashSequence2(fieldValueArrayListSequence);
		packetMetaInfo.setIdentity(identity);
		when(packetReaderService.checkFileExistence(anyString(), anyString(), anyString())).thenReturn(true);
		
	}
	@Test
	public void FilesValidationtest() throws PacketDecryptionFailureException, ApiNotAccessibleException, IOException{
		assertTrue(filesValidation.filesValidation("123456789", packetMetaInfo, "1234", packetValidationDto));
	}
	
	@Test
	public void MetainfoNulltest() throws PacketDecryptionFailureException, ApiNotAccessibleException, IOException{
		assertFalse(filesValidation.filesValidation("123456789", null, "1234", packetValidationDto));
	}
	
	@Test
	public void FilesValidationFailuretest() throws PacketDecryptionFailureException, ApiNotAccessibleException, IOException{
		when(packetReaderService.checkFileExistence(anyString(), anyString(), anyString())).thenReturn(false);
		assertFalse(filesValidation.filesValidation("123456789", packetMetaInfo, "1234", packetValidationDto));
	}
}
