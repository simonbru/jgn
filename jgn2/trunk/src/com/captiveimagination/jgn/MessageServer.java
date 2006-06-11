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
 * Created: Jun 5, 2006
 */
package com.captiveimagination.jgn;

import java.io.*;
import java.net.*;
import java.util.*;

import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.queue.*;

/**
 * MessageServer is the abstract foundation from which all sending and receiving
 * of Messages occur.
 * @author Matthew D. Hicks
 */
public abstract class MessageServer {
	private InetSocketAddress address;
	private MessageQueue incomingMessages;			// Waiting for MessageListener handling
	private MessageQueue outgoingMessages;			// Waiting for MessageListener handling
	private ConnectionQueue incomingConnections;	// Waiting for ConnectionListener handling
	private ConnectionQueue outgoingConnections;	// Connection that needs to be established
	private ArrayList<ConnectionListener> connectionListeners;
	private ArrayList<MessageListener> messageListeners;

	public MessageServer(InetSocketAddress address) {
		this.address = address;
		incomingMessages = new MessagePriorityQueue();
		outgoingMessages = new MessagePriorityQueue();
		incomingConnections = new ConnectionQueue();
		outgoingConnections = new ConnectionQueue();
		connectionListeners = new ArrayList<ConnectionListener>();
		messageListeners = new ArrayList<MessageListener>();
		
		addConnectionListener(InternalListener.getInstance());
		addMessageListener(InternalListener.getInstance());
	}
	
	protected MessageQueue getIncomingMessageQueue() {
		return incomingMessages;
	}
	
	protected MessageQueue getOutgoingMessageQueue() {
		return outgoingMessages;
	}
	
	protected ConnectionQueue getIncomingConnectionQueue() {
		return incomingConnections;
	}

	protected ConnectionQueue getOutgoingConnectionQueue() {
		return outgoingConnections;
	}
	
	/**
	 * @return
	 * 		the InetSocketAddress representing the remote host
	 * 		machine
	 */
	public InetSocketAddress getSocketAddress() {
		return address;
	}

	/**
	 * Establishes a connection to the remote host distinguished by
	 * <code>address</code>. This method simply queues the connection
	 * to be established and is handled by the updateTraffic method.
	 * 
	 * @param address
	 * @return
	 * 		MessageClient will only be returned if a connection has
	 * 		already been established to this client, otherwise, it
	 * 		will always return null as this is a non-blocking method.
	 */
	public MessageClient connect(InetSocketAddress address) {
		// TODO check if this address has already been registered
		outgoingConnections.add(new MessageClient(address, this));
		return null;
	}
	
	/**
	 * Closes all open connections to remote clients
	 */
	public abstract void close();
	
	/**
	 * Processing incoming/outgoing traffic for this MessageServer
	 * implementation. Should handle incoming connections, incoming
	 * messages, and outgoing messages.
	 * 
	 * @throws IOException
	 */
	public abstract void updateTraffic() throws IOException;
	
	/**
	 * Handles processing events for incoming/outgoing messages sending to
	 * the registered listeners for both the MessageServer and MessageClients
	 * associated as well as the incoming established connection events.
	 */
	public synchronized void updateEvents() {
		// Process incoming connections first
		while (!incomingConnections.isEmpty()) {
			MessageClient client = incomingConnections.poll();
			synchronized (connectionListeners) {
				for (ConnectionListener listener : connectionListeners) {
					listener.connected(client);
				}
			}
		}
		
		// Process incoming Messages to the listeners
		while (!incomingMessages.isEmpty()) {
			Message message = incomingMessages.poll();
			synchronized (messageListeners) {
				for (MessageListener listener : messageListeners) {
					listener.messageReceived(message);
				}
			}
		}

		// Process outgoing Messages to the listeners
		while (!outgoingMessages.isEmpty()) {
			Message message = outgoingMessages.poll();
			synchronized (messageListeners) {
				for (MessageListener listener : messageListeners) {
					listener.messageSent(message);
				}
			}
		}
	}
	
	/**
	 * Convenience method to call updateTraffic() and updateEvents().
	 * This is only necessary if you aren't explicitly calling these
	 * methods already.
	 * 
	 * @throws IOException
	 */
	public void update() throws IOException {
		updateTraffic();
		updateEvents();
	}
	
	/**
	 * Adds a ConnectionListener to this MessageServer
	 * 
	 * @param listener
	 */
	public void addConnectionListener(ConnectionListener listener) {
		synchronized (connectionListeners) {
			connectionListeners.add(listener);
		}
	}
	
	/**
	 * Removes a ConnectionListener from this MessageServer
	 * 
	 * @param listener
	 * @return
	 * 		true if the listener was contained in the list
	 */
	public boolean removeConnectionListener(ConnectionListener listener) {
		synchronized (connectionListeners) {
			return connectionListeners.remove(listener);
		}
	}
	
	/**
	 * Adds a MessageListener to this MessageServer
	 * 
	 * @param listener
	 */
	public void addMessageListener(MessageListener listener) {
		synchronized (messageListeners) {
			messageListeners.add(listener);
		}
	}
	
	/**
	 * Removes a MessageListener from this MessageServer
	 * 
	 * @param listener
	 * @return
	 * 		true if the listener was contained in the list
	 */
	public boolean removeMessageListener(MessageListener listener) {
		synchronized (messageListeners) {
			return messageListeners.remove(listener);
		}
	}
}
