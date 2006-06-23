package com.captiveimagination.jgn.test.unit;

import java.io.*;
import java.net.*;

import org.junit.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.event.*;

import static org.junit.Assert.assertEquals;
import junit.framework.JUnit4TestAdapter;

public class TestDisconnect {
	private MessageServer server1;
	private MessageClient client1;
	private boolean client1Disconnected;
	private MessageServer server2;
	private MessageClient client2;
	private boolean client2Disconnected;
	
	@Before
	public void setupServers() throws IOException, InterruptedException {
		// Create first MessageServer
		InetSocketAddress address1 = new InetSocketAddress((InetAddress)null, 1000);
		server1 = new TCPMessageServer(address1);
		server1.addConnectionListener(new ConnectionListener() {
			public void connected(MessageClient client) {
				client1Disconnected = false;
			}

			public void negotiationComplete(MessageClient client) {
				client1 = client;
			}

			public void disconnected(MessageClient client) {
				client1Disconnected = true;
			}
			
		});
		JGN.createMessageServerThread(server1).start();
		
		// Create second MessageServer
		InetSocketAddress address2 = new InetSocketAddress((InetAddress)null, 2000);
		server2 = new TCPMessageServer(address2);
		server2.addConnectionListener(new ConnectionListener() {
			public void connected(MessageClient client) {
				client2Disconnected = false;
			}

			public void negotiationComplete(MessageClient client) {
				client2 = client;
			}

			public void disconnected(MessageClient client) {
				client2Disconnected = true;
			}
			
		});
		JGN.createMessageServerThread(server2).start();
		
		// Connect server2 to server1
		MessageClient client = server2.connectAndWait(address1, 5000);
		if (client == null) {
			System.err.println("Unable to establish connection!");
		} else {
			System.out.println("Connection established successfully");
		}
	}
	
	@Test
	public void testDisconnect1() throws Exception {
		client1.disconnect();
		long time = System.currentTimeMillis();
		long timeout = 5000;
		while (System.currentTimeMillis() <= timeout + time) {
			if ((client1.getStatus() == MessageClient.STATUS_DISCONNECTED) && (client2.getStatus() == MessageClient.STATUS_DISCONNECTED)) {
				break;
			}
			Thread.sleep(1);
		}
		assertEquals(client1.getStatus() == MessageClient.STATUS_DISCONNECTED, client2.getStatus() == MessageClient.STATUS_DISCONNECTED);
		assertEquals(client1Disconnected == true, client2Disconnected == true);
		System.out.println("Disconnection took: " + (System.currentTimeMillis() - time) + "ms");
	}
	
	@Test
	public void testDisconnect2() throws Exception {
		client2.disconnect();
		long time = System.currentTimeMillis();
		long timeout = 5000;
		while (System.currentTimeMillis() <= timeout + time) {
			if ((client1.getStatus() == MessageClient.STATUS_DISCONNECTED) && (client2.getStatus() == MessageClient.STATUS_DISCONNECTED)) {
				break;
			}
			Thread.sleep(1);
		}
		assertEquals(client1.getStatus() == MessageClient.STATUS_DISCONNECTED, client2.getStatus() == MessageClient.STATUS_DISCONNECTED);
		assertEquals(client1Disconnected == true, client2Disconnected == true);
		System.out.println("Disconnection took: " + (System.currentTimeMillis() - time) + "ms");
	}
	
	@After
	public void shutdownServers() throws IOException {
		server1.close();
		server2.close();
		System.exit(0);	// TODO remove - temporary
	}
	
	// Compatability
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(TestDisconnect.class);
	}
}
