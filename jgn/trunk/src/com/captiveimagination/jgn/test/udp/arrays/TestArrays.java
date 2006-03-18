package com.captiveimagination.jgn.test.udp.arrays;

import java.io.*;
import java.net.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.client.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.server.*;

/**
 * Basic test to show usage of sending arrays
 * 
 * @author Matthew D. Hicks
 */
public class TestArrays {
	private static byte[] bytes;
	
	public static void main(String[] args) throws Exception {
		// Create byte array
		bytes = new byte[256];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte)i;
		}
		
		JGN.registerMessage(ByteArrayMessage.class, (short)1);
		
		final NetworkingServer server = new NetworkingServer(new UDPMessageServer(IP.getLocalHost(), 1000));
		new Thread(server).start();
		
		final NetworkingClient client = new NetworkingClient(new UDPMessageServer(IP.getLocalHost(), 1005));
		new Thread(client).start();
        
        server.getPlayerMessageServer().addMessageListener(new MessageListener() {
            public void messageReceived(Message message) {
            }
            
            public void messageReceived(ByteArrayMessage message) {
                verify(message.getBytes());
                try {
                    client.shutdown();
                } catch(IOException exc) {
                    exc.printStackTrace();
                }
                server.shutdown();
            }

            public int getListenerMode() {
                return MessageListener.CLOSEST;
            }
        });
        
		if (client.connectAndWait(IP.getLocalHost(), 1000, -1, 15 * 1000)) {
			System.out.println("Connected, sending message...");
			ByteArrayMessage message = new ByteArrayMessage();
			message.setBytes(bytes);
			client.sendToServer(message);
		} else {
			System.err.println("Unable to connect to the server.");
		}
	}
	
	private static void verify(byte[] bytes) {
		System.out.println("Sent: " + TestArrays.bytes.length + ", " + bytes.length);
		for (int i = 0; i < TestArrays.bytes.length; i++) {
			if (TestArrays.bytes[i] != bytes[i]) {
				System.out.println("Bytes different at: " + i);
			}
		}
		System.out.println("Verification complete.");
	}
}
