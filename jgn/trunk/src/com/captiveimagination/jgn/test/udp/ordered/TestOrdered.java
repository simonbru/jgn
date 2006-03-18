package com.captiveimagination.jgn.test.udp.ordered;

import java.net.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.client.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.server.*;
import com.captiveimagination.jgn.util.*;

/**
 * Simple Example using OrderedMessage
 * 
 * @author Matthew D. Hicks
 */
public class TestOrdered {
	public static void main(String[] args) throws Exception {
		JGN.registerMessage(BasicOrderedMessage.class, (short)1);
		
		NetworkingServer server = new NetworkingServer(new UDPMessageServer(IP.getLocalHost(), 1000));
		server.getPlayerMessageServer().addMessageListener(new MessageListener() {
			public void messageReceived(Message message) {
				System.out.println("Server Received Message: " + message.getClass().getName());
			}
			
			public void messageReceived(BasicOrderedMessage message) {
				System.out.println("Received Message: " + message.getOrderGroup() + ", " + message.getOrderId() + ", " + message.getOrderOriginator() + ", " + message.getValue());
			}

			public int getListenerMode() {
				return MessageListener.CLOSEST;
			}
		});
		server.getPlayerMessageServer().addMessageSentListener(new MessageSentListener() {
			public void messageSent(Message message, MessageServer server) {
				System.out.println("Server Sent Message: " + message.getClass().getName());
			}

			public int getListenerMode() {
				return MessageSentListener.BASIC;
			}
		});
		new Thread(server).start();
		
		NetworkingClient client = new NetworkingClient(new UDPMessageServer(IP.getLocalHost(), 1005));
		client.connectAndWait(IP.getLocalHost(), 1000, -1, 15 * 1000);
		BasicOrderedMessage m0 = createMessage(0, "Zero");
		BasicOrderedMessage m1 = createMessage(1, "One");
		BasicOrderedMessage m2 = createMessage(2, "Two");
		BasicOrderedMessage m3 = createMessage(3, "Three");
		BasicOrderedMessage m4 = createMessage(4, "Four");
		BasicOrderedMessage m5 = createMessage(5, "Five");
		new Thread(client).start();
		
		MessageServerMonitor msm1 = new MessageServerMonitor(server.getPlayerMessageServer(), 1000);
		msm1.start();
		msm1.createGUI("Server");
		MessageServerMonitor msm2 = new MessageServerMonitor(client.getPlayerMessageServer(), 1000);
		msm2.start();
		msm2.createGUI("Client");
		
		client.sendToServer(m5);
		client.sendToServer(m3);
		client.sendToServer(m2);
		client.sendToServer(m4);
		client.sendToServer(m1);
		client.sendToServer(m0);
	}
	
	private static BasicOrderedMessage createMessage(long orderId, String value) {
		BasicOrderedMessage message = new BasicOrderedMessage();
		
		// This should typically not be set, will be set internally automatically if not set
		message.setOrderId(orderId);
		
		message.setValue(value);
		return message;
	}
}
