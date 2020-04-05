package io.mosip.registration.service.security;

import io.mosip.registration.exception.RegBaseCheckedException;

public interface ClientSecurity {
	
	/**
	 * Signs the input data by private key provided
	 * 
	 * @param dataToSign
	 * @return
	 */
	public byte[] signData(byte[] dataToSign);
	
	
	/**
	 * Validates the signed data against the actual data using the public part of underlying security module
	 *  
	 * @param signature
	 * @param actualData
	 * @param publicPart
	 * @return
	 */
	public boolean validateSignature(byte[] signature, byte[] actualData);
	
	/**
	 * Encrypts the input data
	 *  
	 * @param plainData
	 * @return
	 */
	public byte[] asymmetricEncrypt(byte[] plainData);
	
	/**
	 * Decrypts provided cipher text
	 * 
	 * @param cipher
	 * @return
	 */
	public byte[] asymmetricDecrypt(byte[] cipher);
	
	/**
	 * 
	 * 
	 * @return
	 */
	public byte[] getSigningPublicPart();
	
	/**
	 * Closes underlying security implementation
	 * 
	 * @throws RegBaseCheckedException
	 */
	public void closeSecurityInstance() throws RegBaseCheckedException;

}
