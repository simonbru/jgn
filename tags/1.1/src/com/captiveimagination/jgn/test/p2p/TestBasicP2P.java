package com.captiveimagination.jgn.test.p2p;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.p2p.*;
import com.captiveimagination.jgn.test.*;

/**
 * @author Matthew D. Hicks
 *
 */
public class TestBasicP2P {
	public static void main(String[] args) throws Exception {
		JGN.registerMessage(BasicMessage.class, (short)1);
		
		// Create Peer #1
		UDPMessageServer server1a = new UDPMessageServer(null, 1000);
		TCPMessageServer server1b = new TCPMessageServer(null, 1100);
		NetworkingPeer peer1 = new NetworkingPeer(server1a, server1b);
		new Thread(peer1).start();
		
		// Create Peer #2
		UDPMessageServer server2a = new UDPMessageServer(null, 1001);
		TCPMessageServer server2b = new TCPMessageServer(null, 1101);
		NetworkingPeer peer2 = new NetworkingPeer(server2a, server2b);
		new Thread(peer2).start();

		// Create Peer #3
		UDPMessageServer server3a = new UDPMessageServer(null, 1002);
		TCPMessageServer server3b = new TCPMessageServer(null, 1102);
		NetworkingPeer peer3 = new NetworkingPeer(server3a, server3b);
		new Thread(peer3).start();
		
		// Add listeners to see what's happening
		server1a.addMessageListener(new DebugMessageListener("P1UDP"));
		server1b.addMessageListener(new DebugMessageListener("P1TCP"));
		server2a.addMessageListener(new DebugMessageListener("P2UDP"));
		server2b.addMessageListener(new DebugMessageListener("P2TCP"));
		server3a.addMessageListener(new DebugMessageListener("P3UDP"));
		server3b.addMessageListener(new DebugMessageListener("P3TCP"));
		
		System.out.println("Connecting Peer #2 to Peer #1...");
		
		// Connect Peer #2 to Peer #1
		System.out.println("Connected: " + peer2.connectAndWait(null, 1100, 10000));
				
		System.out.println("Now connecting Peer #3 to Peer #1...");
		
		// Connect Peer #3 to Peer #1 - should also get connection to Peer #2
		System.out.println("Connected: " + peer3.connectAndWait(null, 1100, 10000));
		
		System.out.println("Now sleeping to wait for all connections to be established...");
		Thread.sleep(5000);
		
		System.out.println("Now sending message to all peers...");
		BasicMessage message = new BasicMessage();
		message.setText("Test message from Peer3");
		peer3.sendToPeers(message, NetworkingPeer.PEER);
	}
}
