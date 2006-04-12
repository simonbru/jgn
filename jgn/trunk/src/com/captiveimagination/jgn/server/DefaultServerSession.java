/*
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
 */
package com.captiveimagination.jgn.server;

import java.util.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.message.player.*;

/**
 * This is the default implementation of the ServerSession interface.
 * 
 * @author Matthew D. Hicks
 */
public class DefaultServerSession implements ServerSession {
	private ArrayList players;
	private boolean[] playerIds;
	
	public DefaultServerSession() {
		players = new ArrayList();
		playerIds = new boolean[Short.MAX_VALUE];
	}
	
	public PlayerJoinResponseMessage receivedJoinRequest(PlayerJoinRequestMessage message) {
		short playerId = -1;
		for (int i = 1; i < playerIds.length; i++) {
			if (!playerIds[i]) {
				playerId = (short)i;
				playerIds[i] = true;
				break;
			}
		}
        IP address = message.getRemoteAddress();
        int portUDP = -1;
        int portTCP = -1;
        if (message.getMessageServer() instanceof UDPMessageServer) {
            portUDP = message.getRemotePort();
        } else if (message.getMessageServer() instanceof TCPMessageServer) {
            portTCP = message.getRemotePort();
        }
		BasicPlayer player = new BasicPlayer(playerId, address, portUDP, portTCP);
		players.add(player);
		
		PlayerJoinResponseMessage response = new PlayerJoinResponseMessage();
		response.setAccepted(true);
		response.setPlayerId(playerId);
		return response;
	}
	
    public PlayerJoinRequestMessage createClientJoinRequest(short playerId) {
        PlayerJoinRequestMessage request = new PlayerJoinRequestMessage();
        request.setPlayerId(playerId);
        return request;
    }
	
	public void expirePlayer(short playerId) {
		JGNPlayer player = getPlayer(playerId);
        if (player != null) {
            playerIds[player.getPlayerId()] = false;
            players.remove(player);
        }
	}
	
	public long getPlayerTimeout() {
		return 30 * 1000;
	}

	public JGNPlayer getPlayer(short playerId) {
		JGNPlayer player;
		for (int i = 0; i < players.size(); i++) {
			player = (JGNPlayer)players.get(i);
			if (player.getPlayerId() == playerId) return player;
		}
		return null;
	}
	
	public JGNPlayer[] getPlayers() {
		return (JGNPlayer[])players.toArray(new JGNPlayer[players.size()]);
	}
}
