//package io.mosip.registration.processor.stages.utils;
//
//import static org.junit.Assert.assertFalse;
//import static org.junit.Assert.assertTrue;
//import static org.mockito.Matchers.any;
//
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.text.ParseException;
//
//import io.mosip.kernel.packetmanager.exception.ApiNotAccessibleException;
//import io.mosip.kernel.packetmanager.spi.PacketReaderService;
//import io.mosip.kernel.packetmanager.util.IdSchemaUtils;
//
//import org.apache.commons.io.IOUtils;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.powermock.api.mockito.PowerMockito;
//import org.powermock.core.classloader.annotations.PrepareForTest;
//import org.powermock.modules.junit4.PowerMockRunner;
//import org.springframework.core.env.Environment;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//
//import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
//import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
//import io.mosip.registration.processor.core.exception.PacketDecryptionFailureException;
//import io.mosip.registration.processor.core.exception.RegistrationProcessorCheckedException;
//import io.mosip.registration.processor.core.packet.dto.applicantcategory.ApplicantTypeDocument;
//import io.mosip.registration.processor.packet.storage.exception.IdentityNotFoundException;
//import io.mosip.registration.processor.packet.storage.utils.Utilities;
//
///**
// * The Class ApplicantDocumentValidationTest.
// */
//@RunWith(PowerMockRunner.class)
//@PrepareForTest({ Utilities.class })
//public class ApplicantDocumentValidationTest {
//
//	/** The utility. */
//	@Mock
//	Utilities utility;
//	
//	@Mock PacketReaderService packetReaderService;
//	
//	@Mock IdSchemaUtils idSchemaUtils;
//
//	private ApplicantDocumentValidation applicantDocumentValidation;
//	
//	/**
//	 * Sets the up.
//	 *
//	 * @throws IOException                           Signals that an I/O exception
//	 *                                               has occurred.
//	 * @throws ApisResourceAccessException           the apis resource access
//	 *                                               exception
//	 * @throws ParseException                        the parse exception
//	 * @throws                                       io.mosip.kernel.core.exception.IOException
//	 * @throws PacketDecryptionFailureException
//	 * @throws RegistrationProcessorCheckedException
//	 * @throws                                       io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException
//	 */
//	@Before
//	public void setUp()
//			throws IOException, ApisResourceAccessException, ParseException, PacketDecryptionFailureException,
//			io.mosip.kernel.core.exception.IOException, RegistrationProcessorCheckedException,
//			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException, ApiNotAccessibleException {
//
//		when.
//		applicantDocumentValidation = new ApplicantDocumentValidation(utility,idSchemaUtils,packetReaderService );
//	}
//
//	/**
//	 * Test applicant document validation adult success.
//	 *
//	 * @throws ApisResourceAccessException           the apis resource access
//	 *                                               exception
//	 * @throws NoSuchFieldException                  the no such field exception
//	 * @throws IllegalAccessException                the illegal access exception
//	 * @throws IOException                           Signals that an I/O exception
//	 *                                               has occurred.
//	 * @throws ParseException                        the parse exception
//	 * @throws ParseException                        the parse exception
//	 * @throws JSONException                         the JSON exception
//	 * @throws                                       io.mosip.kernel.core.exception.IOException
//	 * @throws PacketDecryptionFailureException
//	 * @throws RegistrationProcessorCheckedException
//	 * @throws                                       io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException
//	 */
//	@Test
//	public void testApplicantDocumentValidationAdultSuccess() throws ApisResourceAccessException, IOException,
//			ApiNotAccessibleException, io.mosip.kernel.core.exception.IOException,
//			RegistrationProcessorCheckedException,
//			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException {
//		boolean isApplicantDocumentValidated = applicantDocumentValidation.validateDocument("1234", jsonStringID);
//		assertTrue("Test for successful Applicant Document Validation success for adult", isApplicantDocumentValidated);
//	}
//
//	/**
//	 * Test applicant document validation child success.
//	 *
//	 * @throws ApisResourceAccessException           the apis resource access
//	 *                                               exception
//	 * @throws NoSuchFieldException                  the no such field exception
//	 * @throws IllegalAccessException                the illegal access exception
//	 * @throws IOException                           Signals that an I/O exception
//	 *                                               has occurred.
//	 * @throws ParseException                        the parse exception
//	 * @throws ParseException                        the parse exception
//	 * @throws JSONException                         the JSON exception
//	 * @throws                                       io.mosip.kernel.core.exception.IOException
//	 * @throws PacketDecryptionFailureException
//	 * @throws RegistrationProcessorCheckedException
//	 * @throws                                       io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException
//	 */
//	@Test
//	public void testApplicantDocumentValidationChildSuccess() throws ApisResourceAccessException, IOException,
//			ApiNotAccessibleException, io.mosip.kernel.core.exception.IOException,
//			RegistrationProcessorCheckedException,
//			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException {
//		Mockito.when(utility.getApplicantAge(any())).thenReturn(4);
//		boolean isApplicantDocumentValidated = applicantDocumentValidation.validateDocument("1234", jsonStringID);
//		assertTrue("Test for successful Applicant Document Validation for child", isApplicantDocumentValidated);
//	}
//
//	/**
//	 * Test applicant document validation IDJSON null.
//	 *
//	 * @throws ApisResourceAccessException           the apis resource access
//	 *                                               exception
//	 * @throws NoSuchFieldException                  the no such field exception
//	 * @throws IllegalAccessException                the illegal access exception
//	 * @throws IOException                           Signals that an I/O exception
//	 *                                               has occurred.
//	 * @throws ParseException                        the parse exception
//	 * @throws ParseException                        the parse exception
//	 * @throws JSONException                         the JSON exception
//	 * @throws                                       io.mosip.kernel.core.exception.IOException
//	 * @throws PacketDecryptionFailureException
//	 * @throws RegistrationProcessorCheckedException
//	 * @throws                                       io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException
//	 */
//	@Test(expected = IdentityNotFoundException.class)
//	public void testApplicantDocumentValidationIDJSONNull() throws ApisResourceAccessException, IOException,
//			ApiNotAccessibleException, io.mosip.kernel.core.exception.IOException,
//			RegistrationProcessorCheckedException,
//			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException {
//
//		boolean isApplicantDocumentValidated = applicantDocumentValidation.validateDocument("1234", "{}");
//
//	}
//
//	/**
//	 * Test invalid type.
//	 *
//	 * @throws ApisResourceAccessException           the apis resource access
//	 *                                               exception
//	 * @throws NoSuchFieldException                  the no such field exception
//	 * @throws IllegalAccessException                the illegal access exception
//	 * @throws IOException                           Signals that an I/O exception
//	 *                                               has occurred.
//	 * @throws ParseException                        the parse exception
//	 * @throws ParseException                        the parse exception
//	 * @throws JSONException                         the JSON exception
//	 * @throws                                       io.mosip.kernel.core.exception.IOException
//	 * @throws PacketDecryptionFailureException
//	 * @throws RegistrationProcessorCheckedException
//	 * @throws                                       io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException
//	 */
//	@Test
//	public void testInvalidType() throws ApisResourceAccessException, IOException,
//			ApiNotAccessibleException, io.mosip.kernel.core.exception.IOException,
//			RegistrationProcessorCheckedException,
//			io.mosip.kernel.packetmanager.exception.PacketDecryptionFailureException {
//
//		InputStream inputStream = new FileInputStream("src/test/resources/ID2.json");
//		byte[] bytes = IOUtils.toByteArray(inputStream);
//		jsonStringID = new String(bytes);
//		boolean isApplicantDocumentValidated = applicantDocumentValidation.validateDocument("1234", jsonStringID);
//		assertFalse("Test for successful Applicant Document Validation for child", isApplicantDocumentValidated);
//
//	}
//}
