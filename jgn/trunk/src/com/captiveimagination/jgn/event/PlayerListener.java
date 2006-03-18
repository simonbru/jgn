/*
 * Created on Feb 5, 2006
 */
package com.captiveimagination.jgn.event;

import com.captiveimagination.jgn.message.player.*;

public interface PlayerListener {
    public void createLocalPlayer(PlayerJoinResponseMessage message);
    
    public void createLocalPlayerDenied();
    
    public void removeLocalPlayer(short playerId);
    
    public void createRemotePlayer(PlayerJoinRequestMessage message);
    
    public void removeRemotePlayer(PlayerDisconnectMessage message);
}
