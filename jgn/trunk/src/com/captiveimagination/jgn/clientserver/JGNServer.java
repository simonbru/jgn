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
 * Created: Jul 11, 2006
 */
package com.captiveimagination.jgn.clientserver;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.clientserver.message.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.message.type.*;

/**
 * @author Matthew D. Hicks
 */
public class JGNServer {
	private MessageServer reliableServer;
	private MessageServer fastServer;
	private ConcurrentLinkedQueue<JGNDirectConnection> registry;
	//private ConcurrentHashMap<Long,JGNConnection> registry;
	private ServerClientConnectionController controller;
	private ConcurrentLinkedQueue<ClientConnectionListener> listeners;
	
	public JGNServer(MessageServer reliableServer, MessageServer fastServer) {
		this.reliableServer = reliableServer;
		this.fastServer = fastServer;
		registry = new ConcurrentLinkedQueue<JGNDirectConnection>();
		
		listeners = new ConcurrentLinkedQueue<ClientConnectionListener>();
		
		controller = new ServerClientConnectionController(this);
		if (reliableServer != null) {
			reliableServer.setConnectionController(controller);
			reliableServer.addConnectionListener(controller);
		}
		if (fastServer != null) {
			fastServer.setConnectionController(controller);
			fastServer.addConnectionListener(controller);
		}
	}
	
	public JGNServer(SocketAddress reliableAddress, SocketAddress fastAddress) throws IOException {
		this(reliableAddress != null ? new TCPMessageServer(reliableAddress) : null, fastAddress != null ? new UDPMessageServer(fastAddress) : null);
	}
	
	public void update() throws IOException {
		updateTraffic();
		updateEvents();
		updateConnections();
	}
	
	public void updateConnections() {
		if (reliableServer != null) reliableServer.updateConnections();
		if (fastServer != null) fastServer.updateConnections();
	}
	
	public void updateTraffic() throws IOException {
		if (reliableServer != null) reliableServer.updateTraffic();
		if (fastServer != null) fastServer.updateTraffic();
	}
	
	public void updateEvents() {
		if (reliableServer != null) reliableServer.updateEvents();
		if (fastServer != null) fastServer.updateEvents();
	}

	public JGNConnection[] getConnections() {
		return registry.toArray(new JGNConnection[registry.size()]);
	}
	
	public JGNConnection getConnection(MessageClient client) {
		Iterator<JGNDirectConnection> iterator = registry.iterator();
		while (iterator.hasNext()) {
			JGNDirectConnection connection = iterator.next();
			if ((connection.getFastClient() != null) && (connection.getFastClient().getId() == client.getId())) {
				return connection;
			} else if ((connection.getReliableClient() != null) && (connection.getReliableClient().getId() == client.getId())) {
				return connection;
			}
		}
		return null;
	}
	
	protected synchronized JGNConnection register(MessageClient client) {
		JGNDirectConnection connection = (JGNDirectConnection)getConnection(client);	// NOOOO! Must use id
		if (connection == null) {
			connection = new JGNDirectConnection();
			registry.add(connection);
		}
		// TODO handle this without explicit knowledge of the MessageServer type
		if (client.getMessageServer() instanceof TCPMessageServer) {
			connection.setReliableClient(client);
		} else {
			connection.setFastClient(client);
		}
		return connection;
	}
	
	protected synchronized JGNConnection unregister(MessageClient client) {
		JGNDirectConnection connection = (JGNDirectConnection)getConnection(client);
		if (connection.getFastClient() == client) {
			connection.setFastClient(null);
		} else if (connection.getReliableClient() == client) {
			connection.setReliableClient(null);
		}
		if ((connection.getFastClient() == null) && (connection.getReliableClient() == null)) {
			registry.remove(client.getId());
			
			// Send disconnection message to all other players
			PlayerStatusMessage psm = new PlayerStatusMessage();
			psm.setPlayerId(connection.getPlayerId());
			psm.setPlayerStatus(PlayerStatusMessage.STATUS_DISCONNECTED);
			sendToAllExcept(psm, connection.getPlayerId());
			
			// Throw event to listeners of connection
			Iterator<ClientConnectionListener> iterator = listeners.iterator();
			while (iterator.hasNext()) {
				iterator.next().disconnected(connection);
			}
		}
		return connection;
	}

	public <T extends Message & PlayerMessage> void sendToAllExcept(T message, short exceptionPlayerId) {
		JGNConnection[] connections = getConnections();
		for (int i = 0; i < connections.length; i++) {
			if (connections[i].getPlayerId() != exceptionPlayerId) {
				if (connections[i].isConnected()) {
					connections[i].sendMessage(message);
				}
			}
		}
	}
	
	public <T extends Message & PlayerMessage> void sendToAll(T message) {
		sendToAllExcept(message, (short)-1);
	}
	
	public void addClientConnectionListener(ClientConnectionListener listener) {
		listeners.add(listener);
	}
	
	public boolean removeClientConnectionListener(ClientConnectionListener listener) {
		return listeners.remove(listener);
	}
	
	protected ConcurrentLinkedQueue<ClientConnectionListener> getListeners() {
		return listeners;
	}
	
	public void addMessageListener(MessageListener listener) {
		if (reliableServer != null) reliableServer.addMessageListener(listener);
		if (fastServer != null) fastServer.addMessageListener(listener);
	}
	
	public void removeMessageListener(MessageListener listener) {
		if (reliableServer != null) reliableServer.removeMessageListener(listener);
		if (fastServer != null) fastServer.removeMessageListener(listener);
	}
	
	public void close() throws IOException {
		if (reliableServer != null) reliableServer.close();
		if (fastServer != null) fastServer.close();
	}
	
	public boolean isAlive() {
		if ((reliableServer != null) && (reliableServer.isAlive())) return true;
		if ((fastServer != null) && (fastServer.isAlive())) return true;
		return false;
	}
	
	protected boolean hasBoth() {
		if ((reliableServer != null) && (fastServer != null)) return true;
		return false;
	}
}
