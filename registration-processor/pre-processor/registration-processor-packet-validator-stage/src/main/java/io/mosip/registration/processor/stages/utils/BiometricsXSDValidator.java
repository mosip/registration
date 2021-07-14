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
			BIR bir = cbeffContainer.createBIRType(biometricRecord.getSegments());
            CbeffValidator.createXMLBytes(bir, IOUtils.toByteArray(xsd));//validates XSD 
        }
    } 

	
}
