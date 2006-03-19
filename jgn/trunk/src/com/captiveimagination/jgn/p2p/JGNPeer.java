/*
 * Created on Feb 12, 2006
 */
package com.captiveimagination.jgn.p2p;

import com.captiveimagination.jgn.*;

/**
 * The bean representation of a Peer
 * 
 * @author Matthew D. Hicks
 */
public class JGNPeer {
	private long peerId;
    private IP address;
    private int portUDP;
    private int portTCP;
    private long lastHeardFrom;
    
    public JGNPeer(long peerId, IP address, int portUDP, int portTCP) {
    	this.peerId = peerId;
    	this.address = address;
    	this.portUDP = portUDP;
    	this.portTCP = portTCP;
    	heardFrom();
    }
    
    public IP getAddress() {
    	return address;
	}

	public long getPeerId() {
		return peerId;
	}

	public int getPortTCP() {
		return portTCP;
	}

	public int getPortUDP() {
		return portUDP;
	}

	public void heardFrom() {
		lastHeardFrom = System.currentTimeMillis();
	}
	
	public long getLastHeardFrom() {
		return lastHeardFrom;
	}
}
