package com.captiveimagination.jgn.test.udp.certify;

import com.captiveimagination.jgn.*;

/**
 * @author Matthew D. Hicks
 *
 */
public class TestCertify {
	public static void main(String[] args) throws Exception {
		JGN.registerMessage(BasicCertifiedMessage.class, (short)1);
		
		MessageServer server1 = new UDPMessageServer(IP.getLocalHost(), 1000);
        // We start the update thread - this is an alternative to calling update() in your game thread
        server1.startUpdateThread();
		
		UDPMessageServer server2 = new UDPMessageServer(IP.getLocalHost(), 1005);
        // We start the update thread - this is an alternative to calling update() in your game thread
        server2.startUpdateThread();
		
		// Show use of send and wait feature
		BasicCertifiedMessage message = new BasicCertifiedMessage();
		long time = JGN.getNanoTime();
		System.out.println("Certified: " + server2.sendCertified(message, IP.getLocalHost(), 1000, 10 * 1000));
		System.out.println("Returned after " + ((JGN.getNanoTime() - time) / 1000000) + " milliseconds.");
		
		// NOTE: you can also just fire and forget this message as it will automatically resend if necessary
		
		server1.shutdown();
		server2.shutdown();
	}
}