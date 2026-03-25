package io.mosip.registration.processor.packet.storage.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.util.Lists;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.dto.ContainerInfoDto;
import io.mosip.registration.processor.packet.storage.dto.Document;
import io.mosip.registration.processor.packet.storage.dto.FieldResponseDto;
import io.mosip.registration.processor.packet.storage.dto.InfoResponseDto;
import io.mosip.registration.processor.packet.storage.dto.ValidatePacketResponse;
import io.mosip.registration.processor.packet.storage.helper.PacketManagerHelper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;

@Component
public class PriorityBasedPacketManagerService {

    private static Logger regProcLogger = RegProcessorLogger.getLogger(PriorityBasedPacketManagerService.class);

    @Autowired
    private Utilities utilities;

    @Autowired
    private PacketManagerHelper packetManagerHelper;

    @Autowired
    private PacketManagerService packetManagerService;

    private static Map<String, String> providerConfiguration;

    public static void initialize(Map<String, String> provider) {
        providerConfiguration = provider;
    }

    /**
     * Get fields by mapping json Constant key.
     *
     * @param id
     * @param key
     * @param process
     * @param stageName
     * @return
     * @throws ApisResourceAccessException
     * @throws PacketManagerException
     * @throws JsonProcessingException
     * @throws IOException
     */
    public String getFieldByMappingJsonKey(String id, String key, String process, ProviderStageName stageName)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        try {
            JSONObject regProcessorIdentityJson = utilities.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);
            String field = JsonUtil.getJSONValue(
                    JsonUtil.getJSONObject(regProcessorIdentityJson, key),
                    MappingJsonConstants.VALUE);

            if (field == null) {
                throw new PacketManagerException("Field mapping not found for key: " + key, "FIELD_MAPPING_NOT_FOUND");
            }

            return getField(id, field, process, stageName);
        } catch (Exception e) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    id, "Error in getFieldByMappingJsonKey: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Get all fields by mapping json keys
     */
    public Map<String, String> getAllFieldsByMappingJsonKeys(String id, String process, ProviderStageName stageName)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        try {
            JSONObject regProcessorIdentityJson = utilities.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);
            List<String> fields = new ArrayList<>();

            for (Object key : regProcessorIdentityJson.keySet()) {
                String field = JsonUtil.getJSONValue(
                        JsonUtil.getJSONObject(regProcessorIdentityJson, key),
                        MappingJsonConstants.VALUE);
                if (field != null) {
                    fields.addAll(List.of(field.split(",")));
                }
            }

            Map<String, String> idValuesMap = getFields(id, fields, process, stageName);
            return idValuesMap;
        } catch (Exception e) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    id, "Error in getAllFieldsByMappingJsonKeys: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Get field by priority set in configuration
     *
     * @param id
     * @param field
     * @param process
     * @param stageName
     * @return
     * @throws ApisResourceAccessException
     * @throws PacketManagerException
     * @throws JsonProcessingException
     * @throws IOException
     */
    public String getField(String id, String field, String process, ProviderStageName stageName)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        Map<String, String> fieldMap = getFields(id, Lists.newArrayList(field), process, stageName);
        return fieldMap != null && fieldMap.size() == 1 ? fieldMap.values().iterator().next() : null;
    }

    /**
     * Get fields by priority set in configuration
     *
     * @param id
     * @param fields
     * @param process
     * @param stageName
     * @return
     * @throws ApisResourceAccessException
     * @throws PacketManagerException
     * @throws JsonProcessingException
     * @throws IOException
     */
    public Map<String, String> getFields(String id, List<String> fields, String process, ProviderStageName stageName)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        try {
            List<String> priorityList = new ArrayList<>();
            List<String> nonPriorityList = new ArrayList<>();
            Map<String, String> fieldMap = new HashMap<>();

            // find how many fields has priority set in configuration
            if (!CollectionUtils.isEmpty(PacketManagerHelper.getKeyMap(stageName, providerConfiguration))) {
                for (String field : fields) {
                    if (packetManagerHelper.isFieldPresent(field, stageName, providerConfiguration)) {
                        priorityList.add(field);
                    } else {
                        nonPriorityList.add(field);
                    }
                }
            } else {
                nonPriorityList.addAll(fields);
            }

            // get fields for which priority is set in config
            if (!CollectionUtils.isEmpty(priorityList)) {
                fieldMap.putAll(getFieldsByPriority(id, stageName, priorityList));
            }

            // get fields for which priority is not set in config
            if (!CollectionUtils.isEmpty(nonPriorityList)) {
                fieldMap.putAll(packetManagerService.getFields(id, nonPriorityList, null, process));
            }

            return fieldMap;
        } catch (Exception e) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    id, "Error in getFields: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Get meta info by priority
     *
     * @param id
     * @param process
     * @param stageName
     * @return
     * @throws ApisResourceAccessException
     * @throws PacketManagerException
     * @throws JsonProcessingException
     * @throws IOException
     */
    public Map<String, String> getMetaInfo(String id, String process, ProviderStageName stageName)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        try {
            ContainerInfoDto containerInfoDto = findSourceAndProcessByPriority(id, MappingJsonConstants.METAINFO, stageName);
            return containerInfoDto != null ?
                    packetManagerService.getMetaInfo(id, containerInfoDto.getSource(), containerInfoDto.getProcess()) :
                    packetManagerService.getMetaInfo(id, null, process);
        } catch (Exception e) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    id, "Error in getMetaInfo: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Get document by priority
     *
     * @param id
     * @param documentName
     * @param process
     * @param stageName
     * @return
     * @throws ApisResourceAccessException
     * @throws PacketManagerException
     * @throws JsonProcessingException
     * @throws IOException
     */
    public Document getDocument(String id, String documentName, String process, ProviderStageName stageName)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        try {
            ContainerInfoDto containerInfoDto = findSourceAndProcessByPriority(id, documentName, stageName);
            return containerInfoDto == null ?
                    packetManagerService.getDocument(id, documentName, process) :
                    packetManagerService.getDocument(id, documentName, containerInfoDto.getSource(), containerInfoDto.getProcess());
        } catch (Exception e) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    id, "Error in getDocument: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Validate packet
     *
     * @param id
     * @param process
     * @param stageName
     * @return
     * @throws ApisResourceAccessException
     * @throws PacketManagerException
     * @throws JsonProcessingException
     * @throws IOException
     */
    public ValidatePacketResponse validate(String id, String process, ProviderStageName stageName)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        try {
            ContainerInfoDto containerInfoDto = findSourceAndProcessByPriority(id, MappingJsonConstants.VALIDATE, stageName);
            return containerInfoDto == null ?
                    packetManagerService.validate(id, null, process) :
                    packetManagerService.validate(id, containerInfoDto.getSource(), containerInfoDto.getProcess());
        } catch (Exception e) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    id, "Error in validate: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Get audits by priority
     *
     * @param id
     * @param process
     * @param stageName
     * @return
     * @throws ApisResourceAccessException
     * @throws PacketManagerException
     * @throws JsonProcessingException
     * @throws IOException
     */
    public List<FieldResponseDto> getAudits(String id, String process, ProviderStageName stageName)
            throws ApisResourceAccessException, PacketManagerException, JsonProcessingException, IOException {
        try {
            ContainerInfoDto containerInfoDto = findSourceAndProcessByPriority(id, MappingJsonConstants.AUDITS, stageName);
            return containerInfoDto == null ?
                    packetManagerService.getAudits(id, null, process) :
                    packetManagerService.getAudits(id, containerInfoDto.getSource(), containerInfoDto.getProcess());
        } catch (Exception e) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    id, "Error in getAudits: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Get biometrics by priority with input as mapping json key
     *
     * @param id
     * @param mappingJsonKey
     * @param process
     * @param stageName
     * @return
     * @throws IOException
     * @throws ApisResourceAccessException
     * @throws PacketManagerException
     * @throws JsonProcessingException
     */
    public BiometricRecord getBiometricsByMappingJsonKey(String id, String mappingJsonKey, String process, ProviderStageName stageName)
            throws IOException, ApisResourceAccessException, PacketManagerException, JsonProcessingException {
        try {
            String biometricLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(
                            utilities.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY), mappingJsonKey),
                    MappingJsonConstants.VALUE);

            if (biometricLabel == null) {
                throw new PacketManagerException("Biometric label not found for key: " + mappingJsonKey, "BIOMETRIC_LABEL_NOT_FOUND");
            }

            return getBiometrics(id, biometricLabel, process, stageName);
        } catch (Exception e) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    id, "Error in getBiometricsByMappingJsonKey: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Get biometrics by priority
     *
     * @param id
     * @param person
     * @param process
     * @param stageName
     * @return
     * @throws IOException
     * @throws ApisResourceAccessException
     * @throws PacketManagerException
     * @throws JsonProcessingException
     */
    public BiometricRecord getBiometrics(String id, String person, String process, ProviderStageName stageName)
            throws IOException, ApisResourceAccessException, PacketManagerException, JsonProcessingException {
        return getBiometricsInternal(id, person, null, process, stageName);
    }

    /**
     * Get biometrics with specific modalities
     */
    public BiometricRecord getBiometrics(String id, String person, List<String> modalities, String process, ProviderStageName stageName)
            throws IOException, ApisResourceAccessException, PacketManagerException, JsonProcessingException {
        return getBiometricsInternal(id, person, modalities, process, stageName);
    }

    private BiometricRecord getBiometricsInternal(String id, String person, List<String> modalities, String process, ProviderStageName stageName)
            throws IOException, ApisResourceAccessException, PacketManagerException, JsonProcessingException {
        try {
            Map<String, String> finalKeyMap = PacketManagerHelper.getKeyMap(stageName, providerConfiguration).isEmpty() ? null
                    : PacketManagerHelper.getKeyMap(stageName, providerConfiguration).entrySet().stream()
                    .filter(key -> key.getKey().contains(person))
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

            // if there is no priority set for individual stage
            if (CollectionUtils.isEmpty(finalKeyMap)) {
                return packetManagerService.getBiometrics(id, person, modalities, null, process);
            }
            // else get fields based on priority set in individual stage level
            InfoResponseDto infoResponseDto = packetManagerService.info(id);

            if (infoResponseDto == null) {
                throw new ApisResourceAccessException("Unable to get packet info for id: " + id);
            }

            // if there is a type/subtype set in properties
            if (finalKeyMap.get(person) != null) {
                ContainerInfoDto containerInfoDto = PacketManagerHelper.getContainerInfo(finalKeyMap, person, infoResponseDto);
                modalities = CollectionUtils.isEmpty(modalities) ?
                        PacketManagerHelper.getTypeSubtypeModalities(containerInfoDto) : modalities;
                return packetManagerService.getBiometrics(id, person, modalities,
                        containerInfoDto.getSource(), containerInfoDto.getProcess());
            }

            Set<ContainerInfoDto> containers = new HashSet<>();
            BiometricRecord biometricRecord = null;

            for (String key : finalKeyMap.keySet()) {
                ContainerInfoDto containerInfoDto = PacketManagerHelper.getBiometricContainerInfo(finalKeyMap, person, key, infoResponseDto);
                if (containerInfoDto != null) {
                    Optional<Boolean> optional = containers.stream()
                            .map(c -> c.equals(containerInfoDto))
                            .findAny();
                    if (!optional.isPresent() || optional.get().equals(Boolean.FALSE)) {
                        containers.add(containerInfoDto);
                    }
                }
            }

            for (ContainerInfoDto containerInfoDto : containers) {
                List<String> containerModalities = CollectionUtils.isEmpty(modalities) ?
                        PacketManagerHelper.getTypeSubtypeModalities(containerInfoDto) : modalities;
                BiometricRecord record = packetManagerService.getBiometrics(
                        id, person, containerModalities, containerInfoDto.getSource(), containerInfoDto.getProcess());

                if (record != null) {
                    if (biometricRecord == null) {
                        biometricRecord = new BiometricRecord();
                    }
                    if (record.getSegments() != null) {
                        biometricRecord.getSegments().addAll(record.getSegments());
                    }
                }
            }

            return biometricRecord;
        } catch (Exception e) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    id, "Error in getBiometricsInternal: " + e.getMessage());
            throw e;
        }
    }

    private Map<String, String> getFieldsByPriority(String id, ProviderStageName stageName, List<String> fields)
            throws ApisResourceAccessException, IOException, PacketManagerException, JsonProcessingException {
        try {
            Map<String, String> fieldMap = new HashMap<>();
            InfoResponseDto infoResponseDto = packetManagerService.info(id);

            if (infoResponseDto == null) {
                throw new ApisResourceAccessException("Unable to get packet info for id: " + id);
            }

            // if there is only one source then save time
            if (infoResponseDto.getInfo().size() == 1) {
                ContainerInfoDto containerInfoDto = infoResponseDto.getInfo().iterator().next();
                fieldMap = packetManagerService.getFields(id, fields, containerInfoDto.getSource(), containerInfoDto.getProcess());
                return fieldMap;
            }

            // else find correct source for each field
            Map<String, String> keyMap = PacketManagerHelper.getKeyMap(stageName, providerConfiguration);
            for (String field : fields) {
                ContainerInfoDto containerInfoDto = PacketManagerHelper.getContainerInfo(keyMap, field, infoResponseDto);
                if (containerInfoDto != null) {
                    String fieldValue = packetManagerService.getField(id, field, containerInfoDto.getSource(), containerInfoDto.getProcess());
                    fieldMap.put(field, fieldValue);
                }
            }

            ContainerInfoDto containerInfoDto = packetManagerHelper.getBiometricSourceAndProcess(fields, keyMap, infoResponseDto.getInfo());
            if (containerInfoDto != null) {
                fieldMap.put(packetManagerHelper.getApplicantBiometricLabel(),
                        packetManagerService.getField(id, packetManagerHelper.getApplicantBiometricLabel(),
                                containerInfoDto.getSource(), containerInfoDto.getProcess()));
            }

            return fieldMap;
        } catch (Exception e) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    id, "Error in getFieldsByPriority: " + e.getMessage());
            throw e;
        }
    }

    private ContainerInfoDto findSourceAndProcessByPriority(String id, String field, ProviderStageName stageName)
            throws ApisResourceAccessException, IOException, PacketManagerException, JsonProcessingException {
        try {
            Map<String, String> keyMap = PacketManagerHelper.getKeyMap(stageName, providerConfiguration);
            if (keyMap != null && keyMap.get(field) != null) {
                InfoResponseDto infoResponseDto = packetManagerService.info(id);
                if (infoResponseDto == null) {
                    throw new ApisResourceAccessException("Unable to get packet info for id: " + id);
                }
                return PacketManagerHelper.getContainerInfo(keyMap, field, infoResponseDto);
            }
            return null;
        } catch (Exception e) {
            regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                    id, "Error in findSourceAndProcessByPriority: " + e.getMessage());
            throw e;
        }
    }
}