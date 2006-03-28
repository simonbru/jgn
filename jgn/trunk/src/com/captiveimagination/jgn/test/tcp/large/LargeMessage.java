/**
 * 
 */
package com.captiveimagination.jgn.test.tcp.large;

import com.captiveimagination.jgn.message.Message;

/**
 * @author Matthew D. Hicks
 */
public class LargeMessage extends Message {
	private byte[] bytes;
	
	public byte[] getMessageArray() {
		return bytes;
	}
	
	public void setMessageArray(byte[] bytes) {
		this.bytes = bytes;
	}
}
