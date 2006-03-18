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
		BasicPlayer player = new BasicPlayer(playerId, address, message.getPortUDP(), message.getPortTCP());
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
		Player player = getPlayer(playerId);
        if (player != null) {
            playerIds[player.getPlayerId()] = false;
            players.remove(player);
        }
	}
	
	public long getPlayerTimeout() {
		return 30 * 1000;
	}

	public Player getPlayer(short playerId) {
		Player player;
		for (int i = 0; i < players.size(); i++) {
			player = (Player)players.get(i);
			if (player.getPlayerId() == playerId) return player;
		}
		return null;
	}
	
	public Player[] getPlayers() {
		return (Player[])players.toArray(new Player[players.size()]);
	}
}
