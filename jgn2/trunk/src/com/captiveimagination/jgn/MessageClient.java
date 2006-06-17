/**
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
 *
 * Created: Jun 6, 2006
 */
package com.captiveimagination.jgn;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;

import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.queue.*;

/**
 * MessageClient defines the communication layer
 * between the local machine and the remote
 * machine.
 * 
 * @author Matthew D. Hicks
 */
public class MessageClient {
	public static final int STATUS_NOT_CONNECTED = 1;
	public static final int STATUS_NEGOTIATING = 2;
	public static final int STATUS_CONNECTED = 3;
	public static final int STATUS_DISCONNECTED = 4;
	
	private InetSocketAddress address;
	private MessageServer server;
	private int status;
	private MessageQueue outgoingQueue;				// Waiting to be sent via updateTraffic()
	private MessageQueue incomingMessages;			// Waiting for MessageListener handling
	private MessageQueue outgoingMessages;			// Waiting for MessageListener handling
	private ArrayList<MessageListener> messageListeners;
	//private HashMap<Integer,JGNInputStream> inputStreams;
	//private HashMap<Integer,JGNOutputStream> outputStreams;
	private ByteBuffer currentWrite;
	private Message currentMessage;
	
	private HashMap<Short,Class<? extends Message>> registry;
	private HashMap<Class<? extends Message>,Short> registryReverse;
	
	public MessageClient(InetSocketAddress address, MessageServer server) {
		this.address = address;
		this.server = server;
		status = STATUS_NOT_CONNECTED;
		outgoingQueue = new MessagePriorityQueue();
		incomingMessages = new MessagePriorityQueue(-1);
		outgoingMessages = new MessagePriorityQueue(-1);
		messageListeners = new ArrayList<MessageListener>(server.getMaxQueueSize());
		
		registry = new HashMap<Short,Class<? extends Message>>();
		registryReverse = new HashMap<Class<? extends Message>,Short>();
		register((short)0, LocalRegistrationMessage.class);
	}
	
	public int getStatus() {
		return status;
	}
	
	public void setStatus(int status) {
		this.status = status;
	}
	
	public boolean isConnected() {
		return status == STATUS_CONNECTED;
	}
	
	public ByteBuffer getCurrentWrite() {
		return currentWrite;
	}
	
	public void setCurrentWrite(ByteBuffer currentWrite) {
		this.currentWrite = currentWrite;
	}
	
	public Message getCurrentMessage() {
		return currentMessage;
	}
	
	public void setCurrentMessage(Message currentMessage) {
		this.currentMessage = currentMessage;
	}
	
	public InetSocketAddress getAddress() {
		return address;
	}
	
	public MessageServer getMessageServer() {
		return server;
	}
	
	/**
	 * Sends a message to the remote machine
	 * that this connection is associated to.
	 * The Message is submitted to the outgoing
	 * queue and processed from the associated
	 * MessageServer's updateTraffic method.
	 * 
	 * Note that the message sent here is cloned
	 * and is utilized instead of the actual
	 * message received. This allows for re-use
	 * of objects when sending without any problems
	 * attempting to send.
	 * 
	 * @param message
	 */
	public void sendMessage(Message message) {
		try {
			Message m = message.clone();
			m.setMessageClient(this);
			outgoingQueue.add(m);
		} catch(CloneNotSupportedException exc) {
			// TODO this should never happen, right?
			throw new RuntimeException(exc);
		}
	}
	
	/**
	 * Represents the MessageQueue containing all messages that
	 * need to be sent to this client.
	 * 
	 * @return
	 * 		MessageQueue instance of outgoingQueue
	 */
	public MessageQueue getOutgoingQueue() {
		return outgoingQueue;
	}
	
	/**
	 * Represents the list of messages that have been received and
	 * are waiting for the listeners to be invoked with them.
	 * 
	 * @return
	 * 		MessageQueue
	 */
	public MessageQueue getIncomingMessageQueue() {
		return incomingMessages;
	}
	
	/**
	 * Represents the list of message that have been sent but are
	 * still waiting for the listeners to be invoked with them.
	 * 
	 * @return
	 * 		MessageQueue
	 */
	public MessageQueue getOutgoingMessageQueue() {
		return outgoingMessages;
	}
	
	public void addMessageListener(MessageListener listener) {
		synchronized (messageListeners) {
			messageListeners.add(listener);
		}
	}
	
	public boolean removeMessageListener(MessageListener listener) {
		synchronized (messageListeners) {
			return messageListeners.remove(listener);
		}
	}

	public ArrayList<MessageListener> getMessageListeners() {
		return messageListeners;
	}
	
	public short getMessageTypeId(Class<? extends Message> c) {
		return registryReverse.get(c);
	}
	
	public Class<? extends Message> getMessageClass(short typeId) {
		return registry.get(typeId);
	}
	
	public void register(short typeId, Class<? extends Message> c) {
		registry.put(typeId, c);
		registryReverse.put(c, typeId);
	}

	public void disconnect() throws IOException {
		// TODO disconnect
		setStatus(STATUS_DISCONNECTED);
	}
}
