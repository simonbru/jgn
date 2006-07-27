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
import com.captiveimagination.jgn.message.type.*;
import com.captiveimagination.jgn.queue.*;
import com.captiveimagination.jgn.stream.*;

/**
 * MessageClient defines the communication layer
 * between the local machine and the remote
 * machine.
 * 
 * @author Matthew D. Hicks
 */
public class MessageClient {
	public static enum Status {
		NOT_CONNECTED,
		NEGOTIATING,
		CONNECTED,
		DISCONNECTING,
		DISCONNECTED,
		TERMINATED
	}
	
	private long id;
	private SocketAddress address;
	private MessageServer server;
	private Status status;
	private long lastReceived;
	private long lastSent;
	private MessageQueue outgoingQueue;				// Waiting to be sent via updateTraffic()
	private MessageQueue incomingMessages;			// Waiting for MessageListener handling
	private MessageQueue outgoingMessages;			// Waiting for MessageListener handling
	private BasicMessageQueue certifiableMessages;	// Waiting for a Receipt message to certify the message was received
	private MessageQueue certifiedMessages;			// Waiting for MessageListener handling
	private MessageQueue failedMessages;			// Waiting for MessageListener handling
	private ArrayList<MessageListener> messageListeners;
	private HashMap<Short,JGNInputStream> inputStreams;
	private HashMap<Short,JGNOutputStream> outputStreams;
	private CombinedPacket currentWrite;
	
	private int readPosition;
	private ByteBuffer readBuffer;
	private Message failedMessage;
	
	private HashMap<Short,Class<? extends Message>> registry;
	private HashMap<Class<? extends Message>,Short> registryReverse;
	
	public MessageClient(SocketAddress address, MessageServer server) {
		this.address = address;
		this.server = server;
		status = Status.NOT_CONNECTED;
		outgoingQueue = new MultiMessageQueue(server.getMaxQueueSize());
		incomingMessages = new MultiMessageQueue(-1);
		outgoingMessages = new MultiMessageQueue(-1);
		certifiableMessages = new BasicMessageQueue();
		certifiedMessages = new MultiMessageQueue(-1);
		failedMessages = new MultiMessageQueue(-1);
		messageListeners = new ArrayList<MessageListener>();
		inputStreams = new HashMap<Short,JGNInputStream>();
		outputStreams = new HashMap<Short,JGNOutputStream>();
		
		readPosition = 0;
		readBuffer = ByteBuffer.allocateDirect(1024 * 10);
		
		registry = new HashMap<Short,Class<? extends Message>>();
		registryReverse = new HashMap<Class<? extends Message>,Short>();
		register((short)-1, LocalRegistrationMessage.class);
		received();
		sent();
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	public long getId() {
		return id;
	}
	
	public Status getStatus() {
		return status;
	}
	
	public void setStatus(Status status) {
		this.status = status;
	}
	
	public boolean isConnected() {
		return status == Status.CONNECTED;
	}
	
	public CombinedPacket getCurrentWrite() {
		return currentWrite;
	}
	
	public void setCurrentWrite(CombinedPacket currentWrite) {
		this.currentWrite = currentWrite;
	}

	public SocketAddress getAddress() {
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
			// Assign unique id if this is a UniqueMessage
			if (m instanceof IdentityMessage) {
				// Ignore setting an id
			} else if (m instanceof UniqueMessage) {
				m.setId(Message.nextUniqueId());
			}
			if (getStatus() == Status.DISCONNECTING) {
				throw new RuntimeException("Connection is closing, no more messages being accepted.");
			} else if (getStatus() == Status.DISCONNECTED) {
				throw new RuntimeException("Connection is closed, no more messages being accepted.");
			}
			outgoingQueue.add(m);
		} catch(CloneNotSupportedException exc) {
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
	
	/**
	 * Represents the list of CertifiedMessages that have been sent
	 * but are waiting for validation from the remote server that the
	 * message was successfully received.
	 * 
	 * @return
	 * 		MessageQueue
	 */
	public BasicMessageQueue getCertifiableMessageQueue() {
		return certifiableMessages;
	}
	
	/**
	 * Represents the list of CertifiedMessages that have been certified
	 * and are waiting to send events to the message listeners regarding
	 * the certification.
	 * 
	 * @return
	 * 		MessageQueue
	 */
	public MessageQueue getCertifiedMessageQueue() {
		return certifiedMessages;
	}
	
	/**
	 * Represents the list of CertifiedMessages that have failed certification
	 * and are waiting to send events to the message listeners regarding
	 * the failure.
	 * 
	 * @return
	 * 		MessageQueue
	 */
	public MessageQueue getFailedMessageQueue() {
		return failedMessages;
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
	
	public JGNInputStream getInputStream() throws IOException {
		return getInputStream((short)0);
	}
	
	public JGNInputStream getInputStream(short streamId) throws IOException {
		if (inputStreams.containsKey(streamId)) {
			throw new StreamInUseException("The stream " + streamId + " is currently in use and must be closed before another session can be established.");
		}
		JGNInputStream stream = new JGNInputStream(this, streamId);
		inputStreams.put(streamId, stream);
		return stream;
	}
	
	public void closeInputStream(short streamId) throws IOException {
		if (inputStreams.containsKey(streamId)) {
			if (!inputStreams.get(streamId).isClosed()) inputStreams.get(streamId).close();
			inputStreams.remove(streamId);
		}
	}
	
	public JGNOutputStream getOutputStream() throws IOException {
		return getOutputStream((short)0);
	}
	
	public JGNOutputStream getOutputStream(short streamId) throws IOException {
		if (outputStreams.containsKey(streamId)) {
			throw new StreamInUseException("The stream " + streamId + " is currently in use and must be closed before another session can be established.");
		}
		JGNOutputStream stream = new JGNOutputStream(this, streamId);
		outputStreams.put(streamId, stream);
		return stream;
	}
	
	public void closeOutputStream(short streamId) throws IOException {
		if (outputStreams.containsKey(streamId)) {
			if (!outputStreams.get(streamId).isClosed()) outputStreams.get(streamId).close();
			outputStreams.remove(streamId);
		}
	}
	
	public short getMessageTypeId(Class<? extends Message> c) {
		if (!registryReverse.containsKey(c)) {
			short id = JGN.getMessageTypeId(c);
			if (id < 0) return id;		// if it's a system id we return the internal value
			throw new NoClassDefFoundError("The Message " + c.getName() + " is not registered, make sure to register with JGN.register() before using.");
		}
		return registryReverse.get(c);
	}
	
	public Class<? extends Message> getMessageClass(short typeId) {
		return registry.get(typeId);
	}
	
	public void register(short typeId, Class<? extends Message> c) {
		registry.put(typeId, c);
		registryReverse.put(c, typeId);
	}
	
	public void received() {
		lastReceived = System.currentTimeMillis();
	}
	
	public long lastReceived() {
		return lastReceived;
	}
	
	public void sent() {
		lastSent = System.currentTimeMillis();
	}
	
	public long lastSent() {
		return lastSent;
	}
	
	protected void certifyMessage(long messageId) {
		synchronized(certifiableMessages) {
			Message firstMessage = certifiableMessages.poll();
			if (firstMessage == null) {
				System.out.println("It should have a message!");
				return;
			}
			if (firstMessage.getId() == messageId) {
				certifiedMessages.add(firstMessage);
				return;
			}
			certifiableMessages.add(firstMessage);
			Message m = null;
			while ((m = certifiableMessages.poll()) != firstMessage) {
				if (m.getId() == messageId) {
					certifiedMessages.add(m);
					return;
				}
				certifiableMessages.add(m);
			}
			if (m != null) certifiableMessages.add(m);
		}
	}
	
	public void disconnect() throws IOException {
		getMessageServer().getConnectionController().disconnect(this);
		setStatus(Status.DISCONNECTING);
	}

	protected ByteBuffer getReadBuffer() {
		return readBuffer;
	}
	
	protected int getReadPosition() {
		return readPosition;
	}
	
	protected void setReadPosition(int readPosition) {
		this.readPosition = readPosition;
	}

	protected Message getFailedMessage() {
		return failedMessage;
	}
	
	protected void setFailedMessage(Message failedMessage) {
		this.failedMessage = failedMessage;
	}

	protected boolean isDisconnectable() {
		if (status == Status.DISCONNECTING) {
			if (!outgoingQueue.isEmpty()) return false;
			if (!incomingMessages.isEmpty()) return false;
			if (!outgoingMessages.isEmpty()) return false;
			if (!certifiableMessages.isEmpty()) return false;
			if (!certifiedMessages.isEmpty()) return false;
			if (!failedMessages.isEmpty()) return false;
			return true;
		}
		return false;
	}
}