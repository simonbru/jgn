/**
 * JGNOutputStream.java
 *
 * Created: May 28, 2006
 */
package com.captiveimagination.jgn.stream;

import java.io.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.message.stream.*;

/**
 * @author Matthew D. Hicks
 */
public class JGNOutputStream extends OutputStream {
	private MessageServer server;
	private IP remoteIp;
	private int remotePort;
	private int streamId;
	
	private byte[] buffer;
	private int position;
	private StreamMessage message;
	
	public JGNOutputStream(MessageServer server, IP remoteIp, int remotePort, int streamId) {
		this.server = server;
		this.remoteIp = remoteIp;
		this.remotePort = remotePort;
		this.streamId = streamId;
		
		buffer = new byte[512];
		position = 0;
		message = new StreamMessage();
		message.setStreamId(streamId);
	}
	
	public IP getRemoteIp() {
		return remoteIp;
	}
	
	public int getRemotePort() {
		return remotePort;
	}
	
	public int getStreamId() {
		return streamId;
	}
	
	/**
	 * Allows you to modify the buffer size of this output stream.
	 * If there is data in the buffer it will be flushed before the
	 * size is adjusted.
	 * 
	 * @param length
	 * @throws IOException
	 */
	public void setBufferSize(int length) throws IOException {
		flush();
		buffer = new byte[length];
	}
	
	public void write(int b) throws IOException {
		if (position >= buffer.length) {
			flush();
			position = 0;
		}
		buffer[position++] = (byte)b;
	}

	public void flush() throws IOException {
		message.setDataLength(position);
		message.setData(buffer);
		//System.out.println("Sending: " + position);
		server.sendMessage(message, remoteIp, remotePort);
	}
	
	public void close() throws IOException {
		flush();
		message.setData(null);
		server.sendMessage(message, remoteIp, remotePort);
		server.closeOutputStream(this);
	}
}
