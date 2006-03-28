/**
 * 
 */
package com.captiveimagination.jgn.test.tcp.large;

import com.captiveimagination.jgn.IP;
import com.captiveimagination.jgn.JGN;
import com.captiveimagination.jgn.TCPMessageServer;
import com.captiveimagination.jgn.event.MessageListener;
import com.captiveimagination.jgn.message.Message;

/**
 * @author Matthew D. Hicks
 */
public class TestLargeMessages {
	public static void main(String[] args) throws Exception {
		JGN.registerMessage(LargeMessage.class, (short)1);
		
		final TCPMessageServer server1 = new TCPMessageServer(IP.getLocalHost(), 1000);
		server1.startUpdateThread();
		
		final TCPMessageServer server2 = new TCPMessageServer(IP.getLocalHost(), 2000);
		server2.addMessageListener(new MessageListener() {
			public void messageReceived(Message message) {
			}
			
			public void messageReceived(LargeMessage message) {
				System.out.println("Received array of size: " + message.getMessageArray().length);
				server1.shutdown();
				server2.shutdown();
			}

			public int getListenerMode() {
				return MessageListener.CLOSEST;
			}
			
		});
		server2.startUpdateThread();
		
		// Create and send message from server1 to server2
		LargeMessage message = new LargeMessage();
		message.setMessageArray(new byte[20000]);
		
		server1.sendMessage(message, IP.getLocalHost(), 2000);
	}
}
