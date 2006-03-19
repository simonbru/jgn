package com.captiveimagination.jgn.server;

import com.captiveimagination.jgn.message.player.*;

public interface ServerSession {
    /**
     * This method is invoked when a request to join the server is received.
     * 
     * @param message
     * @return
     */
	public PlayerJoinResponseMessage receivedJoinRequest(PlayerJoinRequestMessage message);
	
    /**
     * This method is invoked if receivedJoinRequest returns a accept PlayerJoinResponseMessage.
     * This message should inform currently connected clients of the new player join.
     * 
     * @param playerId
     * @return
     *      Should return the PlayerJoinRequestMessage that will be sent to all currently
     *      connected clients.
     */
    public PlayerJoinRequestMessage createClientJoinRequest(short playerId);
    
	public long getPlayerTimeout();
	
	public void expirePlayer(short playerId);
	
	public JGNPlayer getPlayer(short playerId);
	
	public JGNPlayer[] getPlayers();
}
