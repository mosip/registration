package io.mosip.registration.processor.packet.storage.utils;

import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.dto.ContainerInfoDto;
import io.mosip.registration.processor.packet.storage.dto.InfoResponseDto;
import io.mosip.registration.processor.packet.storage.dto.ProviderStageName;
import org.apache.commons.lang.StringUtils;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SourceProcessFilter {

    private static final String sourceInitial = "source:";
    private static final String processInitial = "process:";

    @Value("${packetmanager.name.source.default}")
    private String defaultSource;

    @Autowired
    private Utilities utilities;

    @Autowired
    private PacketManagerService packetManagerService;

    private static Map<String, String> providerConfiguration;

    public static void initialize(Map<String, String> provider) {
        providerConfiguration = provider;
    }

    public Map<String, String> getFieldsByPriority(String id, ProviderStageName stageName, List<String> fields)
            throws ApisResourceAccessException, IOException, PacketManagerException, JsonProcessingException {

        Map<String, String> fieldMap = new HashMap<>();
        InfoResponseDto infoResponseDto = packetManagerService.info(id);
        Map<String, String> keyMap = getKeyMap(stageName);
        // if there is only one source then save time
        if (infoResponseDto.getInfo().size() == 1) {
            ContainerInfoDto containerInfoDto = infoResponseDto.getInfo().iterator().next();
            fieldMap = packetManagerService.getFields(id, fields, containerInfoDto.getSource(), containerInfoDto.getProcess());
            return fieldMap;
        }
        // else find correct source for each field
        for (String field : fields) {
            ContainerInfoDto containerInfoDto = getContainerInfo(keyMap, field, infoResponseDto);
            if (containerInfoDto != null) {
                String fieldValue = packetManagerService.getField(id, field, containerInfoDto.getSource(), containerInfoDto.getProcess());
                fieldMap.put(field, fieldValue);
            }
        }
        ContainerInfoDto containerInfoDto = getBiometricSourceAndProcess(fields, keyMap, infoResponseDto.getInfo());
        if (containerInfoDto != null)
            fieldMap.put(getApplicantBiometricLabel(), packetManagerService.getField(id, getApplicantBiometricLabel(), containerInfoDto.getSource(), containerInfoDto.getProcess()));
        return fieldMap;
    }


    public ContainerInfoDto findSourceAndProcessByPriority(String id, ProviderStageName stageName, String field)
            throws ApisResourceAccessException, IOException, PacketManagerException, JsonProcessingException {

        InfoResponseDto infoResponseDto = packetManagerService.info(id);
        Map<String, String> keyMap = getKeyMap(stageName);

        // to search biometric fields
        ContainerInfoDto containerInfoDto = getBiometricSourceAndProcess(Lists.newArrayList(field), keyMap, infoResponseDto.getInfo());
        if (containerInfoDto != null)
            return containerInfoDto;

        return getContainerInfo(keyMap, field, infoResponseDto);
    }

    public BiometricRecord getBiometrics(String id, ProviderStageName stageName) throws IOException, ApisResourceAccessException, PacketManagerException, JsonProcessingException {
        String biometricLabel = getApplicantBiometricLabel();
        InfoResponseDto infoResponseDto = packetManagerService.info(id);
        Map<String, String> finalKeyMap = getKeyMap(stageName).entrySet().stream().filter(key -> key.getKey().contains(biometricLabel)).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        // if there is no type/subtype set in properties
        if (finalKeyMap.get(biometricLabel) != null) {
            ContainerInfoDto containerInfoDto = getContainerInfo(finalKeyMap, biometricLabel, infoResponseDto);
            return packetManagerService.getBiometrics(id, biometricLabel, null, containerInfoDto.getSource(), containerInfoDto.getProcess());
        }

        Set<ContainerInfoDto> containers = new HashSet<>();
        BiometricRecord biometricRecord = null;

        for (String key : finalKeyMap.keySet()) {
            ContainerInfoDto containerInfoDto = getBiometricContainerInfo(finalKeyMap, biometricLabel, key, infoResponseDto);
            if (containerInfoDto != null) {
                Optional<Boolean> optional = containers.stream().map(c -> c.equals(containerInfoDto)).findAny();
                if (!optional.isPresent() || optional.get().equals(Boolean.FALSE))
                    containers.add(containerInfoDto);
            }
        }

        for (ContainerInfoDto containerInfoDto : containers) {
            List<String> modalities = new ArrayList<>();
            containerInfoDto.getBiometrics().stream().forEach(b -> {
                if (b.getSubtypes() == null)
                    modalities.add(b.getType());
                else
                    modalities.addAll(b.getSubtypes());
            });
            System.out.println("getting these modalities : " + modalities.toString());
            System.out.println("from source : < " + containerInfoDto.getSource() + " > and process : < " + containerInfoDto.getProcess() + " >");
            BiometricRecord record = packetManagerService.getBiometrics(id, biometricLabel, modalities, containerInfoDto.getSource(), containerInfoDto.getProcess());

            if (biometricRecord == null)
                biometricRecord = new BiometricRecord();
            biometricRecord.getSegments().addAll(record.getSegments());
        }
        return biometricRecord;
    }

    private Map<String, String> getKeyMap(ProviderStageName stageName) {
        return providerConfiguration.entrySet().stream().collect(Collectors.toMap(
                e -> e.getKey().startsWith(stageName.getValue()) ? e.getKey().substring(stageName.getValue().length() + 1) : null, e-> e.getValue()));
    }

    private ContainerInfoDto getBiometricSourceAndProcess(List<String> fields, Map<String, String> keyMap, List<ContainerInfoDto> info) throws IOException, ApisResourceAccessException, PacketManagerException, JsonProcessingException {
        String applicantBiometricLabel = getApplicantBiometricLabel();
        if (fields.contains(getApplicantBiometricLabel()) && keyMap.keySet().stream()
                .map(key -> key.contains(applicantBiometricLabel)).findAny().isPresent()) {
            Optional<ContainerInfoDto> containerInfoDto = info.stream().filter(i -> i.getSource().equalsIgnoreCase(defaultSource)).findAny();
            if (containerInfoDto.isPresent())
                return containerInfoDto.get();

        }
        return null;
    }

    private String getApplicantBiometricLabel() throws IOException {
        return JsonUtil.getJSONValue(JsonUtil.getJSONObject(utilities.getRegistrationProcessorMappingJson(
                MappingJsonConstants.IDENTITY), MappingJsonConstants.INDIVIDUAL_BIOMETRICS), MappingJsonConstants.VALUE);
    }

    private ContainerInfoDto getContainerInfo(Map<String, String> keyMap, String field, InfoResponseDto infoResponseDto) {
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

    private ContainerInfoDto getBiometricContainerInfo(Map<String, String> keyMap, String individualBiometrics, String typeSubtype, InfoResponseDto infoResponseDto) {
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
                                info.getBiometrics().stream().filter(bio -> bio.getType().equalsIgnoreCase(finalTypeSubtype) || bio.getSubtypes().contains(finalTypeSubtype)).findAny().isPresent() : false).findAny();
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
}
