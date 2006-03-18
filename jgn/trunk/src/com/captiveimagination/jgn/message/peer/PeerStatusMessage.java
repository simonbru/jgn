package com.captiveimagination.jgn.message.peer;

/**
 * @author Matthew D. Hicks
 *
 */
public class PeerStatusMessage extends PeerMessage {
	private long statusPeerId;
	private byte[] statusPeerAddress;
	private int statusPeerPortUDP;
	private int statusPeerPortTCP;
	
	public byte[] getStatusPeerAddress() {
		return statusPeerAddress;
	}
	
	public void setStatusPeerAddress(byte[] statusPeerAddress) {
		this.statusPeerAddress = statusPeerAddress;
	}
	
	public long getStatusPeerId() {
		return statusPeerId;
	}
	
	public void setStatusPeerId(long statusPeerId) {
		this.statusPeerId = statusPeerId;
	}
	
	public int getStatusPeerPortTCP() {
		return statusPeerPortTCP;
	}
	
	public void setStatusPeerPortTCP(int statusPeerPortTCP) {
		this.statusPeerPortTCP = statusPeerPortTCP;
	}
	
	public int getStatusPeerPortUDP() {
		return statusPeerPortUDP;
	}
	
	public void setStatusPeerPortUDP(int statusPeerPortUDP) {
		this.statusPeerPortUDP = statusPeerPortUDP;
	}
}
