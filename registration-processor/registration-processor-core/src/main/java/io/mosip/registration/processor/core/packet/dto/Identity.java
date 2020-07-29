package io.mosip.registration.processor.core.packet.dto;

import java.util.List;

/**
 * This contains the attributes which have to be displayed in PacketMetaInfo
 * JSON
 * 
 * @author Balaji Sridharan
 * @since 1.0.0
 */
public class Identity {

	private Biometric biometric;
	private List<BiometricExceptionDTO> exceptionBiometrics;
	private Photograph applicantPhotograph;
	private Photograph exceptionPhotograph;
	private List<Document> documents;
	private List<FieldValue> metaData;
	private List<FieldValue> operationsData;
	private List<FieldValueArray> hashSequence1;
	private List<FieldValueArray> hashSequence2;
	private List<NewRegisteredDevice> capturedRegisteredDevices;
	private List<FieldValue> capturedNonRegisteredDevices;
	private List<FieldValue> checkSum;
	private List<String> uinUpdatedFields;

	/**
	 * @return the biometric
	 */
	public Biometric getBiometric() {
		return biometric;
	}

	/**
	 * @param biometric
	 *            the biometric to set
	 */
	public void setBiometric(Biometric biometric) {
		this.biometric = biometric;
	}

	/**
	 * @return the exceptionBiometrics
	 */
	public List<BiometricExceptionDTO> getExceptionBiometrics() {
		return exceptionBiometrics;
	}

	/**
	 * @param exceptionBiometrics
	 *            the exceptionBiometrics to set
	 */
	public void setExceptionBiometrics(List<BiometricExceptionDTO> exceptionBiometrics) {
		this.exceptionBiometrics = exceptionBiometrics;
	}

	/**
	 * @return the applicantPhotograph
	 */
	public Photograph getApplicantPhotograph() {
		return applicantPhotograph;
	}

	/**
	 * @param applicantPhotograph
	 *            the applicantPhotograph to set
	 */
	public void setApplicantPhotograph(Photograph applicantPhotograph) {
		this.applicantPhotograph = applicantPhotograph;
	}

	/**
	 * @return the exceptionPhotograph
	 */
	public Photograph getExceptionPhotograph() {
		return exceptionPhotograph;
	}

	/**
	 * @param exceptionPhotograph
	 *            the exceptionPhotograph to set
	 */
	public void setExceptionPhotograph(Photograph exceptionPhotograph) {
		this.exceptionPhotograph = exceptionPhotograph;
	}

	/**
	 * @return the documents
	 */
	public List<Document> getDocuments() {
		return documents;
	}

	/**
	 * @param documents
	 *            the documents to set
	 */
	public void setDocuments(List<Document> documents) {
		this.documents = documents;
	}

	/**
	 * @return the metaData
	 */
	public List<FieldValue> getMetaData() {
		return metaData;
	}

	/**
	 * @param metaData
	 *            the metaData to set
	 */
	public void setMetaData(List<FieldValue> metaData) {
		this.metaData = metaData;
	}

	/**
	 * @return the osiData
	 */
	public List<FieldValue> getOperationsData() {
		return operationsData;
	}

	/**
	 * @param osiData
	 *            the osiData to set
	 */
	public void setOsiData(List<FieldValue> operationsData) {
		this.operationsData = operationsData;
	}

	/**
	 * @return the hashSequence
	 */
	public List<FieldValueArray> getHashSequence1() {
		return hashSequence1;
	}

	/**
	 * @param hashSequence
	 *            the hashSequence to set
	 */
	public void setHashSequence(List<FieldValueArray> hashSequence) {
		this.hashSequence1 = hashSequence;
	}

	/**
	 * @return the hashSequence2
	 */
	public List<FieldValueArray> getHashSequence2() {
		return hashSequence2;
	}

	/**
	 * @param hashSequence2
	 *            the hashSequence2 to set
	 */
	public void setHashSequence2(List<FieldValueArray> hashSequence2) {
		this.hashSequence2 = hashSequence2;
	}

	/**
	 * @return the capturedRegisteredDevices
	 */
	public List<NewRegisteredDevice> getCapturedRegisteredDevices() {
		return capturedRegisteredDevices;
	}

	/**
	 * @param capturedRegisteredDevices
	 *            the capturedRegisteredDevices to set
	 */
	public void setCapturedRegisteredDevices(List<NewRegisteredDevice> capturedRegisteredDevices) {
		this.capturedRegisteredDevices = capturedRegisteredDevices;
	}

	/**
	 * @return the capturedNonRegisteredDevices
	 */
	public List<FieldValue> getCapturedNonRegisteredDevices() {
		return capturedNonRegisteredDevices;
	}

	/**
	 * @param capturedNonRegisteredDevices
	 *            the capturedNonRegisteredDevices to set
	 */
	public void setCapturedNonRegisteredDevices(List<FieldValue> capturedNonRegisteredDevices) {
		this.capturedNonRegisteredDevices = capturedNonRegisteredDevices;
	}

	/**
	 * @return the checkSum
	 */
	public List<FieldValue> getCheckSum() {
		return checkSum;
	}

	/**
	 * @param checkSum
	 *            the checkSum to set
	 */
	public void setCheckSum(List<FieldValue> checkSum) {
		this.checkSum = checkSum;
	}

	/**
	 * @return the uinUpdatedFields
	 */
	public List<String> getUinUpdatedFields() {
		return uinUpdatedFields;
	}

	/**
	 * @param uinUpdatedFields
	 *            the uinUpdatedFields to set
	 */
	public void setUinUpdatedFields(List<String> uinUpdatedFields) {
		this.uinUpdatedFields = uinUpdatedFields;
	}

}
