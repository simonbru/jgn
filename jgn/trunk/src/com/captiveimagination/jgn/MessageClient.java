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

import com.captiveimagination.jgn.event.MessageListener;
import com.captiveimagination.jgn.message.DisconnectMessage;
import com.captiveimagination.jgn.message.LocalRegistrationMessage;
import com.captiveimagination.jgn.message.Message;
import com.captiveimagination.jgn.message.Receipt;
import com.captiveimagination.jgn.message.type.UniqueMessage;
import com.captiveimagination.jgn.queue.BasicMessageQueue;
import com.captiveimagination.jgn.queue.MessageQueue;
import com.captiveimagination.jgn.queue.MultiMessageQueue;
import com.captiveimagination.jgn.stream.JGNInputStream;
import com.captiveimagination.jgn.stream.JGNOutputStream;
import com.captiveimagination.jgn.stream.StreamInUseException;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * MessageClient defines the communication layer  between the local machine and the remote
 * machine.
 * <p/>
 * it does that, by providing datastructures for Messageflows, state information about the
 * current connection, and storage places for serialization processes
 *
 * @author Matthew D. Hicks
 * @author Alfons Seul
 */
public final class MessageClient implements MessageSender {

  // preliminary !!!! change to logging
  private static int debuglevel = -1; // change this to get debug messages to system.out
                                      // off=-1, error=0, debug=1, info=2, fine=4, finer=5, finest=6
	/**
	 * the connection status of this client (positions in the lifecycle)
	 */
	public static enum Status {	NOT_CONNECTED, NEGOTIATING, CONNECTED, DISCONNECTING,	DISCONNECTED }

	/**
	 * when a client is status DISCONNECTED, this enum describes what event led to that
	 */
	public enum CloseReason {
		ErrChannelRead ("channel had read error",false),
		ErrChannelWrite("channel had write error",false),
		ErrChannelClosed("channel was closed on remote",false),
		ErrMessageWrong("received unknown message", false),
		ClosedByUser("gracefully closed by user",true),
		ClosedByRemote("remote host sent disconnect",true),
    TimeoutConnect("timeout while connecting",true),
		TimeoutDisconnect("timeout while disconnecting",true), // ???
		TimeoutRead("no receive within allotted time", true),
		KickedFromRemote("remote host kicked me",true),
		Filtered("connection filtered",true),
		NA("doesn't apply", true),
		Ignore("",true); // this state is used for letting the old state persist. see disconnectInternal

		private String desc;
		private boolean graceful;

		CloseReason(String description, boolean graceful) {
			desc = description;
			this.graceful = graceful;
		}
		public String toString()    { return desc; }
		public boolean isGraceful() { return graceful; }
	}

  private long id;
	private SocketAddress address;
	private MessageServer server;
	private Status status;
	private CloseReason closeReason;
	private MessageQueue outgoingQueue;					// Waiting to be sent (eg written to socket) via updateTraffic()
	private MessageQueue incomingMessages;			// Waiting for MessageListener handling
	private MessageQueue outgoingMessages;			// Waiting for MessageListener handling
	private final BasicMessageQueue certifiableMessages;	// Waiting for a Receipt message to certify the message was received
	private MessageQueue certifiedMessages;			// Waiting for MessageListener handling
	private MessageQueue failedMessages;				// Waiting for MessageListener handling
	private final ArrayList<MessageListener> messageListeners;
	private HashMap<Short, JGNInputStream> inputStreams;
	private HashMap<Short, JGNOutputStream> outputStreams;
	private CombinedPacket currentWrite;
	private boolean sentRegistration;				// Refers to if registration message has been sent
	private long lastReceived;							// last time I got a message
	private long lastSent;									// last time I published a message

	private int readPosition;
	private ByteBuffer readBuffer;
	private Message overhangMessage;				// holds overflow from PacketCombiner
	private String kickReason;

	private volatile long receivedCount;		// statistics only
	private volatile long sentCount;				// same

  private HashMap<Short, Class<? extends Message>> registry;
	private HashMap<Class<? extends Message>, Short> registryReverse;

	public MessageClient(SocketAddress address, MessageServer server) {
    if (debuglevel >= 1) // off=-1, error=0, debug=1, info=2, fine=4, finer=5, finest=6
      System.out.println("   --- create MC("+server.getMessageServerId()+") for "+address);
		this.address = address;
		this.server = server;
		status = Status.NOT_CONNECTED;
    closeReason = CloseReason.NA;

		outgoingQueue = new MultiMessageQueue(server.getMaxQueueSize());
		incomingMessages = new MultiMessageQueue(-1);
		outgoingMessages = new MultiMessageQueue(-1);
		certifiableMessages = new BasicMessageQueue();
		certifiedMessages = new MultiMessageQueue(-1);
		failedMessages = new MultiMessageQueue(-1);
		messageListeners = new ArrayList<MessageListener>();
		inputStreams = new HashMap<Short, JGNInputStream>();
		outputStreams = new HashMap<Short, JGNOutputStream>();

		readPosition = 0;
		readBuffer = ByteBuffer.allocateDirect(1024 * 10);

		registry = new HashMap<Short, Class<? extends Message>>();
		registryReverse = new HashMap<Class<? extends Message>, Short>();

		received();	// initialize watch-clocks, to be used by my MessageServer
		sent();			// same.
	}

	//**************************** GETTER/SETTER *****************************/

	public void setId(long id) {this.id = id;}
	public long getId() {return id;}

  public Status getStatus() {return status;}
	public void setStatus(Status newState) {
    if (debuglevel >= 1) // off=-1, error=0, debug=1, info=2, fine=4, finer=5, finest=6
      System.out.println("   --- MC("+server.getMessageServerId()+") "+address+" state: "+status+" --> "+newState );
    status = newState;
    if (debuglevel >= 4) // off=-1, error=0, debug=1, info=2, fine=4, finer=5, finest=6
    if (status == Status.DISCONNECTING) {
      new RuntimeException("Disconnecting ...").printStackTrace();
    }
  }
	public CloseReason getCloseReason() { return closeReason; }
	public void setCloseReason(CloseReason cs) { closeReason = cs; }

	public SocketAddress getAddress() {return address;}

	public MessageServer getMessageServer() {return server;}

	public void received() {lastReceived = System.currentTimeMillis();}
	public long lastReceived() {return lastReceived;}
	public void sent() {lastSent = System.currentTimeMillis();}
	public long lastSent() {return lastSent;}

	public long getReceivedCount() {return receivedCount;}
	public long getSentCount() {return sentCount;}

	protected ByteBuffer getReadBuffer() { return readBuffer;}
	protected int getReadPosition() {return readPosition;	}
	protected void setReadPosition(int readPos) {readPosition = readPos;}

	protected CombinedPacket getCurrentWrite() {return currentWrite;}
	protected void setCurrentWrite(CombinedPacket curWrite) {currentWrite = curWrite; }
	protected Message getOverhangMessage() {return overhangMessage;}
	protected void setOverhangMessage(Message overhangMsg) {overhangMessage = overhangMsg;}

	//true if the negotiating phase has taken place just after sending LocalRegistrationMessage
	protected boolean hasSentRegistration() {	return sentRegistration;}

	protected void setKickReason(String reason) { this.kickReason = reason;}
	protected String getKickReason() {return kickReason;}


	//************************ MESSAGEQUEUES *****************************/

	/**
	 * Represents the MessageQueue containing all messages that
	 * need to be sent to this client.
	 *
	 * @return MessageQueue instance of outgoingQueue
	 */
	public MessageQueue getOutgoingQueue() {
		return outgoingQueue;
	}

	/**
	 * Represents the list of messages that have been received and
	 * are waiting for the listeners to be invoked with them.
	 *
	 * @return MessageQueue
	 */
	public MessageQueue getIncomingMessageQueue() {
		return incomingMessages;
	}

	/**
	 * Represents the list of message that have been sent but are
	 * still waiting for the listeners to be invoked with them.
	 *
	 * @return MessageQueue
	 */
	public MessageQueue getOutgoingMessageQueue() {
		return outgoingMessages;
	}

	/**
	 * Represents the list of CertifiedMessages that have been sent
	 * but are waiting for validation from the remote server that the
	 * message was successfully received.
	 *
	 * @return MessageQueue
	 */
	public BasicMessageQueue getCertifiableMessageQueue() {
		return certifiableMessages;
	}

	/**
	 * Represents the list of CertifiedMessages that have been certified
	 * and are waiting to send events to the message listeners regarding
	 * the certification.
	 *
	 * @return MessageQueue
	 */
	public MessageQueue getCertifiedMessageQueue() {
		return certifiedMessages;
	}

	/**
	 * Represents the list of CertifiedMessages that have failed certification
	 * and are waiting to send events to the message listeners regarding
	 * the failure.
	 *
	 * @return MessageQueue
	 */
	public MessageQueue getFailedMessageQueue() {
		return failedMessages;
	}

	//****************************** STREAMS *************************/

	public JGNInputStream getInputStream() throws IOException {
		return getInputStream((short) 0);
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
		return getOutputStream((short) 0);
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

	//****************************** NOTIFICATION *****************************/

	/**
	 * adds a new MessageListener, that gets notified, when there is a MessageListener.MESSAGE_EVENT
	 * this only refers to messages to/from THIS client.
	 *
	 * @param listener
	 * @see MessageListener
	 */
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

	//************************* LOCAL REGISTRY ***********************/

	/**
	 * if the local registry had the messageclass registered, return it's id.
	 * else ask JGN's registry, but then accept only negative values, meaning system ids
	 * <p/>
	 * Note, we store the messagetype numbers from my partner at the other side of the wire!
	 *
	 * @param c the class to be looked up
	 * @return short - the id
	 * @throws MessageHandlingException - when Messageclass was not registered before
	 */
	public short getMessageTypeId(Class<? extends Message> c) throws MessageHandlingException {
		if (!registryReverse.containsKey(c)) {
			short id = JGN.getMessageTypeId(c);
			if (id < 0) return id;		// if it's a system id we return the internal value
			throw new MessageHandlingException("The Message " + c.getName() + " is not registered, make sure to register with JGN.register() before using.");
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

	//**************************** CONNECTION RELATED ************************/

	public boolean isConnected() {
		return status == Status.CONNECTED;
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

	/**
	 * breaks the connection to the remote by sending it a Disconnect message
	 * thereafter this MessageClient will not any more process messages.
	 */
	public void disconnect() {
    if (status==Status.CONNECTED || status==Status.NEGOTIATING) {
      getMessageServer().getConnectionController().disconnect(this);
      setStatus(Status.DISCONNECTING);
      if (closeReason==CloseReason.NA)
        setCloseReason(CloseReason.ClosedByUser);
    }
  }

	public void disconnectAndWait(long timeout) throws IOException, InterruptedException {
		disconnect();
		long time = System.currentTimeMillis();
		while (System.currentTimeMillis() <= time + timeout) {
			if (status == Status.DISCONNECTED) return;
			Thread.sleep(10);
		}
		throw new IOException("MessageClient did not disconnect within the allotted time (" + status.toString() + ").");
	}

	/**
	 * Connection will be terminated, because it was 'filtered'. Whenever in the
	 * Negotiating phase, the LocalRegistrationMessage arrives, The InternalListener of MessageServer
	 * will check if filters apply. If true, IL will call this method.
	 * This method will delegate to the ConnectionController associated with this MessageClient's
	 * MessageServer. CC will in turn send a DisconnectMessage(reason) to the remote side
	 * @param reason
	 */
	public void kick(String reason) {
		getMessageServer().getConnectionController().kick(this, reason);
		setStatus(Status.DISCONNECTING);
		setCloseReason(CloseReason.Filtered);
	}

	//************************* MESSAGE HANDLING *********************/

	/**
	 * Sends a message to the remote machine that this connection is associated to.
	 * The Message is submitted to the outgoing queue and processed from the associated
	 * MessageServer's updateTraffic method.
	 * <p/>
	 * Note that the message sent here is cloned and is utilized instead of the actual
	 * message received. This allows for re-use  of objects when sending without any problems
	 * attempting to send.
	 *
	 * @param message
	 * @throws ConnectionException when trying to send a message when connection is Status=DISCONNECTED or
	 *         except for DisconnectMessage and Receipt, when Status=DISCONNECTING
	 *
	 *         QueueFullException if output queue filled up. (May work later on)
	 */
	public void sendMessage(Message message) throws ConnectionException {
		Message m = null;

    if (getStatus() == Status.DISCONNECTED) {
      throw new ConnectionException("Connection is closed, no more messages being accepted.");
    } else if (message instanceof DisconnectMessage) {
      // This is a possible occurrence under valid circumstances
    } else if (message instanceof Receipt) {
      // If it hasn't finished disconnecting we can try to send a receipt
    } else if (getStatus() == Status.DISCONNECTING) {
      throw new ConnectionException("Connection is closing, no more messages being accepted.");
    }

		try { m = message.clone(); }
		catch (CloneNotSupportedException cnse) {// cannot happen, since Message is cloneable}
		}
		assert m != null; // just to make javac happy

		m.setMessageClient(this);

		// Make sure we know if we've already sent a registration message
		if (m instanceof LocalRegistrationMessage) {
			sentRegistration = true;
		}
//		if (m instanceof IdentityMessage) {
//			// Ignore setting an id
//		} else

		// Assign unique id if this is a UniqueMessage
    if (m instanceof UniqueMessage) {
			m.setId(Message.nextUniqueId());
		}

		outgoingQueue.add(m);
		sentCount++;
	}

	/**
	 * Used as a mechanism for receiving messages from remote.
	 * an incoming message is put to the IncomingMessageQueue, uhm, aha
	 * for later processing by my MessageServer
	 *
	 * @param message
	 */
	protected void receiveMessage(Message	message) {
		getIncomingMessageQueue().add(message);
		receivedCount++;
	}

	// called only from InternalListener.messageReceived();
	// messageId is the certifiedId of the received Receipt
	protected void certifyMessage(long messageId) {
		Message firstMessage = certifiableMessages.poll();
		if (firstMessage == null) {
//			System.out.println("It should have a message!");
			return;
		}
		if (firstMessage.getId() == messageId) {
			certifiedMessages.add(firstMessage);
			return;
		}
		certifiableMessages.add(firstMessage); // restore
		Message m;
		while ((m = certifiableMessages.poll()) != firstMessage) {
			if (m.getId() == messageId) {
				certifiedMessages.add(m);
				return;
			}
			certifiableMessages.add(m);
		}
		certifiableMessages.add(m);
	}

}