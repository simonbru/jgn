package com.captiveimagination.jgn.test.udp.arrays;

import com.captiveimagination.jgn.message.*;

/**
 * @author Matthew D. Hicks
 *
 */
public class ByteArrayMessage extends OrderedMessage {
	private byte[] bytes;
	
	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}
	
	public byte[] getBytes() {
		return bytes;
	}
	
	public long getResendTimeout() {
		return 1000;
	}
}
