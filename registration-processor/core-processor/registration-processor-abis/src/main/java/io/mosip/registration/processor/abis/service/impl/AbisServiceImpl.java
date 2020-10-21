package io.mosip.registration.processor.abis.service.impl;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.registration.processor.abis.exception.MissingMandatoryFieldsException;
import io.mosip.registration.processor.abis.service.AbisService;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.abis.AbisIdentifyRequestDto;
import io.mosip.registration.processor.core.packet.dto.abis.AbisIdentifyResponseDto;
import io.mosip.registration.processor.core.packet.dto.abis.AbisInsertRequestDto;
import io.mosip.registration.processor.core.packet.dto.abis.AbisInsertResponseDto;
import io.mosip.registration.processor.core.packet.dto.abis.AbisPingRequestDto;
import io.mosip.registration.processor.core.packet.dto.abis.AbisPingResponseDto;
import io.mosip.registration.processor.core.packet.dto.abis.CandidateListDto;
import io.mosip.registration.processor.core.packet.dto.abis.CandidatesDto;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Class AbisServiceImpl.
 *
 * @author M1048860 Kiran Raj
 */
@Service
public class AbisServiceImpl implements AbisService {

	private static final String duplicateSet = "dummy.abis.return.duplicate";
	/** The rest client service. */
	@Autowired
	private RegistrationProcessorRestClientService<Object> restClientService;

	/** The Constant DUPLICATE. */
	private static final String DUPLICATE = "duplicate";

	/** The Constant INSERT. */
	private static final String ABIS_INSERT = "mosip.abis.insert";

	/** The Constant IDENTIFY. */
	private static final String ABIS_IDENTIFY = "mosip.abis.identify";

	private static Set<String> storedRefId = new HashSet<>();

	private static Set<String> actualStoredRefId = new HashSet<>();

	@Autowired
	private Environment env;

	/** The Constant TESTFINGERPRINT. */
	/*@Value("${TESTFINGERPRINT}")
	private String testFingerPrint;*/

	/** The Constant TESTIRIS. */
	/*@Value("${TESTIRIS}")
	private String testIris;*/

	/** The Constant TESTFACE. */
	/*@Value("${TESTFACE}")
	private String testFace;*/

	/** The reg proc logger. */
	private static Logger regProcLogger = RegProcessorLogger.getLogger(AbisServiceImpl.class);

	@Override
	public AbisInsertResponseDto insert(AbisInsertRequestDto abisInsertRequestDto) {

		AbisInsertResponseDto response = new AbisInsertResponseDto();
		String referenceId = abisInsertRequestDto.getReferenceId();
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REFFERENCEID.toString(),
				referenceId, "AbisServiceImpl::insert()::entry");
		if (storedRefId.size() < 1000)
			storedRefId.add(referenceId);

		regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REFFERENCEID.toString(),
				referenceId, " referenceId storeList " + storedRefId);

		response.setId(ABIS_INSERT);
		response.setRequestId(abisInsertRequestDto.getRequestId());
		response.setResponsetime(abisInsertRequestDto.getRequesttime());
		response.setReturnValue("1");

		//Document doc;
		/*try {
			//doc = getCbeffDocument(referenceId);
			if (testFingerPrint == null || testIris == null || testFace == null) {
				regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REFFERENCEID.toString(),
						referenceId, "Test Tags are not present");
			}
			if (doc != null) {
				NodeList fingerNodeList = doc.getElementsByTagName(testFingerPrint);
				NodeList irisNodeList = doc.getElementsByTagName(testIris);
				NodeList faceNodeList = doc.getElementsByTagName(testFace);

				if (fingerNodeList.getLength() > 0 || irisNodeList.getLength() > 0 || faceNodeList.getLength() > 0) {
					response.setReturnValue("1");

				} else {
					response.setReturnValue("2");
					response.setFailureReason("7");
				}
			} else {
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REFFERENCEID.toString(),
						referenceId, "AbisServiceImpl():: unable to fect CBEF file ");
				response.setReturnValue("2");
				response.setFailureReason("7");
			}
			response.setReturnValue("1");

		} catch (ApisResourceAccessException | ParserConfigurationException | SAXException | IOException e) {
			response.setReturnValue("2");
			response.setFailureReason("7");
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					referenceId, "ApisResourceAccessException : Unable to acces getting cbef url."
							+ ExceptionUtils.getStackTrace(e));

		} catch (MissingMandatoryFieldsException e) {
			response.setReturnValue("2");
			response.setFailureReason("5");
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					referenceId, "MissingMandatoryFieldsException : Mandatory fields are missing in Request."
							+ ExceptionUtils.getStackTrace(e));

		} catch (Exception e) {
			response.setReturnValue("2");
			response.setFailureReason("3");

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REFFERENCEID.toString(),
					referenceId, "Due to some internal error, abis failed" + ExceptionUtils.getStackTrace(e));

		}*/

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REFFERENCEID.toString(),
				referenceId, "AbisServiceImpl::insert()::exit");

		return response;
	}

	private Document getCbeffDocument(String referenceId)
			throws ApisResourceAccessException, ParserConfigurationException, SAXException, IOException {

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REFFERENCEID.toString(),
				referenceId, "AbisServiceImpl::getCbeffDocument()::entry");
		if (referenceId != null) {
			List<String> pathSegments = new ArrayList<>();
			pathSegments.add(referenceId);

			byte[] bytefile = (byte[]) restClientService.getApi(ApiName.BIODEDUPE, pathSegments, "", "", byte[].class);
			if (bytefile == null) {
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REFFERENCEID.toString(),
						referenceId,
						"AbisServiceImpl::getCbeffDocument():: get BIODEDUPE service call ended	and cbeff file is not present for the referenceID");
			}

			if (bytefile != null) {
				regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REFFERENCEID.toString(),
						referenceId,
						"AbisServiceImpl::getCbeffDocument():: get BIODEDUPE service call ended and Got Byte file from BioDedupe api");
				String byteFileStr = new String(bytefile);

				InputSource is = new InputSource();
				is.setCharacterStream(new StringReader(byteFileStr));

				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				dbFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				return dBuilder.parse(is);
			}

		} else {
			throw new MissingMandatoryFieldsException(PlatformErrorMessages.MISSING_MANDATORY_FIELDS.getMessage());
		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REFFERENCEID.toString(),
				referenceId, "AbisServiceImpl::getCbeffDocument()::exit");

		return null;
	}

	@Override
	public AbisIdentifyResponseDto identify(AbisIdentifyRequestDto identifyRequest) {
		boolean duplicate = false;
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"AbisServiceImpl::performDedupe()::entry");
		AbisIdentifyResponseDto response = new AbisIdentifyResponseDto();
		String identifyReqId = identifyRequest.getReferenceId();
		if (storedRefId.size() < 1000)
			storedRefId.add(identifyReqId);

		//Document doc;
		try {
			//doc = getCbeffDocument(identifyReqId);

			response.setId(ABIS_IDENTIFY);
			response.setRequestId(identifyRequest.getRequestId());
			response.setResponsetime(identifyRequest.getRequesttime());

			response.setReturnValue("1");
			String duplicateIndicator = env.getProperty(duplicateSet);
			if (StringUtils.isNotEmpty(duplicateIndicator) && duplicateIndicator.equalsIgnoreCase("true")) {
				addCandidateList(identifyReqId, identifyRequest, response);
			}

			/*if (doc != null) {
				NodeList fingerNodeList = doc.getElementsByTagName(testFingerPrint);
				if (fingerNodeList != null) {
					duplicate = checkDuplicate(duplicate, fingerNodeList);
				}

				NodeList irisNodeList = doc.getElementsByTagName(testIris);
				if (irisNodeList != null) {
					duplicate = checkDuplicate(duplicate, irisNodeList);
				}
				NodeList faceNodeList = doc.getElementsByTagName(testFace);
				if (faceNodeList != null) {
					duplicate = checkDuplicate(duplicate, faceNodeList);
				}
				response.setReturnValue("1");

				if (duplicate) {
					addCandidateList(identifyReqId, identifyRequest, response);
				}
			} else {
				response.setReturnValue("2");
				response.setFailureReason("7");
			}*/

		} /*catch (ApisResourceAccessException | ParserConfigurationException | SAXException | IOException e) {
			response.setReturnValue("2");
			response.setFailureReason("7");
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					identifyReqId, "ApisResourceAccessException : Unable to acces getting cbef url."
							+ ExceptionUtils.getStackTrace(e));

		} catch (MissingMandatoryFieldsException e) {
			response.setReturnValue("2");
			response.setFailureReason("5");
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					identifyReqId, "MissingMandatoryFieldsException : Mandatory fields are missing in Request."
							+ ExceptionUtils.getStackTrace(e));

		}*/ catch (Exception e) {
			response.setReturnValue("2");
			response.setFailureReason("3");

			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					identifyReqId, "Due to some internal error, abis failed" + ExceptionUtils.getStackTrace(e));

		}

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"AbisServiceImpl::performDedupe()::exit");
		return response;
	}

	private synchronized void addCandidateList(String identifyReqId, AbisIdentifyRequestDto identifyRequest,
			AbisIdentifyResponseDto response) {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"AbisServiceImpl::addCandidateList()::Entry");
		int count = 0;

		CandidateListDto cd = new CandidateListDto();
		if (storedRefId != null && storedRefId.size() >= 1) {
			for (String refId : storedRefId) {
				if (refId != null && !identifyRequest.getReferenceId().equals(refId)) {
					actualStoredRefId.add(refId);
				}
			}
		}
		regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"AbisServiceImpl::addCandidateList()::storedRefId" + storedRefId);
		regProcLogger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"AbisServiceImpl::addCandidateList()::actualStoredRefId" + actualStoredRefId);
		ArrayList<String> storedRefIdList = new ArrayList<>(actualStoredRefId);
		Collections.shuffle(storedRefIdList);
		List<CandidatesDto> candidatesDtoList = new ArrayList<>();
		if (storedRefIdList.size() > 5) {
			for (int i = 0; i < 5; i++) {
				if (!(identifyRequest.getReferenceId().equals(storedRefIdList.get(i)))) {
					if (StringUtils.isNotEmpty(storedRefIdList.get(i)) && !storedRefIdList.get(i).equalsIgnoreCase("null")) {
						CandidatesDto candidatesDto = new CandidatesDto();
						candidatesDto.setReferenceId(storedRefIdList.get(i));
						candidatesDtoList.add(candidatesDto);
						count++;
					}
				}
			}
		}


		cd.setCount(count + "");
		if (count != 0) {
			cd.setCandidates(candidatesDtoList.isEmpty() ? null : getCandidateArray(candidatesDtoList));
			response.setCandidateList(cd);
		}
		if (storedRefId.size() < 1000)
			storedRefId.add(identifyReqId);

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.USERID.toString(), "",
				"AbisServiceImpl::addCandidateList()::Exit");

	}

	private CandidatesDto[] getCandidateArray(List<CandidatesDto> candidatesDtoList) {
		CandidatesDto[] candidatesDtos = new CandidatesDto[candidatesDtoList.size()];
		candidatesDtos = candidatesDtoList.toArray(candidatesDtos);
		return candidatesDtos;
	}

	/**
	 * Check duplicate.
	 *
	 * @param duplicate
	 *            the duplicate
	 * @param nodeList
	 *            the node list
	 * @return true, if successful
	 */
	private boolean checkDuplicate(boolean duplicate, NodeList nodeList) {
		for (int i = 0; i < nodeList.getLength(); i++) {
			String value = nodeList.item(i).getTextContent();
			if (value.equalsIgnoreCase(DUPLICATE)) {
				duplicate = true;
				break;
			}
		}
		return duplicate;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.processor.abis.service.impl.AbisService#delete()
	 */
	@Override
	public void delete() {
		// Delete should be implemented in future
	}

	@Override
	public AbisPingResponseDto ping(AbisPingRequestDto abisPingRequestDto) {
		// Ping should be implemented in future
		return null;
	}
}
