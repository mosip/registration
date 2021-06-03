package io.mosip.registration.processor.cbeffutil.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringRunner;

import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.spi.CbeffUtil;
import io.mosip.registration.processor.core.util.CbeffToBiometricUtil;
import io.mosip.registration.processor.core.util.exception.BiometricTagMatchException;

/**
 * The Class CbeffToBiometricUtilTest.
 * 
 * @author M1048358 Alok
 */
@RunWith(SpringRunner.class)
public class CbeffToBiometricUtilTest {
	
	/** The util. */
	@InjectMocks
	private CbeffToBiometricUtil util;
	
	/** The cbeff util. */
	@Mock
	private CbeffUtil cbeffUtil;
	
	/**
	 * Setup.
	 *
	 * @throws Exception the exception
	 */
	@Before
	public void setup() throws Exception {
		byte[] bioBytes = "individual biometric value".getBytes();
		List<BiometricType> singleList = new ArrayList<>();
		singleList.add(BiometricType.FACE);
		List<String> subtypeList = new ArrayList<>();
		BIR type = new BIR();
		type.setBdb(bioBytes);
		BDBInfo bDBInfo = new BDBInfo();
		bDBInfo.setType(singleList);
		bDBInfo.setSubtype(subtypeList);
		type.setBdbInfo(bDBInfo);
		List<BIR> birList = new ArrayList<>();
		birList.add(type);
		Mockito.when(cbeffUtil.getBIRDataFromXML(Matchers.any())).thenReturn(birList);
	}
	
	/**
	 * Test image bytes success.
	 *
	 * @throws Exception the exception
	 */
	@Test
	public void testImageBytesSuccess() throws Exception {
		ClassLoader classLoader = getClass().getClassLoader();
		File cbeffFile = new File(classLoader.getResource("TestCbeff.xml").getFile());
		InputStream inputStream = new FileInputStream(cbeffFile);
		String cbeff = IOUtils.toString(inputStream, "UTF-8");
		List<String> subtype = new ArrayList<>();
		byte[] photo = util.getImageBytes(cbeff, "FACE", subtype);
	
		assertTrue(photo != null);
	}
	
	/**
	 * Test image bytes failure.
	 *
	 * @throws Exception the exception
	 */
	@Test
	public void testImageBytesFailure() throws Exception {
		ClassLoader classLoader = getClass().getClassLoader();
		File cbeffFile = new File(classLoader.getResource("TestCbeff.xml").getFile());
		InputStream inputStream = new FileInputStream(cbeffFile);
		String cbeff = IOUtils.toString(inputStream, "UTF-8");
		List<String> subtype = new ArrayList<>();
		byte[] photo = util.getImageBytes(cbeff, "", subtype);
		
		assertFalse(photo != null);
	}

	@Test
	public void testmergeCbeffSuccess() throws Exception {
		ClassLoader classLoader = getClass().getClassLoader();
		File cbeffFile1 = new File(classLoader.getResource("cbeff1.xml").getFile());
		InputStream inputStream1 = new FileInputStream(cbeffFile1);
		String cbeff1 = IOUtils.toString(inputStream1, "UTF-8");

		byte[] bioBytes = cbeff1.getBytes();
		List<BiometricType> singleList = new ArrayList<>();
		singleList.add(BiometricType.FACE);
		List<String> subtypeList = new ArrayList<>();
		BIR type = new BIR();
		type.setBdb(bioBytes);
		BDBInfo bdbInfo = new BDBInfo();
		bdbInfo.setType(singleList);
		bdbInfo.setSubtype(subtypeList);
		type.setBdbInfo(bdbInfo);
		List<BIR> birList = new ArrayList<>();
		birList.add(type);

		File cbeffFile2 = new File(classLoader.getResource("cbeff2.xml").getFile());
		InputStream inputStream2 = new FileInputStream(cbeffFile2);
		String cbeff2 = IOUtils.toString(inputStream2, "UTF-8");
		byte[] bioBytes2 = cbeff2.getBytes();
		List<BiometricType> singleList2 = new ArrayList<>();
		singleList2.add(BiometricType.IRIS);
		List<String> subtypeList2 = new ArrayList<>();
		BIR type2 = new BIR();
		type2.setBdb(bioBytes2);
		BDBInfo bdbInfo2 = new BDBInfo();
		bdbInfo2.setType(singleList2);
		bdbInfo2.setSubtype(subtypeList2);
		type2.setBdbInfo(bdbInfo2);
		List<BIR> birList2 = new ArrayList<>();
		birList2.add(type2);
		Mockito.when(cbeffUtil.getBIRDataFromXML(Matchers.any())).thenReturn(birList).thenReturn(birList2);

		Mockito.when(cbeffUtil.createXML(Matchers.any())).thenReturn("mergedcbeff".getBytes());

		InputStream stream = util.mergeCbeff(cbeff1, cbeff2);
		byte[] result = IOUtils.toString(stream, "UTF-8").getBytes();

		Assert.assertTrue(EqualsBuilder.reflectionEquals("mergedcbeff".getBytes(), result));
	}

	@Test
	public void testExtractCbeffSuccess() throws Exception{
		ClassLoader classLoader = getClass().getClassLoader();
		File cbeffFile = new File(classLoader.getResource("TestCbeff.xml").getFile());
		InputStream inputStream = new FileInputStream(cbeffFile);
		String cbeff = IOUtils.toString(inputStream, "UTF-8");

		List<String> types = new ArrayList<>();
		types.add("FACE");

		Mockito.when(cbeffUtil.createXML(Matchers.any())).thenReturn("extractedCbeff".getBytes());

		InputStream stream = util.extractCbeffWithTypes(cbeff,types);
		byte[] result = IOUtils.toString(stream, "UTF-8").getBytes();

		Assert.assertTrue(EqualsBuilder.reflectionEquals("extractedCbeff".getBytes(), result));
	}

	@Test(expected = BiometricTagMatchException.class)
	public void testMergeCbefffException() throws Exception{
		ClassLoader classLoader = getClass().getClassLoader();
		File cbeffFile1 = new File(classLoader.getResource("cbeff1.xml").getFile());
		InputStream inputStream1 = new FileInputStream(cbeffFile1);
		String cbeff1 = IOUtils.toString(inputStream1, "UTF-8");

		File cbeffFile2 = new File(classLoader.getResource("cbeff2.xml").getFile());
		InputStream inputStream2 = new FileInputStream(cbeffFile2);
		String cbeff2 = IOUtils.toString(inputStream2, "UTF-8");

		Mockito.when(cbeffUtil.createXML(Matchers.any())).thenReturn("mergedcbeff".getBytes());

		util.mergeCbeff(cbeff1, cbeff2);
	}
}
