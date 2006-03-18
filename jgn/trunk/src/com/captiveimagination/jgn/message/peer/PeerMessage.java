package com.captiveimagination.jgn.message.peer;

import com.captiveimagination.jgn.message.*;

/**
 * Base class for PeerMessage communication.
 * 
 * @author Matthew D. Hicks
 */
public class PeerMessage extends CertifiedMessage {
	public static final byte REQUEST_JOIN = 1;
	public static final byte REQUEST_RESPONSE = 2;
	public static final byte REQUEST_PEERS = 3;
	public static final byte REQUEST_NOOP = 4;
	public static final byte REQUEST_DISCONNECT = 5;
	
	private long peerId;
	private byte requestType;
	private int portUDP;
	private int portTCP;
	
	public long getPeerId() {
		return peerId;
	}

	public void setPeerId(long peerId) {
		this.peerId = peerId;
	}

	public byte getRequestType() {
		return requestType;
	}

	public void setRequestType(byte requestType) {
		this.requestType = requestType;
	}

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
		return 2000;
	}

	public int getMaxTries() {
		return 5;
	}

	public static final String getRequestType(byte type) {
		if (type == REQUEST_JOIN) {
			return "REQUEST_JOIN";
		} else if (type == REQUEST_RESPONSE) {
			return "REQUEST_RESPONSE";
		} else if (type == REQUEST_PEERS) {
			return "REQUEST_PEERS";
		} else if (type == REQUEST_NOOP) {
			return "REQUEST_NOOP";
		} else if (type == REQUEST_DISCONNECT) {
			return "REQUEST_DISCONNECT";
		}
		return "Unknown: " + type;
	}
}
