package io.mosip.registration.packetmanager.spi;

public interface PacketSigner {
	
	public byte[] signZip(byte[] data);

}
