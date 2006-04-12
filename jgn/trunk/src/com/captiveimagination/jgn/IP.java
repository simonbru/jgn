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
