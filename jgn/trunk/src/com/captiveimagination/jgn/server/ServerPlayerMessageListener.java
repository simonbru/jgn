package com.captiveimagination.jgn.server;

import java.io.*;

import com.captiveimagination.jgn.*;
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
        JGNPlayer player = server.getPlayer(message.getPlayerId());
        
        // Check to see if the player has a port assigned for this message server
        if ((message.getMessageServer() instanceof TCPMessageServer) && (player.getTCPPort() == -1)) {
            player.setTCPPort(message.getRemotePort());
        } else if ((message.getMessageServer() instanceof UDPMessageServer) && (player.getUDPPort() == -1)) {
            player.setUDPPort(message.getRemotePort());
        }
        
		// Update the player to say that it has been heard from
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
