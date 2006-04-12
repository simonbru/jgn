/*
 * Copyright (c) 2005-2006 JavaGameNetworking
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'JavaGameNetworking' nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.captiveimagination.jgn.p2p;

import java.io.*;
import java.util.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.message.peer.*;

/**
 * The basic class each Peer need instantiate to handle P2P Networking
 * 
 * @author Matthew D. Hicks
 */
public class NetworkingPeer implements Runnable {
	public static final int UDP = 1;
	public static final int TCP = 2;
	public static final int PEER = 3;
	
	public static int PEER_TIMEOUT = 30 * 1000;
	/**
	 * The time in milliseconds to wait before sending a noop
	 * This defaults to 9000 ms (9 seconds).
	 */
	public static long NOOP_DELAY = 9 * 1000;
	
	private long peerId;
    private UDPMessageServer messageServerUDP;
    private TCPMessageServer messageServerTCP;
    private ArrayList peers;
    private boolean keepAlive;
    
    private long lastNoopUDP;
	private long lastNoopTCP;
    
    public NetworkingPeer(UDPMessageServer messageServerUDP, TCPMessageServer messageServerTCP) {
        this.messageServerUDP = messageServerUDP;
        this.messageServerTCP = messageServerTCP;
        init();
    }
    
    private void init() {
    	JGN.registerMessage(PeerMessage.class, (short)(Short.MIN_VALUE + 1));
    	JGN.registerMessage(PeerStatusMessage.class, (short)(Short.MIN_VALUE + 2));
    	
    	peerId = JGN.getUniqueLong();
    	lastNoopUDP = System.currentTimeMillis();
    	lastNoopTCP = System.currentTimeMillis();
    	
    	peers = new ArrayList();
    	
    	MessageListener listener = new MessageListener() {
			public void messageReceived(Message message) {
			}
			
			public void messageReceived(PeerMessage message) {
				JGNPeer peer = getPeer(message.getPeerId());
				if (peer != null) {
					peer.heardFrom();
				}
				if (message.getRequestType() == PeerMessage.REQUEST_JOIN) {
					peer = new JGNPeer(message.getPeerId(), message.getRemoteAddress(), message.getPortUDP(), message.getPortTCP());
					peers.add(peer);
					
					PeerMessage response = new PeerMessage();
					response.setRequestType(PeerMessage.REQUEST_RESPONSE);
					sendToPeer(message.getPeerId(), response, PEER);
				} else if (message.getRequestType() == PeerMessage.REQUEST_RESPONSE) {
					peer = new JGNPeer(message.getPeerId(), message.getRemoteAddress(), message.getPortUDP(), message.getPortTCP());
					peers.add(peer);
					
					if (peers.size() == 1) {
						PeerMessage request = new PeerMessage();
						request.setRequestType(PeerMessage.REQUEST_PEERS);
						sendToPeer(peer.getPeerId(), request, PEER);
					}
				} else if (message.getRequestType() == PeerMessage.REQUEST_DISCONNECT) {
					disconnect(peer.getPeerId());
				} else if (message.getRequestType() == PeerMessage.REQUEST_PEERS) {
					PeerStatusMessage status;
					JGNPeer[] peers = getPeers();
					for (int i = 0; i < peers.length; i++) {
						if (peers[i].getPeerId() != message.getPeerId()) {
							status = new PeerStatusMessage();
							status.setStatusPeerId(peers[i].getPeerId());
							status.setStatusPeerAddress(peers[i].getAddress().getBytes());
							status.setStatusPeerPortUDP(peers[i].getPortUDP());
							status.setStatusPeerPortTCP(peers[i].getPortTCP());
							sendToPeer(message.getPeerId(), status, PEER);
						}
					}
				}
			}
			
			public void messageReceived(PeerStatusMessage message) {
				JGNPeer peer = getPeer(message.getStatusPeerId());
				if (peer == null) {
					try {
						connect(new IP(message.getStatusPeerAddress()), message.getRemotePort());
					} catch(IOException exc) {
						exc.printStackTrace();
						System.err.println("Unable to connect to peer " + message.getPeerId());
					}
				}
			}

			public int getListenerMode() {
				return MessageListener.ALL;
			}
    	};
    	if (messageServerUDP != null) {
    		messageServerUDP.addMessageListener(listener);
    	}
    	if (messageServerTCP != null) {
    		messageServerTCP.addMessageListener(listener);
    	}
    }
    
    public void update() {
        if (messageServerUDP != null) {
            messageServerUDP.update();
        }
        if (messageServerTCP != null) {
            messageServerTCP.update();
        }
        
        JGNPeer[] peers = getPeers();
        
        // Send Noops
        if ((getUDPMessageServer() != null) && (lastNoopUDP + NOOP_DELAY < System.currentTimeMillis())) {
			PeerMessage message = new PeerMessage();
			message.setRequestType(PeerMessage.REQUEST_NOOP);
			for (int i = 0; i < peers.length; i++) {
				sendToPeers(message, UDP);
			}
			lastNoopUDP = System.currentTimeMillis();
		}
		if ((getTCPMessageServer() != null) && (lastNoopTCP + NOOP_DELAY < System.currentTimeMillis())) {
			PeerMessage message = new PeerMessage();
			message.setRequestType(PeerMessage.REQUEST_NOOP);
			for (int i = 0; i < peers.length; i++) {
				sendToPeers(message, UDP);
			}
			lastNoopTCP = System.currentTimeMillis();
		}

        // Expire peers
        for (int i = 0; i < peers.length; i++) {
        	if (peers[i].getLastHeardFrom() + PEER_TIMEOUT < System.currentTimeMillis()) {
        		disconnect(peers[i].getPeerId());
        	}
        }
    }
    
    public void run() {
		keepAlive = true;
		while (keepAlive) {
			try {
				Thread.sleep(100);
			} catch(InterruptedException exc) {
			}
			update();
		}
	}
    
    /**
     * This need be called on connection into the peer environment once.
     * This will send a message to the address and port requesting connection
     * and should receive a response back. If accepted, a second message is
     * sent to request all connected peers. Upon receipt of each peer information
     * a subsequent connect is made to each of them.
     * 
     * @param remoteAddress
     * @param remotePort
     */
    public void connect(IP remoteAddress, int remotePort) throws IOException {
        // Send join message to this peer
    	PeerMessage message = new PeerMessage();
    	message.setPeerId(getPeerId());
    	message.setRequestType(PeerMessage.REQUEST_JOIN);
    	if (messageServerUDP != null) {
    		message.setPortUDP(messageServerUDP.getPort());
    	}
    	if (messageServerTCP != null) {
    		message.setPortTCP(messageServerTCP.getPort());
    	}
    	getPeerMessageServer().sendMessage(message, remoteAddress, remotePort);
    }
    
    public boolean connectAndWait(IP remoteAddress, int remotePort, long maxWait) throws IOException {
    	connect(remoteAddress, remotePort);
    	
    	long time = System.currentTimeMillis();
    	while (peers.size() == 0) {
    		if (System.currentTimeMillis() - time > maxWait) {
    			return false;
    		}
    	}
    	return true;
    }
    
    public void disconnect(long peerId) {
    	JGNPeer peer = getPeer(peerId);
    	if (messageServerTCP != null) {
    		messageServerTCP.disconnect(peer.getAddress(), peer.getPortTCP());
    	}
    	peers.remove(peer);
    }
    
    /**
     * Send a message to all peers with the given method:
     *		UDP, TCP, or PEER (will select the preferred transport method)
     * 
     * @param message
     * @param method
     */
    public void sendToPeers(Message message, int method) {
    	JGNPeer[] peers = getPeers();
    	for (int i = 0; i < peers.length; i++) {
    		sendToPeer(peers[i].getPeerId(), message, method);
    	}
    }
    
    /**
     * Sends a message to the peer specified by <code>peerId</code> with the given method:
     * 		UDP, TCP, or PEER (will select the preferred transport method)
     * 
     * @param peerId
     * @param message
     * @param method
     * @throws IOException
     */
    public void sendToPeer(long peerId, Message message, int method) {
    	try {
	    	if (message instanceof PeerMessage) {
	    		((PeerMessage)message).setPeerId(this.peerId);
	    		((PeerMessage)message).setPortUDP(-1);
	    		((PeerMessage)message).setPortTCP(-1);
				if (messageServerUDP != null) {
					((PeerMessage)message).setPortUDP(messageServerUDP.getPort());
				}
				if (messageServerTCP != null) {
					((PeerMessage)message).setPortTCP(messageServerTCP.getPort());
				}
	    	}
	    	JGNPeer peer = getPeer(peerId);
	    	if (method == PEER) {
	    		if (messageServerTCP != null) {
	    			method = TCP;
	    		} else {
	    			method = UDP;
	    		}
	    	}
	    	if (method == UDP) {
	    		messageServerUDP.sendMessage(message, peer.getAddress(), peer.getPortUDP());
	    	} else {
	    		messageServerTCP.sendMessage(message, peer.getAddress(), peer.getPortTCP());
	    	}
    	} catch(IOException exc) {
    		exc.printStackTrace();
    		System.err.println("Disconnecting peer " + peerId);
    		disconnect(peerId);
    	}
    }
    
    /**
     * @return
     * 		Array of all currently connected peers
     */
    public JGNPeer[] getPeers() {
    	return (JGNPeer[])peers.toArray(new JGNPeer[peers.size()]);
    }
    
    /**
     * @param peerId
     * @return
     * 		The Peer connected defined by <code>peerId</code>
     */
    public JGNPeer getPeer(long peerId) {
    	JGNPeer[] peers = getPeers();
    	for (int i = 0; i < peers.length; i++) {
    		if (peers[i].getPeerId() == peerId) {
    			return peers[i];
    		}
    	}
    	return null;
    }
    
    /**
     * @return
     * 		this peer's peerId
     */
    public long getPeerId() {
    	return peerId;
    }

    public MessageServer getPeerMessageServer() {
    	if (messageServerTCP != null) {
    		return messageServerTCP;
    	}
    	return messageServerUDP;
    }

    public void addMessageListener(MessageListener listener) {
    	if (messageServerUDP != null) {
    		messageServerUDP.addMessageListener(listener);
    	}
    	if (messageServerTCP != null) {
    		messageServerTCP.addMessageListener(listener);
    	}
    }
    
    public void addMessageSentListener(MessageSentListener listener) {
    	if (messageServerUDP != null) {
    		messageServerUDP.addMessageSentListener(listener);
    	}
    	if (messageServerTCP != null) {
    		messageServerTCP.addMessageSentListener(listener);
    	}
    }

    /**
     * @return
     *      The local UDPMessageServer being used to send
     *      and receive messages or null if one is not set.
     */
	public UDPMessageServer getUDPMessageServer() {
		return messageServerUDP;
	}
    
    /**
     * @return
     *      The local TCPMessageServer being used to send
     *      and receive messages or null if one is not set.
     */
    public TCPMessageServer getTCPMessageServer() {
        return messageServerTCP;
    }
    
    public void shutdown() {
    	keepAlive = false;
    }
}
