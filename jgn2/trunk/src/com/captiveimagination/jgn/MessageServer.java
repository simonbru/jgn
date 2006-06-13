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
import java.lang.reflect.*;
import java.net.*;
import java.nio.*;
import java.util.*;

import com.captiveimagination.jgn.convert.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.queue.*;

/**
 * MessageServer is the abstract foundation from which all sending and receiving
 * of Messages occur.
 * @author Matthew D. Hicks
 */
public abstract class MessageServer {
	private static HashMap<Class,ArrayList<Class>> classHierarchyCache = new HashMap<Class,ArrayList<Class>>();
	
	private InetSocketAddress address;
	private ConnectionQueue incomingConnections;	// Waiting for ConnectionListener handling
	private ConnectionQueue negotiatedConnections;	// Waiting for ConnectionListener handling
	private ArrayList<ConnectionListener> connectionListeners;
	private ArrayList<MessageListener> messageListeners;
	private List<MessageClient> clients;

	public MessageServer(InetSocketAddress address) {
		this.address = address;
		incomingConnections = new ConnectionQueue();
		negotiatedConnections = new ConnectionQueue();
		connectionListeners = new ArrayList<ConnectionListener>();
		messageListeners = new ArrayList<MessageListener>();
		clients = Collections.synchronizedList(new LinkedList<MessageClient>());
		
		addConnectionListener(InternalListener.getInstance());
		addMessageListener(InternalListener.getInstance());
	}
	
	protected ConnectionQueue getIncomingConnectionQueue() {
		return incomingConnections;
	}
	
	protected ConnectionQueue getNegotiatedConnectionQueue() {
		return negotiatedConnections;
	}
	
	protected List<MessageClient> getMessageClients() {
		return clients;
	}
	
	/**
	 * @return
	 * 		the InetSocketAddress representing the remote host
	 * 		machine
	 */
	public InetSocketAddress getSocketAddress() {
		return address;
	}

	public MessageClient getMessageClient(InetSocketAddress address) {
		for (MessageClient client : getMessageClients()) {
			if (client.getAddress().equals(address)) return client;
		}
		return null;
	}
	
	/**
	 * Establishes a connection to the remote host distinguished by
	 * <code>address</code>. This method is non-blocking.
	 * 
	 * @param address
	 * @return
	 * 		MessageClient will only be returned if a connection has
	 * 		already been established to this client, otherwise, it
	 * 		will always return null as this is a non-blocking method.
	 * @throws IOException
	 */
	public abstract MessageClient connect(InetSocketAddress address) throws IOException;
	
	/**
	 * Exactly the same as connect, but blocks for <code>timeout</code> in milliseconds
	 * for the connection to be established and returned. If the connection is already
	 * established it will be immediately returned.
	 * 
	 * <b>WARNING</b>: Do not execute this method in the same thread as is doing the
	 * update calls or you will end up with a timeout every time.
	 * 
	 * @param address
	 * @param timeout
	 * @return
	 * 		MessageClient for the connection that is established.
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public MessageClient connectAndWait(InetSocketAddress address, int timeout) throws IOException, InterruptedException {
		MessageClient client = connect(address);
		if (client != null) {
			return client;
		}
		client = getMessageClient(address);
		long time = System.currentTimeMillis();
		while (System.currentTimeMillis() < time + timeout) {
			if (client.isConnected()) {
				return client;
			}
			Thread.sleep(10);
		}
		// Last attempt before failing
		if (client.isConnected()) return client;
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
		for (MessageClient client : clients) {
			MessageQueue incomingMessages = client.getIncomingMessageQueue();
			while (!incomingMessages.isEmpty()) {
				Message message = incomingMessages.poll();
				synchronized (client.getMessageListeners()) {
					for (MessageListener listener : client.getMessageListeners()) {
						//listener.messageReceived(message);
						sendToListener(message, listener, true);
					}
				}
				synchronized (messageListeners) {
					for (MessageListener listener : messageListeners) {
						//listener.messageReceived(message);
						sendToListener(message, listener, true);
					}
				}
			}
		}

		// Process outgoing Messages to the listeners
		for (MessageClient client : clients) {
			MessageQueue outgoingMessages = client.getOutgoingMessageQueue();
			while (!outgoingMessages.isEmpty()) {
				Message message = outgoingMessages.poll();
				synchronized (client.getMessageListeners()) {
					for (MessageListener listener : client.getMessageListeners()) {
						//listener.messageReceived(message);'
						sendToListener(message, listener, false);
					}
				}
				synchronized (messageListeners) {
					for (MessageListener listener : messageListeners) {
						listener.messageSent(message);
						sendToListener(message, listener, false);
					}
				}
			}
		}

		// Process incoming negiated connections
		while (!negotiatedConnections.isEmpty()) {
			MessageClient client = negotiatedConnections.poll();
			synchronized (connectionListeners) {
				for (ConnectionListener listener : connectionListeners) {
					listener.negotiationComplete(client);
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

	protected ByteBuffer convertMessage(Message message, ByteBuffer buffer) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, IOException {
		ConversionHandler handler = JGN.getConverter(message.getClass());
		handler.sendMessage(message, buffer);
		// TODO pass the byte buffer through any MessageProcessors associated with this message
		return buffer;
	}
	
	private static final void sendToListener(Message message, MessageListener listener, boolean received) {
		if (received) {
			if (listener instanceof DynamicMessageListener) {
				callMethod(listener, "messageReceived", message, false);
			} else {
				listener.messageReceived(message);
			}
		} else {
			if (listener instanceof DynamicMessageListener) {
				callMethod(listener, "messageSent", message, false);
			} else {
				listener.messageSent(message);
			}
		}
	}
	
	private static void callMethod(MessageListener listener, String methodName, Message message, boolean callAll) {
        try {
            Method[] allMethods = listener.getClass().getMethods();
            ArrayList<Method> m = new ArrayList<Method>();
            for (int i = 0; i < allMethods.length; i++) {
                if ((allMethods[i].getName().equals(methodName)) && (allMethods[i].getParameterTypes().length == 1)) {
                    m.add(allMethods[i]);
                }
            }
            
            // Check to see if an interface is found first
            ArrayList classes = getClassList(message.getClass());
            for (int j = 0; j < classes.size(); j++) {
            	for (int i = 0; i < m.size(); i++) {
                    if (((Method)m.get(i)).getParameterTypes()[0] == classes.get(j)) {
                        ((Method)m.get(i)).setAccessible(true);
                        ((Method)m.get(i)).invoke(listener, new Object[] {message});
                        if (!callAll) return;
                    }
                }
            }
        } catch(Throwable t) {
            System.err.println("Object: " + listener + ", MethodName: " + methodName + ", Var: " + message + ", callAll: " + callAll);
            t.printStackTrace();
        }
    }
	
	private static ArrayList getClassList(Class c) {
    	if (classHierarchyCache.containsKey(c)) {
    		return (ArrayList)classHierarchyCache.get(c);
    	}
    	
    	ArrayList<Class> list = new ArrayList<Class>();
    	Class[] interfaces;
    	do {
    		list.add(c);
    		interfaces = c.getInterfaces();
    		for (int i = 0; i < interfaces.length; i++) {
    			list.add(interfaces[i]);
    		}
    	} while ((c = c.getSuperclass()) != null);
    	
    	classHierarchyCache.put(c, list);
    	
    	return list;
    }
}
