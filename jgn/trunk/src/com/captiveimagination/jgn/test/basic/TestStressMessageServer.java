/**
 * Copyright (c) 2005-2006 JavaGameNetworking
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'JavaGameNetworking' nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Created: Jun 10, 2006
 */
package com.captiveimagination.jgn.test.basic;

import java.net.*;

import javax.crypto.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.queue.*;
import com.captiveimagination.jgn.translation.encryption.*;

/**
 * @author Matthew D. Hicks
 */
public class TestStressMessageServer {
	private static final int MAX = 10000000; //2000000000;
	
	public static int receiveCount = 0;
	public static MessageClient client1;
	public static MessageClient client2;
	
	public static void main(String[] args) throws Exception {
		JGN.register(BasicMessage.class);
		final MessageServer server1 = new TCPMessageServer(new InetSocketAddress(InetAddress.getLocalHost(), 1000));
		server1.addConnectionListener(new ConnectionListener() {
			public void connected(MessageClient client) {
				System.out.println("S1> Connected: " + client);
				client1 = client;
			}

			public void negotiationComplete(MessageClient client) {
				System.out.println("S1> Negotiation completed successfully with: " + client);
			}
			
			public void disconnected(MessageClient client) {
				System.out.println("S1> Disconnected: " + client);
			}


			public void kicked(MessageClient client, String reason) {
			}
		});
		server1.addMessageListener(new MessageAdapter() {
			private long time;
			
			public void messageReceived(Message message) {
				if (message instanceof BasicMessage) {
					if (receiveCount == 0) time = System.currentTimeMillis();
					receiveCount++;
					//System.out.println("Count: " + receiveCount + ", " + ((BasicMessage)message).getValue());
					//if (receiveCount > 2000) System.out.println("Receive Count: " + receiveCount);
					if (receiveCount == MAX) {
						System.out.println("Completed in: " + (System.currentTimeMillis() - time) + "ms");
						System.exit(0);
					}
				}
			}

			public void messageSent(Message message) {
				System.out.println("S1> Message Sent: " + message);
			}
			
		});
		Thread t = new Thread() {
			private long cycle;
			
			public void run() {
				try {
					while (true) {
						if (System.currentTimeMillis() - cycle > 1000) {
							if (client1 != null) {
								System.out.println("Received: " + receiveCount + 
												 ", " + client1.getIncomingMessageQueue().getTotal() + "(" + client1.getIncomingMessageQueue().getSize() + ")" +
												 ", " + client1.getOutgoingMessageQueue().getTotal() + "(" + client1.getOutgoingMessageQueue().getSize() + ")" +
												 ", " + client1.getOutgoingQueue().getTotal() + "(" + client1.getOutgoingQueue().getSize() + ")" +
												 
												 ", " + client2.getIncomingMessageQueue().getTotal() + "(" + client2.getIncomingMessageQueue().getSize() + ")" +
												 ", " + client2.getOutgoingMessageQueue().getTotal() + "(" + client2.getOutgoingMessageQueue().getSize() + ")" +
												 ", " + client2.getOutgoingQueue().getTotal() + "(" + client2.getOutgoingQueue().getSize() + ")");
							}
							cycle = System.currentTimeMillis();
						}
						Thread.sleep(1);
					}
				} catch(Exception exc) {
					exc.printStackTrace();
				}
			}
		};
		//t.setDaemon(true);
		//t.setPriority(Thread.MIN_PRIORITY);
		t.start();
		
		final MessageServer server2 = new TCPMessageServer(new InetSocketAddress(InetAddress.getLocalHost(), 2000));
		
		JGN.createThread(server1, server2).start();
		
//		KeyGenerator kgen = KeyGenerator.getInstance("Blowfish");
//		SecretKey skey = kgen.generateKey();
//		byte[] raw = skey.getEncoded();
//		BlowfishDataTranslator trans = new BlowfishDataTranslator(raw);
//		server1.addDataTranslator(trans);
//		server2.addDataTranslator(trans);
		
		server2.addConnectionListener(new ConnectionListener() {
			public void connected(MessageClient client) {
				System.out.println("S2> Connected: " + client);
				client2 = client;
			}

			public void negotiationComplete(MessageClient client) {
				System.out.println("S2> Negotiation completed successfully with: " + client);
			}
			
			public void disconnected(MessageClient client) {
				System.out.println("S2> Disconnected: " + client);
			}

			
			public void kicked(MessageClient client, String reason) {
			}
		});
		MessageClient client = server2.connectAndWait(new InetSocketAddress(InetAddress.getLocalHost(), 1000), 5000);
		if (client != null) {
			System.out.println("Connection established!");
			BasicMessage message = new BasicMessage();
			long time = System.currentTimeMillis();
			//Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
			//message.setData(new byte[512]);
			for (int i = 0; i < MAX; i++) {
				message.setValue(i);
				try {
					client.sendMessage(message);
				} catch(QueueFullException exc) {
					i--;
					try {
						Thread.sleep(1);
					} catch(InterruptedException ie) {}
				}
			}
			System.out.println("Enqueued in: " + (System.currentTimeMillis() - time) + "ms");
		} else {
			System.out.println("Connection timed out!");
		}
	}
}
