/**
 * PlayerCertifiedMessage.java
 *
 * Created: Apr 30, 2006
 */
package com.captiveimagination.jgn.message.player;

import com.captiveimagination.jgn.message.*;

/**
 * @author Matthew D. Hicks
 */
public class PlayerCertifiedMessage extends CertifiedMessage implements PlayerMessage {
	private short playerId;
	
	public long getResendTimeout() {
		return 1000;
	}
	
	public int getMaxTries() {
		return 5;
	}

	public void setPlayerId(short playerId) {
		this.playerId = playerId;
	}

	public short getPlayerId() {
		return playerId;
	}
}
