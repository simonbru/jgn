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

import com.captiveimagination.jgn.*;

/**
 * @author Matthew D. Hicks
 */
public class JGNClient {
	private long id;
	private MessageServer reliableServer;
	private MessageServer fastServer;
	private ClientServerConnectionController controller;
	
	private JGNConnection serverConnection;
	
	public JGNClient(MessageServer reliableServer, MessageServer fastServer) {
		id = JGN.generateUniqueId();
		this.reliableServer = reliableServer;
		this.fastServer = fastServer;
		
		controller = new ClientServerConnectionController(this);
		
		if (reliableServer != null) reliableServer.setConnectionController(controller);
		if (fastServer != null) fastServer.setConnectionController(controller);
	}
	
	public JGNClient(SocketAddress reliableAddress, SocketAddress fastAddress) throws IOException {
		this(reliableAddress != null ? new TCPMessageServer(reliableAddress) : null, fastAddress != null ? new UDPMessageServer(fastAddress) : null);
	}
	
	public long getId() {
		return id;
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

	public void connect(InetSocketAddress reliableRemoteAddress, InetSocketAddress fastRemoteAddress) throws IOException {
		if (serverConnection != null) throw new IOException("A connection already exists. Only one connection to a server may exist.");
		
		serverConnection = new JGNConnection();
		if (reliableRemoteAddress != null) {
			serverConnection.setReliableClient(reliableServer.connect(reliableRemoteAddress));
		}
		if (fastRemoteAddress != null) {
			serverConnection.setFastClient(fastServer.connect(fastRemoteAddress));
		}
	}
	
	public void connectAndWait(InetSocketAddress reliableRemoteAddress, InetSocketAddress fastRemoteAddress, long timeout) throws IOException, InterruptedException {
		connect(reliableRemoteAddress, fastRemoteAddress);
		
		long time = System.currentTimeMillis();
		while (System.currentTimeMillis() < time + timeout) {
			if (serverConnection.isConnected()) return;
			Thread.sleep(10);
		}

		// Last attempt before failing
		if (!serverConnection.isConnected()) {
			// fastClient exists and is not connected
			throw new IOException("Connection to fastRemoteAddress failed.");
		}
	}
}
