package com.captiveimagination.jgn.message.player;

import com.captiveimagination.jgn.message.*;

/**
 * This message is sent from the server to all clients when
 * the server is going to shut down.
 * 
 * @author Matthew D. Hicks
 */
public class ServerDisconnectMessage extends CertifiedMessage {
	public long getResendTimeout() {
		return 1000;
	}

	public int getMaxTries() {
		return 10;
	}

}
