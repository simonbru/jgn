package com.captiveimagination.jgn.message.player;

/**
 * Sent by the server after receiving a <code>PlayerJoinRequestMessage</code>.
 * 
 * @author Matthew D. Hicks
 */
public class PlayerJoinResponseMessage extends PlayerMessage {
	private boolean accepted;
	
	public boolean isAccepted() {
		return accepted;
	}

	public boolean getAccepted() {
		return isAccepted();
	}
	
	public void setAccepted(boolean accepted) {
		this.accepted = accepted;
	}
}
