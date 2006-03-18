package com.captiveimagination.jgn.server;

import java.io.*;

import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.message.player.*;

/**
 * @author Matthew D. Hicks
 *
 */
public class ServerPlayerMessageListener implements MessageListener {
	private NetworkingServer server;
	
	public ServerPlayerMessageListener(NetworkingServer server) {
		this.server = server;
	}
	
	public void messageReceived(Message message) {
	}
	
	public void messageReceived(PlayerMessage message) {
		// Update the player to say that it has been heard from
		Player player = server.getPlayer(message.getPlayerId());
        if (player != null) {
            player.heardFrom();
        }
	}
	
	public void messageReceived(PlayerJoinRequestMessage message) {
        try {
            server.joinRequest(message);
        } catch(IOException exc) {
            // TODO create exception handling for server player message listener
            exc.printStackTrace();
        }
	}
	
	public void messageReceived(PlayerDisconnectMessage message) {
		server.disconnectRequest(message);
	}

	public int getListenerMode() {
		return MessageListener.ALL;
	}
}
