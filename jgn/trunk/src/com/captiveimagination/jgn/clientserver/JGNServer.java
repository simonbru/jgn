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
import java.util.concurrent.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.event.*;

/**
 * @author Matthew D. Hicks
 */
public class JGNServer {
	private MessageServer reliableServer;
	private MessageServer fastServer;
	private ConcurrentHashMap<Long,JGNConnection> registry;
	
	public JGNServer(MessageServer reliableServer, MessageServer fastServer) {
		this.reliableServer = reliableServer;
		this.fastServer = fastServer;
		registry = new ConcurrentHashMap<Long,JGNConnection>();
		
		ServerListener listener = new ServerListener(this);
		
		if (reliableServer != null) reliableServer.addConnectionListener(listener);
		if (fastServer != null) fastServer.addConnectionListener(listener);
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
		return registry.values().toArray(new JGNConnection[registry.size()]);
	}
	
	protected void register(MessageClient client) {
		JGNConnection connection = registry.get(client.getId());
		if (connection == null) {
			connection = new JGNConnection();
			registry.put(client.getId(), connection);
		}
		// TODO handle this without explicit knowledge of the MessageServer type
		if (client.getMessageServer() instanceof TCPMessageServer) {
			connection.setReliableClient(client);
		} else {
			connection.setFastClient(client);
		}
	}
}

class ServerListener extends ConnectionAdapter {
	private JGNServer server;
	
	public ServerListener(JGNServer server) {
		this.server = server;
	}
	
	public void negotiationComplete(MessageClient client) {
		server.register(client);
	}
}
