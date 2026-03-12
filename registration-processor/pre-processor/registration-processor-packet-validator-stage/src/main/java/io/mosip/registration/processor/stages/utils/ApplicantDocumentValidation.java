package io.mosip.registration.processor.stages.utils;

import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.packet.storage.dto.Document;
import io.mosip.registration.processor.packet.storage.exception.IdentityNotFoundException;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * The Class ApplicantDocumentValidation.
 *
 * @author Nagalakshmi
 */
@Component
public class ApplicantDocumentValidation {

    private static Logger regProcLogger = RegProcessorLogger.getLogger(ApplicantDocumentValidation.class);

    @Autowired
    private PriorityBasedPacketManagerService packetManagerService;

    @Autowired
    private Utilities utility;

    private static final String VALUE = "value";

    public boolean validateDocument(String registrationId, String process, Map<String, BiometricRecord> fetchedBiometrics) throws IdentityNotFoundException, IOException, ApisResourceAccessException, PacketManagerException, JsonProcessingException {
        JSONObject docMappingJson = utility.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT);
        JSONObject identityMappingJson = utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);

        List<String> docFieldNames = new ArrayList<>();
        for (Object doc : docMappingJson.values()) {
            Map docMap = (LinkedHashMap) doc;
            docFieldNames.add(docMap.values().iterator().next().toString());
        }

        String applicantBiometricLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(identityMappingJson, MappingJsonConstants.INDIVIDUAL_BIOMETRICS), VALUE);
        String introducerBiometricLabel = JsonUtil.getJSONValue(JsonUtil.getJSONObject(identityMappingJson, MappingJsonConstants.INTRODUCER_BIO), VALUE);

        // Single batch getFields call for all document + biometric presence checks
        List<String> allFields = new ArrayList<>(docFieldNames);
        allFields.add(applicantBiometricLabel);
        allFields.add(introducerBiometricLabel);
        Map<String, String> fieldValues = packetManagerService.getFields(registrationId, allFields, process, ProviderStageName.PACKET_VALIDATOR);

        // Collect documents that need fetching
        List<String> docsToFetch = new ArrayList<>();
        for (String docValue : docFieldNames) {
            if (fieldValues.get(docValue) != null) docsToFetch.add(docValue);
        }
        boolean needIndividualBio = fieldValues.get(applicantBiometricLabel) != null;
        boolean needIntroducerBio = fieldValues.get(introducerBiometricLabel) != null;

        // Fire all document + biometrics fetches in parallel using virtual threads
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            Map<String, CompletableFuture<Document>> docFutures = new LinkedHashMap<>();
            for (String docValue : docsToFetch) {
                docFutures.put(docValue, CompletableFuture.supplyAsync(() -> {
                    try {
                        return packetManagerService.getDocument(registrationId, docValue, process, ProviderStageName.PACKET_VALIDATOR);
                    } catch (Exception e) { throw new CompletionException(e); }
                }, executor));
            }

            CompletableFuture<BiometricRecord> individualBioFuture = needIndividualBio
                    ? CompletableFuture.supplyAsync(() -> {
                        try {
                            return packetManagerService.getBiometricsByMappingJsonKey(registrationId, MappingJsonConstants.INDIVIDUAL_BIOMETRICS, process, ProviderStageName.PACKET_VALIDATOR);
                        } catch (Exception e) { throw new CompletionException(e); }
                    }, executor) : null;

            CompletableFuture<BiometricRecord> introducerBioFuture = needIntroducerBio
                    ? CompletableFuture.supplyAsync(() -> {
                        try {
                            return packetManagerService.getBiometricsByMappingJsonKey(registrationId, MappingJsonConstants.INTRODUCER_BIO, process, ProviderStageName.PACKET_VALIDATOR);
                        } catch (Exception e) { throw new CompletionException(e); }
                    }, executor) : null;

            // Validate documents
            for (Map.Entry<String, CompletableFuture<Document>> entry : docFutures.entrySet()) {
                try {
                    if (entry.getValue().join() == null) {
                        regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                                LoggerFileConstant.REGISTRATIONID.toString(), registrationId, "Missing document : " + entry.getKey());
                        return false;
                    }
                } catch (CompletionException e) { rethrowChecked(e); }
            }

            // Validate INDIVIDUAL_BIOMETRICS presence
            if (individualBioFuture != null) {
                try {
                    BiometricRecord biometricRecord = individualBioFuture.join();
                    if (biometricRecord == null || biometricRecord.getSegments() == null || biometricRecord.getSegments().size() == 0) {
                        regProcLogger.error(LoggerFileConstant.SESSIONID.toString(),
                                LoggerFileConstant.REGISTRATIONID.toString(), registrationId, "Missing document : " + applicantBiometricLabel);
                        return false;
                    }
                    if (fetchedBiometrics != null) fetchedBiometrics.put(MappingJsonConstants.INDIVIDUAL_BIOMETRICS, biometricRecord);
                } catch (CompletionException e) { rethrowChecked(e); }
            }

            // Validate INTRODUCER_BIO presence
            if (introducerBioFuture != null) {
                try {
                    BiometricRecord biometricRecord = introducerBioFuture.join();
                    if (biometricRecord == null || biometricRecord.getSegments() == null || biometricRecord.getSegments().size() == 0)
                        return false;
                    if (fetchedBiometrics != null) fetchedBiometrics.put(MappingJsonConstants.INTRODUCER_BIO, biometricRecord);
                } catch (CompletionException e) { rethrowChecked(e); }
            }
        } finally {
            executor.close();
        }

        regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
                registrationId, "ApplicantDocumentValidation::validateApplicantData::exit");
        return true;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T { throw (T) t; }

    private void rethrowChecked(CompletionException e) throws ApisResourceAccessException, PacketManagerException, IOException, JsonProcessingException {
        Throwable cause = e.getCause();
        while (cause instanceof CompletionException && cause.getCause() != null) cause = cause.getCause();
        if (cause instanceof ApisResourceAccessException) throw (ApisResourceAccessException) cause;
        if (cause instanceof PacketManagerException) throw (PacketManagerException) cause;
        if (cause instanceof JsonProcessingException) throw (JsonProcessingException) cause;
        if (cause instanceof IOException) throw (IOException) cause;
        sneakyThrow(cause);
    }


}