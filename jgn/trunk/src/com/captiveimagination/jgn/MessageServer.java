package com.captiveimagination.jgn;

import java.io.*;
import java.util.*;

import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;

public abstract class MessageServer {
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
	
	public void update() {
		// Process inbound messages and enqueue them
		updateIncoming();
		
		updateEvents();
	}
	
	public void updateIncoming() {
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
	
	public void updateEvents() {
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
	
	protected abstract void closeChannel();
}
