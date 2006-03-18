package com.captiveimagination.jgn.message.player;

/**
 * Should be sent from client to server to request joining of that
 * server. A server should send back a <code>PlayerJoinResponseMessage</code>
 * that tells if the client's request is accepted, and if accepted the playerId
 * assigned to this player.
 * 
 * @author Matthew D. Hicks
 */
public class PlayerJoinRequestMessage extends PlayerMessage {
    private int portUDP;
    private int portTCP;
    
	public int getPortTCP() {
        return portTCP;
    }

    public void setPortTCP(int portTCP) {
        this.portTCP = portTCP;
    }

    public int getPortUDP() {
        return portUDP;
    }

    public void setPortUDP(int portUDP) {
        this.portUDP = portUDP;
    }

    public long getResendTimeout() {
		return 1000;
	}

	public int getMaxTries() {
		return 5;
	}
}
