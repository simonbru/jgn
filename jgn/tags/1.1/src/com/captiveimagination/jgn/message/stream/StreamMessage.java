/**
 * StreamMessage.java
 *
 * Created: May 28, 2006
 */
package com.captiveimagination.jgn.message.stream;

import com.captiveimagination.jgn.message.*;

/**
 * @author Matthew D. Hicks
 */
public class StreamMessage extends OrderedMessage {
	private int streamId;
	private byte[] data;
	private int dataLength;
	
	public void setStreamId(int streamId) {
		this.streamId = streamId;
	}
	
	public int getStreamId() {
		return streamId;
	}
	
	public void setData(byte[] data) {
		this.data = data;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public void setDataLength(int dataLength) {
		this.dataLength = dataLength;
	}
	
	public int getDataLength() {
		return dataLength;
	}
	
	public long getResendTimeout() {
		return 1000;
	}

}
