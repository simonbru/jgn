/**
 * 
 */
package com.captiveimagination.jgn.test.tcp.large;

import com.captiveimagination.jgn.message.Message;

/**
 * @author Matthew D. Hicks
 */
public class LargeMessage extends Message {
	private int count;
	private byte[] bytes;
	
	public int getCount() {
		return count;
	}
	
	public void setCount(int count) {
		this.count = count;
	}
	
	public byte[] getMessageArray() {
		return bytes;
	}
	
	public void setMessageArray(byte[] bytes) {
		this.bytes = bytes;
	}
}
