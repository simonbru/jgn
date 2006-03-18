package com.captiveimagination.jgn.client;

import java.io.*;

import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.message.player.*;

/**
 * @author Matthew D. Hicks
 *
 */
public class ClientPlayerMessageListener implements MessageListener {
	private NetworkingClient client;
	
	public ClientPlayerMessageListener(NetworkingClient client) {
		this.client = client;
	}
	
	public void messageReceived(Message message) {
	}

	public void messageReceived(PlayerJoinResponseMessage message) {
		client.connectResponse(message);
	}
	
    public void messageReceived(PlayerJoinRequestMessage message) {
        client.connectRequest(message);
    }
    
	public void messageReceived(ServerDisconnectMessage message) {
        try {
            client.disconnect();
        } catch(IOException exc) {
            // TODO handle this better
            exc.printStackTrace();
        }
	}
	
    public void messageReceived(PlayerDisconnectMessage message) {
        client.playerDisconnect(message);
    }
    
	public int getListenerMode() {
		return MessageListener.CLOSEST;
	}
}
