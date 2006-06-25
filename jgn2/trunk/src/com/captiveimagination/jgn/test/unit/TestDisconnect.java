package com.captiveimagination.jgn.test.unit;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.message.*;

public class TestDisconnect extends AbstractMessageServerTestCase {
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
		Thread.sleep(1000);
		assertTrue(client1.getStatus() == MessageClient.STATUS_DISCONNECTED);
		assertTrue(client2.getStatus() == MessageClient.STATUS_DISCONNECTED);
		assertTrue(client1Disconnected);
		assertTrue(client2Disconnected);
		System.out.println("Disconnection took: " + (System.currentTimeMillis() - time) + "ms");
	}
	
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
		Thread.sleep(1000);
		assertTrue(client1.getStatus() == MessageClient.STATUS_DISCONNECTED);
		assertTrue(client2.getStatus() == MessageClient.STATUS_DISCONNECTED);
		assertTrue(client1Disconnected);
		assertTrue(client2Disconnected);
		System.out.println("Disconnection took: " + (System.currentTimeMillis() - time) + "ms");
	}
	
	public void testTimeout1() throws Exception {
		DisconnectMessage message = new DisconnectMessage();
		client1.sendMessage(message);
		long time = System.currentTimeMillis();
		while (client1.isConnected()) {
			if (System.currentTimeMillis() > time + MessageServer.DEFAULT_TIMEOUT + 5000) break;
		}
		Thread.sleep(1000);
		System.out.println("Elapsed: " + (System.currentTimeMillis() - time) + "ms");
		assertTrue(client1.getStatus() == MessageClient.STATUS_TERMINATED);
		assertTrue(client2.getStatus() == MessageClient.STATUS_DISCONNECTED);
		assertTrue(client1Disconnected);
		assertTrue(client2Disconnected);
	}
	
	public void testTimeout2() throws Exception {
		DisconnectMessage message = new DisconnectMessage();
		client1.sendMessage(message);
		client1.sendMessage(new NoopMessage());
		client2.sendMessage(new NoopMessage());
		long time = System.currentTimeMillis();
		while (client1.isConnected()) {
			if (System.currentTimeMillis() > time + MessageServer.DEFAULT_TIMEOUT + 5000) break;
		}
		Thread.sleep(1000);
		System.out.println("Elapsed: " + (System.currentTimeMillis() - time) + "ms");
		assertTrue(client1.getStatus() == MessageClient.STATUS_TERMINATED);
		assertTrue(client2.getStatus() == MessageClient.STATUS_DISCONNECTED);
		assertTrue(client1Disconnected);
		assertTrue(client2Disconnected);
	}
}
