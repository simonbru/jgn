package com.captiveimagination.jgn.server;

import com.captiveimagination.jgn.*;

/**
 * @author Matthew D. Hicks
 *
 */
public class BasicPlayer implements JGNPlayer {
	private short playerId;
	private IP address;
	private int portUDP;
    private int portTCP;
	private long lastHeardFrom;
	
	public BasicPlayer(short playerId, IP address, int portUDP, int portTCP) {
		this.playerId = playerId;
		this.address = address;
		this.portUDP = portUDP;
        this.portTCP = portTCP;
		lastHeardFrom = System.currentTimeMillis();
	}
	
	public short getPlayerId() {
		return playerId;
	}

	public IP getAddress() {
        return address;
	}

	public int getUDPPort() {
		return portUDP;
	}
    
    public void setUDPPort(int portUDP) {
        this.portUDP = portUDP;
    }
    
    public int getTCPPort() {
        return portTCP;
    }
    
    public void setTCPPort(int portTCP) {
        this.portTCP = portTCP;
    }
    
    public int getPlayerPort() {
        if (portTCP > 0) {
            return portTCP;
        } else {
            return portUDP;
        }
    }

	public long getLastHeardFrom() {
		return lastHeardFrom;
	}
	
	public void heardFrom() {
		lastHeardFrom = System.currentTimeMillis();
	}
}
