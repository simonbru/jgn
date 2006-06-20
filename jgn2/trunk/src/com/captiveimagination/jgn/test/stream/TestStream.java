package com.captiveimagination.jgn.test.stream;

import java.io.*;
import java.net.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.stream.*;

public class TestStream {
	public static JGNInputStream input;
	
	public static void main(String[] args) throws Exception {
		createServerThread();
		
		// Create Client
		final MessageServer server = new TCPMessageServer(new InetSocketAddress(InetAddress.getLocalHost(), 2000));
		Thread t = new Thread() {
			public void run() {
				try {
					while (true) {
						server.update();
					}
				} catch(Exception exc) {
					exc.printStackTrace();
				}
			}
		};
		t.start();
		MessageClient client = server.connectAndWait(new InetSocketAddress(InetAddress.getLocalHost(), 1000), 5000);
		if (client != null) {
			System.out.println("Connection established!");
			
		}
	}
	
	private static final void createServerThread() throws UnknownHostException, IOException {
		final MessageServer server = new TCPMessageServer(new InetSocketAddress(InetAddress.getLocalHost(), 1000));
		server.addConnectionListener(new ConnectionListener() {
			public void connected(MessageClient client) {
			}

			public void negotiationComplete(MessageClient client) {
				try {
					input = client.getInputStream();
				} catch(Exception exc) {
					exc.printStackTrace();
				}
			}

			public void disconnected(MessageClient client) {
			}
		});
		//FileOutputStream fos = new FileOutputStream()
		Thread t = new Thread() {
			public void run() {
				try {
					while (true) {
						server.update();
						if (input != null) {
							try {
								input.read();
							} catch(StreamClosedException exc) {
								System.out.println("Stream closed!");
								
							}
						}
					}
				} catch(Exception exc) {
					exc.printStackTrace();
				}
			}
		};
		t.start();
	}
}
