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
 * Created: Jul 15, 2006
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

/**
 * @author Matthew D. Hicks
 */
public class JGNClient implements Updatable {
	private long id;
	private short playerId;
	private MessageServer reliableServer;
	private MessageServer fastServer;
	private ClientServerConnectionController controller;
	private ConcurrentLinkedQueue<ClientConnectionListener> listeners;
	
	private JGNDirectConnection serverConnection;
	private ConcurrentLinkedQueue<JGNConnection> connections;
	
	public JGNClient(MessageServer reliableServer, MessageServer fastServer) {
		id = JGN.generateUniqueId();
		this.reliableServer = reliableServer;
		this.fastServer = fastServer;
		
		connections = new ConcurrentLinkedQueue<JGNConnection>();
		
		listeners = new ConcurrentLinkedQueue<ClientConnectionListener>();
		MessageListener ml = new MessageAdapter() {
			public void messageReceived(Message message) {
				if (message instanceof PlayerStatusMessage) {
					PlayerStatusMessage psm = (PlayerStatusMessage)message;
					JGNConnection connection = getConnection(psm.getPlayerId());
					if (connection == null) {
						connection = register(psm.getPlayerId());
					}
					Iterator<ClientConnectionListener> iterator = listeners.iterator();
					while (iterator.hasNext()) {
						if (psm.getPlayerStatus() == PlayerStatusMessage.STATUS_CONNECTED) {
							iterator.next().connected(connection);
						} else {
							iterator.next().disconnected(connection);
						}
					}
				} else if (message instanceof LocalRegistrationMessage) {
					LocalRegistrationMessage lrm = (LocalRegistrationMessage)message;
					setPlayerId((short)lrm.getId());
				}
			}
		};
		
		controller = new ClientServerConnectionController(this);
		
		if (reliableServer != null) {
			reliableServer.setConnectionController(controller);
			reliableServer.addMessageListener(ml);
		}
		if (fastServer != null) {
			fastServer.setConnectionController(controller);
			fastServer.addMessageListener(ml);
		}
	}
	
	public JGNClient(SocketAddress reliableAddress, SocketAddress fastAddress) throws IOException {
		this(reliableAddress != null ? new TCPMessageServer(reliableAddress) : null, fastAddress != null ? new UDPMessageServer(fastAddress) : null);
	}
	
	public long getId() {
		return id;
	}
	
	public short getPlayerId() {
		return playerId;
	}
	
	protected void setPlayerId(short playerId) {
		this.playerId = playerId;
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

	public void connect(SocketAddress reliableRemoteAddress, SocketAddress fastRemoteAddress) throws IOException {
		if (serverConnection != null) throw new IOException("A connection already exists. Only one connection to a server may exist.");
		
		serverConnection = new JGNDirectConnection();
		if (reliableRemoteAddress != null) {
			serverConnection.setReliableClient(reliableServer.connect(reliableRemoteAddress));
		}
		if (fastRemoteAddress != null) {
			serverConnection.setFastClient(fastServer.connect(fastRemoteAddress));
		}
	}
	
	/**
	 * Invokes connect() and then waits <code>timeout</code> for the connection to complete successfully.
	 * If it is unable to connect within the allocated time an IOException will be thrown.
	 * 
	 * @param reliableRemoteAddress
	 * @param fastRemoteAddress
	 * @param timeout
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void connectAndWait(SocketAddress reliableRemoteAddress, SocketAddress fastRemoteAddress, long timeout) throws IOException, InterruptedException {
		connect(reliableRemoteAddress, fastRemoteAddress);
		
		long time = System.currentTimeMillis();
		while (System.currentTimeMillis() < time + timeout) {
			if (isServerConnected()) return;
			Thread.sleep(10);
		}

		// Last attempt before failing
		if (!isServerConnected()) {
			// fastClient exists and is not connected
			throw new IOException("Connection to fastRemoteAddress failed.");
		}
	}

	private boolean isServerConnected() {
		if (serverConnection.isConnected()) {
			if ((reliableServer == null) == (serverConnection.getReliableClient() == null)) {
				if ((fastServer == null) == (serverConnection.getFastClient() == null)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private JGNConnection register(short playerId) {
		JGNConnection connection = new JGNRelayConnection(this, playerId);
		connections.add(connection);
		return connection;
	}
	
	public JGNConnection[] getConnections() {
		synchronized(connections) {
			return connections.toArray(new JGNConnection[connections.size()]);
		}
	}
	
	public JGNConnection getConnection(short playerId) {
		Iterator<JGNConnection> iterator = connections.iterator();
		while (iterator.hasNext()) {
			JGNConnection connection = iterator.next();
			if (connection.getPlayerId() == playerId) return connection;
		}
		return null;
	}
	
	public JGNConnection getServerConnection() {
		return serverConnection;
	}
	
	public void addClientConnectionListener(ClientConnectionListener listener) {
		listeners.add(listener);
	}
	
	public boolean removeClientConnectionListener(ClientConnectionListener listener) {
		return listeners.remove(listener);
	}
	
	public void addMessageListener(MessageListener listener) {
		if (reliableServer != null) reliableServer.addMessageListener(listener);
		if (fastServer != null) fastServer.addMessageListener(listener);
	}
	
	public void removeMessageListener(MessageListener listener) {
		if (reliableServer != null) reliableServer.removeMessageListener(listener);
		if (fastServer != null) fastServer.removeMessageListener(listener);
	}
	
	public void disconnect() throws IOException {
		serverConnection.disconnect();
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
