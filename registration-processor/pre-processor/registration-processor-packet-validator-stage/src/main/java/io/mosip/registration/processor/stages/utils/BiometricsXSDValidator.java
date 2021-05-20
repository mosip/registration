package io.mosip.registration.processor.stages.utils;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.kernel.biometrics.commons.CbeffValidator;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.ProcessedLevelType;
import io.mosip.kernel.biometrics.constant.PurposeType;
import io.mosip.kernel.biometrics.constant.QualityType;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BIRInfo;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.biometrics.entities.RegistryIDType;
import io.mosip.kernel.biometrics.entities.VersionType;
import io.mosip.kernel.cbeffutil.container.impl.CbeffContainerImpl;
@Component
public class BiometricsXSDValidator {

    @Value("${mosip.kernel.xsdstorage-uri}")
    private String configServerFileStorageURL;
    
    @Value("${mosip.kernel.xsdfile}")
    private String schemaFileName;
    
    public void validateXSD(BiometricRecord biometricRecord ) throws Exception  {
    	try (InputStream xsd = new URL(configServerFileStorageURL + schemaFileName).openStream()) {
            CbeffContainerImpl cbeffContainer = new CbeffContainerImpl();
            List<BIR> birList = new ArrayList<>();
            biometricRecord.getSegments().forEach(s -> birList.add(convertToBIR(s)));
			BIR bir = cbeffContainer.createBIRType(birList);
            CbeffValidator.createXMLBytes(bir, IOUtils.toByteArray(xsd));//validates XSD 
        }
    } 

	public  BIR convertToBIR(io.mosip.kernel.biometrics.entities.BIR bir) {
		List<BiometricType> bioTypes = new ArrayList<>();
        for(BiometricType type : bir.getBdbInfo().getType()) {
			bioTypes.add(BiometricType.fromValue(type.value()));
        }

        RegistryIDType format = null;
        if (bir.getBdbInfo() != null && bir.getBdbInfo().getFormat() != null) {
            format = new RegistryIDType();
            format.setOrganization(bir.getBdbInfo().getFormat().getOrganization());
            format.setType(bir.getBdbInfo().getFormat().getType());
        }

        RegistryIDType birAlgorithm = null;
        if (bir.getBdbInfo() != null
                && bir.getBdbInfo().getQuality() != null && bir.getBdbInfo().getQuality().getAlgorithm() != null) {
            birAlgorithm = new RegistryIDType();
            birAlgorithm.setOrganization(bir.getBdbInfo().getQuality().getAlgorithm().getOrganization());
            birAlgorithm.setType(bir.getBdbInfo().getQuality().getAlgorithm().getType());
        }


        QualityType qualityType = null;
        if (bir.getBdbInfo() != null && bir.getBdbInfo().getQuality() != null) {
            qualityType = new QualityType();
            qualityType.setAlgorithm(birAlgorithm);
            qualityType.setQualityCalculationFailed(bir.getBdbInfo().getQuality().getQualityCalculationFailed());
            qualityType.setScore(bir.getBdbInfo().getQuality().getScore());
        }

        return new BIR.BIRBuilder()
                .withBdb(bir.getBdb())
				.withVersion(bir.getVersion() == null ? null
						: new VersionType.VersionTypeBuilder()
                        .withMinor(bir.getVersion().getMinor())
                        .withMajor(bir.getVersion().getMajor()).build())
				.withCbeffversion(bir.getCbeffversion() == null ? null
						: new VersionType.VersionTypeBuilder()
                        .withMinor(bir.getCbeffversion().getMinor())
                        .withMajor(bir.getCbeffversion().getMajor()).build())
                .withBirInfo(new BIRInfo.BIRInfoBuilder().withIntegrity(true).build())
                .withBdbInfo(bir.getBdbInfo() == null ? null : new BDBInfo.BDBInfoBuilder()
                        .withFormat(format)
                        .withType(bioTypes)
                        .withQuality(qualityType)
                        .withCreationDate(bir.getBdbInfo().getCreationDate())
                        .withIndex(bir.getBdbInfo().getIndex())
                        .withPurpose(bir.getBdbInfo().getPurpose() == null ? null :
                                PurposeType.fromValue(io.mosip.kernel.biometrics.constant.PurposeType.fromValue(bir.getBdbInfo().getPurpose().name()).value()))
                        .withLevel(bir.getBdbInfo().getLevel() == null ? null :
                                ProcessedLevelType.fromValue(io.mosip.kernel.biometrics.constant.ProcessedLevelType.fromValue(bir.getBdbInfo().getLevel().name()).value()))
                        .withSubtype(bir.getBdbInfo().getSubtype()).build()).build();
    }
}
