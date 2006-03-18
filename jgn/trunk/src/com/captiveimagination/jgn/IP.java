package com.captiveimagination.jgn;

import java.net.*;

/**
 * @author Matthew D. Hicks
 *
 */
public class IP {
	private byte[] ip;
	
	public IP(byte[] ip) {
		this.ip = ip;
	}
	
	public byte[] getBytes() {
		return ip;
	}
	
	public int[] getInts() {
		int[] ints = new int[ip.length];
		for (int i = 0; i < ip.length; i++) {
			if (ip[i] < 0) {
				ints[i] = ip[i] + 256;
			} else {
				ints[i] = ip[i];
			}
		}
		return ints;
	}
	
	public String toString() {
		int[] ints = getInts();
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < ints.length; i++) {
			if (i > 0) {
				buffer.append(".");
			}
			buffer.append(ints[i]);
		}
		return buffer.toString();
	}
	
	public static final IP fromInetAddress(InetAddress address) {
		return new IP(address.getAddress());
	}
	
	public static final IP fromString(String address) {
		String[] split = address.split("\\.");
		byte[] bytes = new byte[split.length];
		for (int i = 0; i < split.length; i++) {
			int t = Integer.parseInt(split[i]);
			if (t >= 128) {
				bytes[i] = (byte)(t - 256);
			} else {
				bytes[i] = (byte)t;
			}
		}
		return new IP(bytes);
	}
	
	public static final IP fromName(String name) throws UnknownHostException {
		return fromInetAddress(InetAddress.getByName(name));
	}
	
	public static final IP getLocalHost() throws UnknownHostException {
		return fromInetAddress(InetAddress.getLocalHost());
	}
}
