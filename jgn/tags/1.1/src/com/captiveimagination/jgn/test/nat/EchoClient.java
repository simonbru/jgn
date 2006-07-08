package com.captiveimagination.jgn.test.nat;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.client.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;

/**
 * @author Matthew D. Hicks
 */
public class EchoClient {
	public static void main(String[] args) throws Exception {
		JGN.registerMessage(EchoMessage.class, (short)1);
		
		final NetworkingClient client = new NetworkingClient(5000, 6000);
		
		client.addMessageListener(new MessageListener() {
			public void messageReceived(Message message) {
                System.out.println("C> Message received: " + message.getClass().getName());
			}

			public void messageReceived(EchoMessage message) {
				System.out.println("C> Received Message: " + message.getString() + " from " + message.getRemoteAddress().toString());
				try {
					//client.disconnect();
					//client.shutdown();
				} catch(Exception exc) {
					exc.printStackTrace();
				}
			}
			
			public int getListenerMode() {
				return MessageListener.CLOSEST;
			}
		});
		
		client.addMessageSentListener(new MessageSentListener() {
			public void messageSent(Message message) {
				System.out.println("C> Message sent: " + message.getClass().getName() + " (" + message.getMessageServer().getClass().getName() + "), To: " + message.getRemoteAddress() + ":" + message.getRemotePort() + ", From: " + message.getMessageServer().getAddress() + ":" + message.getMessageServer().getPort());
			}

			public int getListenerMode() {
				return MessageListener.BASIC;
			}
		});
		
		Thread t = new Thread(client);
		t.start();
		System.out.println("C> EchoClient Started Successfully");
		
		if (client.connectAndWait(IP.fromName("captiveimagination.com"), 10010, 10020, 15000)) {
		//if (client.connectAndWait(IP.getLocalHost(), 10010, 10020, 15000)) {
		
			EchoMessage message = new EchoMessage();
			message.setString("Hello Server!");
			client.sendToServer(message);
		} else {
			System.err.println("Unable to establish connection to server!");
			client.shutdown();
		}
	}
}
