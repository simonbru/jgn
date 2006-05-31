/**
 * JGNInputStream.java
 *
 * Created: May 28, 2006
 */
package com.captiveimagination.jgn.stream;

import java.io.*;
import java.util.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.message.stream.*;

/**
 * JGNInputStream is a representation of a InputStream inside a MessageServer
 * that can represents bytes streamed from another MessageServer.
 * 
 * @author Matthew D. Hicks
 */
public class JGNInputStream extends InputStream implements MessageListener {
	private MessageServer server;
	private IP remoteIp;
	private int remotePort;
	private int streamId;
	
	private List cache;
	private List cacheLengths;
	private int position;
	private boolean endOfStream;
	
	public JGNInputStream(MessageServer server, IP remoteIp, int remotePort, int streamId) {
		this.server = server;
		this.remoteIp = remoteIp;
		this.remotePort = remotePort;
		this.streamId = streamId;
		
		cache = Collections.synchronizedList(new ArrayList());
		cacheLengths = Collections.synchronizedList(new ArrayList());
		position = 0;
		endOfStream = false;
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
	 * Will always return -1 unless there is a message to stream back
	 */
	public int read() throws IOException {
		try {
			while (true) {
				if (cache.size() > 0) {
					byte[] buf = (byte[])cache.get(0);
					int b = buf[position++];
					if (position >= ((Integer)cacheLengths.get(0)).intValue()) {
						position = 0;
						cache.remove(0);
						cacheLengths.remove(0);
					}
					return b & 0xff;
				} else if (endOfStream) {
					break;
				}
				Thread.sleep(5);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return -1;
	}

	public void messageReceived(Message message) {
	}
	
	public void messageReceived(StreamMessage message) {
		if ((message.getRemoteAddress().equals(remoteIp)) && ((message.getRemotePort() == remotePort) || (remotePort == -1)) && (message.getStreamId() == streamId)) {
			if (message.getData() == null) {
				endOfStream = true;
			} else {
				cacheLengths.add(new Integer(message.getDataLength()));
				cache.add(message.getData());
			}
		}
	}

	public int getListenerMode() {
		return MessageListener.CLOSEST;
	}
	
	public void close() {
		server.closeInputStream(this);
	}
}
