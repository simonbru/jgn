package com.captiveimagination.jgn.test.udp.ping;

import java.net.*;

import com.captiveimagination.jgn.*;

/**
 * Tests the ping functionality of JGN
 * 
 * @author Matthew D. Hicks
 */
public class TestPing {
	public static void main(String[] args) throws Exception {
		MessageServer server1 = new UDPMessageServer(IP.getLocalHost(), 1000);
        // We start the update thread - this is an alternative to calling update() in your game thread
        server1.startUpdateThread();
		
		MessageServer server2 = new UDPMessageServer(IP.getLocalHost(), 1005);
        // We start the update thread - this is an alternative to calling update() in your game thread
        server2.startUpdateThread();
		System.out.println("Ping: " + server2.ping(IP.getLocalHost(), 1000, 10000));
		server1.shutdown();
		server2.shutdown();
	}
}
