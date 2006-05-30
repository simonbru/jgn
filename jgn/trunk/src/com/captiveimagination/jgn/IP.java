/*
 * Copyright (c) 2005-2006 JavaGameNetworking
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'JavaGameNetworking' nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.captiveimagination.jgn;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * This class is a &quot;JGN&quot; internal abstraction (representation) of a
 * computers network ip-address.<br>
 * 
 * @author Matthew D. Hicks
 */
public class IP {

	/**
	 * This convenience method creates an ip object of a specified adress
	 * object.<br>
	 * 
	 * @param address
	 *            Java adress representation.
	 * @return &quot;JGN&quot; internal representation.
	 */
	public static final IP fromInetAddress(InetAddress address) {
		return new IP(address.getAddress());
	}

	/**
	 * This convenience method creates an ip object for the specified host name.<br>
	 * 
	 * @param name
	 *            name of the host, of which the ip should be obtained.
	 * @return ip object for the specified host.
	 * @throws UnknownHostException
	 *             If the host cannot be found.
	 */
	public static final IP fromName(String name) throws UnknownHostException {
		return fromInetAddress(InetAddress.getByName(name));
	}

	/**
	 * This method creates an ip object from an adress which is coded in a
	 * {@link String}<br>
	 * Pattern: Something like "192.168.13.22"
	 * 
	 * @param address
	 *            string coded ip address
	 * @return ip object for the specified address.
	 */
	public static final IP fromString(String address) {
		String[] split = address.split("\\.");
		byte[] bytes = new byte[split.length];
		for (int i = 0; i < split.length; i++) {
			int t = Integer.parseInt(split[i]);
			if (t >= 128) {
				bytes[i] = (byte) (t - 256);
			} else {
				bytes[i] = (byte) t;
			}
		}
		return new IP(bytes);
	}

	/**
	 * Convenience method for obtaining an ip object for the local host.<br>
	 * 
	 * @see InetAddress#getLocalHost()
	 * @return ip object for the local host.
	 * @throws UnknownHostException
	 *             Is thrown by {@link InetAddress#getLocalHost()}.
	 */
	public static final IP getLocalHost() throws UnknownHostException {
		return fromInetAddress(InetAddress.getLocalHost());
	}

	/**
	 * The hashcode for this object.<br>
	 * Stored in this field to prevent multiple hashcode calculation.
	 */
	private int hashCode;

	/**
	 * The bytes an ip address is made of.
	 */
	private byte[] ip;

	/**
	 * Creates an instace.<br>
	 * 
	 * @param ipAddress
	 *            binary representation of the ip address.
	 */
	public IP(byte[] ipAddress) {
		this.ip = ipAddress;
		this.hashCode = Arrays.hashCode(ipAddress);
	}

	/**
	 * (overridden)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof IP) {
			IP other = (IP) obj;
			return Arrays.equals(ip, other.ip);
		}
		return false;
	}

	/**
	 * Returns the binary representation of the ip address.
	 * 
	 * @return the binary representation of the ip address.
	 */
	public byte[] getBytes() {
		// Returns a close, so external manipulation is impossible. 
		return (byte[])ip.clone();
	}

	/**
	 * Returns {@link #getBytes()} as an <code>int[]</code>.
	 * 
	 * @return {@link #getBytes()} as an <code>int[]</code>.
	 */
	public int[] getInts() {
		int[] ints = new int[ip.length];
		for (int i = 0; i < ip.length; i++) {
			// create unsigned value
			ints[i] = ip[i] & 0xFF;
		}
		return ints;
	}

	/**
	 * (overridden)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		// See constructor for generation.
		return this.hashCode;
	}

	/**
	 * (overridden)<br>
	 * Constructs something like &quot;192.168.22.13&quot;.s
	 * 
	 * @see java.lang.Object#toString()
	 */
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
}
