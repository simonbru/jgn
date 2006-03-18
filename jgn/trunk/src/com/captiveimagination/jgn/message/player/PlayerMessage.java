package com.captiveimagination.jgn.message.player;

import com.captiveimagination.jgn.message.*;

/**
 * All messages player-specific should extend this Object.
 * 
 * @author Matthew D. Hicks
 */
public abstract class PlayerMessage extends CertifiedMessage {
	private short playerId;
	
	public void setPlayerId(short playerId) {
		this.playerId = playerId;
	}
	
	public short getPlayerId() {
		return playerId;
	}
	
	public long getResendTimeout() {
		return 1000;
	}
	
	public int getMaxTries() {
		return 5;
	}
}
