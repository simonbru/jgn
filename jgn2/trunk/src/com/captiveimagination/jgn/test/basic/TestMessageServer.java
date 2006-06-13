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
import java.nio.channels.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.tcp.*;

/**
 * @author Matthew D. Hicks
 */
public class TestMessageServer {
	public static int receiveCount = 0;
	
	public static void main(String[] args) throws Exception {
		JGN.register(BasicMessage.class);
		final MessageServer server = new TCPMessageServer(new InetSocketAddress(InetAddress.getLocalHost(), 1000));
		server.addConnectionListener(new ConnectionListener() {
			public void connected(MessageClient client) {
				System.out.println("Connected: " + client);
			}

			public void negotiationComplete(MessageClient client) {
				System.out.println("Negotiation completed successfully with: " + client);
			}
			
			public void disconnected(MessageClient client) {
				System.out.println("Disconnected: " + client);
			}
		});
		server.addMessageListener(new MessageListener() {
			public void messageReceived(Message message) {
				if (message instanceof BasicMessage) {
					receiveCount++;
					System.out.println("Count: " + receiveCount + ", " + ((BasicMessage)message).getValue());
				}
			}

			public void messageSent(Message message) {
				System.out.println("Message Sent: " + message);
			}
			
		});
		Thread t = new Thread() {
			public void run() {
				try {
					while (true) {
						server.update();
						Thread.sleep(1);
					}
				} catch(Exception exc) {
					exc.printStackTrace();
				}
			}
		};
		t.start();
		
		final MessageServer server2 = new TCPMessageServer(new InetSocketAddress(InetAddress.getLocalHost(), 2000));
		Thread t2 = new Thread() {
			public void run() {
				try {
					while (true) {
						server2.update();
						Thread.sleep(1);
					}
				} catch(Exception exc) {
					exc.printStackTrace();
				}
			}
		};
		t2.start();
		MessageClient client = server2.connectAndWait(new InetSocketAddress(InetAddress.getLocalHost(), 1000), 5000);
		if (client != null) {
			System.out.println("Connection established!");
			BasicMessage message = new BasicMessage();
			for (int i = 0; i < 1000; i++) {
				message.setValue(i);
				client.sendMessage(message);
			}
		} else {
			System.out.println("Connection timed out!");
		}
	}
}
