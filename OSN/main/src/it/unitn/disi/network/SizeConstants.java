package it.unitn.disi.network;

public class SizeConstants {

	// 8 bytes for sequence numbers (even if in simulations we use 4).
	public static final int SEQNUMBER_SIZE = 8;

	// 4 bytes for an IPV4 address.
	public static final int IPV4_ADDRESS_SIZE = 4;
	
	// 20 bytes for an IPV4 header.
	public static final int IPV4_HEADER = 20;
	
	// 20 bytes for a TCP header.
	public static final int TCP_HEADER = 20;

	// 8 for a social network ID.
	public static final int SNID_SIZE = 8;

}
