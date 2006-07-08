package com.captiveimagination.jgn.test.nat;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.server.*;

/**
 * @author Matthew D. Hicks
 *
 */
public class EchoServer {
	public static void main(String[] args) throws Exception {
		JGN.registerMessage(EchoMessage.class, (short)1);
		
		final NetworkingServer server = new NetworkingServer(10010, 10020);
		
		server.addMessageListener(new MessageListener() {
			public void messageReceived(Message message) {
				System.out.println("S> Message received: " + message.getClass().getName() + " From: " + message.getRemoteAddress() + ":" + message.getRemotePort());
			}

			public void messageReceived(EchoMessage message) {
				System.out.println("S> Received Message: " + message.getString() + " from " + message.getRemoteAddress().toString());
				server.sendToAllClients(message);
			}
			
			public int getListenerMode() {
				return MessageListener.CLOSEST;
			}
		});
		
		server.addMessageSentListener(new MessageSentListener() {
			public void messageSent(Message message) {
				System.out.println("S> Message sent: " + message.getClass().getName() + " (" + server.getClass().getName() + "), To: " + message.getRemoteAddress() + ":" + message.getRemotePort() + ", From: " + message.getMessageServer().getAddress() + ":" + message.getMessageServer().getPort());
			}

			public int getListenerMode() {
				return MessageListener.BASIC;
			}
		});
		
		Thread t = new Thread(server);
		t.start();
		System.out.println("EchoServer Started Successfully");
	}
}
