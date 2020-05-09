package io.mosip.registration.packetmanager.impl;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.UUID;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.springframework.stereotype.Component;

import io.mosip.kernel.core.cbeffutil.entity.BDBInfo;
import io.mosip.kernel.core.cbeffutil.entity.BIR;
import io.mosip.kernel.core.cbeffutil.entity.BIRInfo;
import io.mosip.kernel.core.cbeffutil.entity.BIRVersion;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.ProcessedLevelType;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.PurposeType;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.QualityType;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.RegistryIDType;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.registration.packetmanager.spi.BiometricDataBuilder;
import io.mosip.registration.packetmananger.constants.PacketManagerConstants;

@Component
public class CbeffBIRBuilder implements BiometricDataBuilder {	
	
	@Override
	public BIR buildBIR(byte[] bdb, long formatType, double qualityScore, SingleType type, String subType) {

		// Format
		RegistryIDType birFormat = new RegistryIDType();
		birFormat.setOrganization(PacketManagerConstants.CBEFF_DEFAULT_FORMAT_ORG);
		birFormat.setType(String.valueOf(formatType));

		// Algorithm
		RegistryIDType birAlgorithm = new RegistryIDType();
		birAlgorithm.setOrganization(PacketManagerConstants.CBEFF_DEFAULT_ALG_ORG);
		birAlgorithm.setType(PacketManagerConstants.CBEFF_DEFAULT_ALG_TYPE);

		// Quality Type
		QualityType qualityType = new QualityType();
		qualityType.setAlgorithm(birAlgorithm);
		qualityType.setScore((long) qualityScore);

		return new BIR.BIRBuilder()
				.withBdb(bdb)
				.withElement(Arrays.asList(getCBEFFTestTag(type)))
				.withVersion(new BIRVersion.BIRVersionBuilder().withMajor(1).withMinor(1).build())
				.withCbeffversion(new BIRVersion.BIRVersionBuilder().withMajor(1).withMinor(1).build())
				.withBirInfo(new BIRInfo.BIRInfoBuilder().withIntegrity(false).build())
				.withBdbInfo(new BDBInfo.BDBInfoBuilder()
						.withFormat(birFormat)
						.withQuality(qualityType)
						.withType(Arrays.asList(type))
						.withSubtype(Arrays.asList(subType))
						.withPurpose(PurposeType.ENROLL)
						.withLevel(ProcessedLevelType.RAW)
						.withCreationDate(LocalDateTime.now(ZoneOffset.UTC))
						.withIndex(UUID.randomUUID().toString())
						.build())
				.build();
	}
	
	private JAXBElement<String> getCBEFFTestTag(SingleType biometricType) {
		String testTagElementName = null;
		
		switch (biometricType) {
		case FINGER:
			testTagElementName = "TestFinger";
			break;
		case IRIS:
			testTagElementName = "TestIris";
			break;
		case FACE:
			testTagElementName = "TestFace";
			break;
		default:
			break;
		}

		return new JAXBElement<>(new QName("testschema", testTagElementName), String.class, 
				PacketManagerConstants.CBEFF_DEFAULT_FORMAT_TYPE);
	}

}
