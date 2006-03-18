package com.captiveimagination.jgn.client;

import com.captiveimagination.jgn.message.player.*;

/**
 * Default implementation of ClientSession.
 * 
 * @author Matthew D. Hicks
 */
public class DefaultClientSession implements ClientSession {
	public PlayerJoinRequestMessage createJoinRequest() {
		return new PlayerJoinRequestMessage();
	}
}
