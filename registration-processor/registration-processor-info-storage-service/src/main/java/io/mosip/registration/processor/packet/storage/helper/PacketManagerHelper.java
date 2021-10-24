package io.mosip.registration.processor.packet.storage.helper;

import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.dto.ContainerInfoDto;
import io.mosip.registration.processor.packet.storage.dto.InfoResponseDto;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class PacketManagerHelper {

    private static final String sourceInitial = "source:";
    private static final String processInitial = "process:";

    @Value("${packetmanager.name.source.default}")
    private String defaultSource;

    @Autowired
    private Utilities utilities;

    public boolean isFieldPresent(String field, ProviderStageName stageName, Map<String, String> providerConfiguration) throws IOException {
        Map<String, String> keyMap = getKeyMap(stageName, providerConfiguration);
        return !CollectionUtils.isEmpty(keyMap) && (keyMap.keySet().contains(field) || isApplicantBiometricPresent(field, keyMap));
    }


    public static Map<String, String> getKeyMap(ProviderStageName stageName, Map<String, String> providerConfiguration) {
        Map<String, String> keyMap = new HashMap<>();
        for (Map.Entry<String, String> e : providerConfiguration.entrySet()) {
            if (e.getKey().startsWith(stageName.getValue()))
                keyMap.put(e.getKey().substring(stageName.getValue().length() + 1), e.getValue());
        }
        return keyMap;
    }

    public static List<String> getTypeSubtypeModalities(ContainerInfoDto containerInfoDto) {
        List<String> modalities = new ArrayList<>();
        containerInfoDto.getBiometrics().stream().forEach(b -> {
            if (b.getSubtypes() == null)
                modalities.add(b.getType());
            else
                modalities.addAll(b.getSubtypes());
        });
        return modalities;
    }

    public static ContainerInfoDto getBiometricContainerInfo(Map<String, String> keyMap, String individualBiometrics, String typeSubtype, InfoResponseDto infoResponseDto) {
        String finalTypeSubtype = typeSubtype.substring(individualBiometrics.length() + 1);
        String sourceProcessString = keyMap.get(typeSubtype);
        if (StringUtils.isNotEmpty(sourceProcessString)) {
            String[] val = sourceProcessString.split(",");
            if (val != null && val.length > 0) {
                for (String value : val) {
                    String[] str = value.split("/");
                    if (str != null && str.length > 0 && str[0].startsWith(sourceInitial)) {
                        String sourceStr = str[0].substring(sourceInitial.length());
                        // String processStr = str[1].substring(processInitial.length());

                        Optional<ContainerInfoDto> isSourcePresent = infoResponseDto.getInfo().stream().filter(info -> info.getSource().equalsIgnoreCase(sourceStr) ?
                                info.getBiometrics() != null && info.getBiometrics().stream().filter(bio -> (
                                        StringUtils.isNotEmpty(bio.getType()) && bio.getType().equalsIgnoreCase(finalTypeSubtype)) ||
                                        (!CollectionUtils.isEmpty(bio.getSubtypes()) && bio.getSubtypes().contains(finalTypeSubtype))).findAny().isPresent() : false).findAny();
                        // if source is present then get from that source or else continue searching
                        if (isSourcePresent.isPresent()) {
                            return isSourcePresent.get();
                        } else
                            continue;
                    }
                }
            }
        }
        return null;
    }

    public static ContainerInfoDto getContainerInfo(Map<String, String> keyMap, String field, InfoResponseDto infoResponseDto) {
        String sourceProcessString = keyMap.get(field);
        if (StringUtils.isNotEmpty(sourceProcessString)) {
            String[] val = sourceProcessString.split(",");
            if (val != null && val.length > 0) {
                for (String value : val) {
                    String[] str = value.split("/");
                    if (str != null && str.length > 0 && str[0].startsWith(sourceInitial)) {
                        String sourceStr = str[0].substring(sourceInitial.length());
                        // String processStr = str[1].substring(processInitial.length());
                        Optional<ContainerInfoDto> containerDto = infoResponseDto.getInfo().stream().filter(info ->
                                info.getDemographics().contains(field) && info.getSource().equalsIgnoreCase(sourceStr)).findAny();
                        // if source is present then get from that source or else continue searching
                        if (containerDto.isPresent()) {
                            return containerDto.get();
                        } else
                            continue;
                    }
                }
            }
        }
        return null;
    }

    public ContainerInfoDto getBiometricSourceAndProcess(List<String> fields, Map<String, String> keyMap, List<ContainerInfoDto> info) throws IOException {
        String applicantBiometricLabel = getApplicantBiometricLabel();
        if (fields.contains(getApplicantBiometricLabel()) && keyMap.keySet().stream()
                .map(key -> key.contains(applicantBiometricLabel)).findAny().isPresent()) {
            Optional<ContainerInfoDto> containerInfoDto = info.stream().filter(i -> i.getSource().equalsIgnoreCase(defaultSource)).findAny();
            if (containerInfoDto.isPresent())
                return containerInfoDto.get();
        }
        return null;
    }

    public String getApplicantBiometricLabel() throws IOException {
        return JsonUtil.getJSONValue(JsonUtil.getJSONObject(utilities.getRegistrationProcessorMappingJson(
                MappingJsonConstants.IDENTITY), MappingJsonConstants.INDIVIDUAL_BIOMETRICS), MappingJsonConstants.VALUE);
    }

    private boolean isApplicantBiometricPresent(String field, Map<String, String> keyMap) throws IOException  {
        String applicantBiometricLabel = getApplicantBiometricLabel();
        return (field != null) && field.equalsIgnoreCase(applicantBiometricLabel)
                && keyMap.keySet().stream()
                .map(key -> key.contains(applicantBiometricLabel)).findAny().isPresent();
    }
}
