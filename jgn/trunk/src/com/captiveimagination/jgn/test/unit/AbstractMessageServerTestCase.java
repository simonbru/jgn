package com.captiveimagination.jgn.test.unit;

import java.io.*;
import java.net.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.event.*;

import junit.framework.*;

public class AbstractMessageServerTestCase extends TestCase {
	protected MessageServer server1;
	protected MessageClient client1;
	protected boolean client1Disconnected;
	protected MessageServer server2;
	protected MessageClient client2;
	protected boolean client2Disconnected;
	
	protected void setUp() throws IOException, InterruptedException {
		boolean tcp = false;
		boolean debug = false;
		
		JGN.register(MyCertifiedMessage.class);
		JGN.register(MyRealtimeMessage.class);
		JGN.register(MyUniqueMessage.class);
		JGN.register(MySerializableMessage.class);
		
		// Create first MessageServer
		InetSocketAddress address1 = new InetSocketAddress(InetAddress.getLocalHost(), 1000);
		if (tcp) {
			server1 = new TCPMessageServer(address1);
		} else {
			server1 = new UDPMessageServer(address1);
		}
		if (debug) {
			server1.addMessageListener(DebugListener.getInstance());
			server1.addConnectionListener(DebugListener.getInstance());
		}
		server1.addConnectionListener(new ConnectionListener() {
			public void connected(MessageClient client) {
				client1Disconnected = false;
			}

			public void negotiationComplete(MessageClient client) {
				client1 = client;
			}

			public void disconnected(MessageClient client) {
				System.out.println("Disconnected1");
				client1Disconnected = true;
			}
			
		});
		JGN.createThread(server1).start();
		
		// Create second MessageServer
		InetSocketAddress address2 = new InetSocketAddress(InetAddress.getLocalHost(), 2000);
		if (tcp) {
			server2 = new TCPMessageServer(address2);
		} else {
			server2 = new UDPMessageServer(address2);
		}
		if (debug) {
			server2.addMessageListener(DebugListener.getInstance());
			server2.addConnectionListener(DebugListener.getInstance());
		}
		server2.addConnectionListener(new ConnectionListener() {
			public void connected(MessageClient client) {
				client2Disconnected = false;
			}

			public void negotiationComplete(MessageClient client) {
				client2 = client;
			}

			public void disconnected(MessageClient client) {
				System.out.println("Disconnected2");
				client2Disconnected = true;
			}
			
		});
		JGN.createThread(server2).start();
		
		// Connect server2 to server1
		MessageClient client = server2.connectAndWait(address1, 5000);
		if (client == null) {
			System.err.println("Unable to establish connection!");
		} else {
			System.out.println("Connection established successfully");
		}
		long time = System.currentTimeMillis();
		while (System.currentTimeMillis() < time + 5000) {
			if ((client1 != null) && (client2 != null)) break;
			Thread.sleep(1);
		}
		assertTrue(client1 != null);
		assertTrue(client2 != null);
	}

	protected void tearDown() throws IOException, InterruptedException {
		server1.closeAndWait(5000);
		server2.closeAndWait(5000);
	}
}
