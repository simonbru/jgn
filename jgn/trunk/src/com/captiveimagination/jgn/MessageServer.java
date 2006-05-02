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

import java.io.*;
import java.util.*;

import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;

public abstract class MessageServer {
	public static final int UDP = 1;
	public static final int TCP = 2;
	
	protected IP address;
	protected int port;
	private MessageQueue queue;
	private boolean keepAlive;
	
	private HashMap timeConversion;
	
	private InternalMessageListener internalMessageListener;
	
	public MessageServer(IP address, int port) {
		this.address = address;
		this.port = port;
		queue = new MessageQueue(this);
		
		keepAlive = true;
		
		timeConversion = new HashMap();

    	// Add Receipt Listener to certify messages
        internalMessageListener = new InternalMessageListener(this);
        addMessageListener(internalMessageListener);
	}
	
	/**
	 * Convenience method primarily for testing to call the update() method of this
	 * MessageServer in another thread.
	 */
	public void startUpdateThread() {
		Thread t = new Thread() {
			public void run() {
				while (keepAlive) {
					try {
						Thread.sleep(10);
					} catch(InterruptedException exc) {
						exc.printStackTrace();
					}
					update();
				}
			}
		};
		t.start();
	}
	
	/**
	 * @return
	 * 		Responsible for receiving incoming messages. May return null if
	 * 		no message is waiting or an error occurred.
	 */
	protected abstract Message receiveMessage() throws IOException;
	
	/**
	 * Method responsible for sending a message to a remote machine.
	 * 
	 * @param message
	 * @param remoteAddress
	 * @param remotePort
	 */
	public abstract void sendMessage(Message message, IP remoteAddress, int remotePort) throws IOException;
	
	public synchronized void update() {
		// Process inbound messages and enqueue them
		updateIncoming();
		
		updateEvents();
	}
	
	public synchronized void updateIncoming() {
		try {
			Message message;
			while ((message = receiveMessage()) != null) {
				queue.enqueue(message);
			}
        } catch(IOException exc) {
            exc.printStackTrace();
            shutdown();
        }
	}
	
	public synchronized void updateEvents() {
		queue.update();
	}
	
	/**
     * @return
     *      The local address listening for messages on.
     */
	public IP getAddress() {
		return address;
	}
	
	/**
     * @return
     *      The local port listening for messages on.
     */
	public int getPort() {
		return port;
	}
	
	/**
     * @return
     *      The Messagequeue responsible for handling enqueued messages
     *      that have been sent or received.
     */
	public MessageQueue getMessageQueue() {
		return queue;
	}
	
	/**
     * This method adds a MessageListener for any incoming
     * messages.
     * 
     * @param listener
     */
	public void addMessageListener(MessageListener listener) {
		queue.addMessageListener(listener);
	}
    
    /**
     * This method removes a MessageListener from receiving any
     * further events.
     * 
     * @param listener
     */
    public void removeMessageListener(MessageListener listener) {
        queue.removeMessageListener(listener);
    }
	
	/**
     * This method adds a MessageSentListener for any outgoing
     * messages.
     * 
     * @param listener
     */
	public void addMessageSentListener(MessageSentListener listener) {
		queue.addMessageSentListener(listener);
	}
	
    /**
     * This method removes a MessageSentListener from receiving any
     * further events.
     * 
     * @param listener
     */
    public void removeMessageSentListener(MessageSentListener listener) {
        queue.removeMessageSentListener(listener);
    }
    
	/**
	 * Called when a message is sent from this MessageServer. The sendMessage() method
	 * should call this after a message has been successfully sent.
	 * 
	 * @param message
	 */
	public void messageSent(Message message) {
		queue.enqueueSent(message);
	}
	
	public void timeSync(IP remoteAddress, int remotePort) {
		internalMessageListener.timeSync(remoteAddress, remotePort);
	}
	
	public void setTimeConversion(IP remoteAddress, int remotePort, long conversion) {
		String key = remoteAddress.toString() + ":" + remotePort;
		timeConversion.put(key, new Long(conversion));
	}
	
	public long getConvertedTime(IP remoteAddress, int remotePort) {
		long time = System.currentTimeMillis();
		if (remoteAddress != null) {
            String key = remoteAddress.toString() + ":" + remotePort;
            if (timeConversion.containsKey(key)) {
                long conversion = ((Long)timeConversion.get(key)).longValue();
                time += conversion;
            }
        }
		return time;
	}
	
	/**
     * @param remoteAddress
     * @param remotePort
     * @param timeout
     * @return
     * 		The time in seconds the ping took to return.
     */
	public float ping(IP remoteAddress, int remotePort, long timeout) {
    	return internalMessageListener.pingAndWait(remoteAddress, remotePort, timeout) / 1000000000.0f;
    }
    
    /**
     * Sends this certified message to the remote server and waits
     * for delivery confirmation.
     * 
     * @param message
     * @param remoteAddress
     * @param remotePort
     * @param timeout
     * @return
     * 		true only if it was certified within the timeout
     * 		specified
     */
    public boolean sendCertified(CertifiedMessage message, IP remoteAddress, int remotePort, long timeout) {
    	return internalMessageListener.sendCertified(message, remoteAddress, remotePort, timeout);
    }
	
	/**
	 * Tells this MessageServer to shut down. Will wait until all messages have been sent.
	 */
	public void shutdown() {
		keepAlive = false;
		closeChannel();
	}
	
	public boolean isKeepAlive() {
		return keepAlive;
	}
	
	protected abstract void closeChannel();
}
