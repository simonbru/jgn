package com.captiveimagination.jgn.client;

import com.captiveimagination.jgn.message.player.*;

/**
 * This interface serves as a means by which the implementing game
 * can extend communication between the client and server.
 * 
 * @author Matthew D. Hicks
 */
public interface ClientSession {
	public PlayerJoinRequestMessage createJoinRequest();
}
