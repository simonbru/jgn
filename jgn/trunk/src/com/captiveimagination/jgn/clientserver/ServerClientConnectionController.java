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
 * Created: Jul 22, 2006
 */
package com.captiveimagination.jgn.clientserver;

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
public class ServerClientConnectionController extends DefaultConnectionController implements ConnectionListener, MessageListener {
	private JGNServer server;
	private boolean[] playerIds;
	
	public ServerClientConnectionController(JGNServer server) {
		this.server = server;
		playerIds = new boolean[Short.MAX_VALUE];
	}
	
	public void negotiate(MessageClient client) {
		JGNDirectConnection connection = (JGNDirectConnection)server.register(client);
		
		LocalRegistrationMessage message = new LocalRegistrationMessage();
		short playerId = connection.getPlayerId();
		if (playerId == -1) {
			playerId = nextPlayerId();
			connection.setPlayerId(playerId);
		}
		
		// Send negotiation message back
		message.setId(playerId);
		JGN.populateRegistrationMessage(message);
		client.sendMessage(message);
		
		// Throw event to listeners of connection
		if (((server.hasBoth()) && (connection.getReliableClient() != null) && (connection.getFastClient() != null)) || (!server.hasBoth())) {
			ConcurrentLinkedQueue<ClientConnectionListener> listeners = server.getListeners();
			Iterator<ClientConnectionListener> iterator = listeners.iterator();
			while (iterator.hasNext()) {
				iterator.next().connected(connection);
			}

			// Send connection message to all connected clients
			PlayerStatusMessage psm = new PlayerStatusMessage();
			psm.setPlayerId(playerId);
			psm.setPlayerStatus(PlayerStatusMessage.STATUS_CONNECTED);
			server.sendToAllExcept(psm, playerId);
			
			// Send messages to the client for all established connections
			JGNConnection[] connections = server.getConnections();
			for (int i = 0; i < connections.length; i++) {
				if (connections[i].getPlayerId() != playerId) {
					psm.setPlayerId(connections[i].getPlayerId());
					psm.setPlayerStatus(PlayerStatusMessage.STATUS_CONNECTED);
					connection.sendMessage(psm);
				}
			}
		}
	}
		
	private synchronized short nextPlayerId() {
		for (int i = 0; i < playerIds.length; i++) {
			if (!playerIds[i]) {
				playerIds[i] = true;
				return (short)i;
			}
		}
		throw new RuntimeException("Ran out of player ids.");
	}

	public void connected(MessageClient client) {
	}

	public void disconnected(MessageClient client) {
		JGNDirectConnection connection = (JGNDirectConnection)server.unregister(client);
	}

	public void negotiationComplete(MessageClient client) {
	}

	
	public void messageCertified(Message message) {
	}


	public void messageFailed(Message message) {
	}


	public void messageReceived(Message message) {
		if (message instanceof PlayerMessage) {
			// Route messages to destination
			JGNConnection connection = server.getConnection(message.getDestinationPlayerId());
			// TODO add validation features
			if (connection != null) {
				connection.sendMessage(message);
			}
		}
	}


	public void messageSent(Message message) {
	}
}
