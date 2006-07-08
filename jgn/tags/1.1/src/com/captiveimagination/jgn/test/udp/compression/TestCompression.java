/**
 * TestCompression.java
 *
 * Created: May 28, 2006
 */
package com.captiveimagination.jgn.test.udp.compression;

import java.io.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.compression.handler.*;
import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.test.*;

/**
 * @author Matthew D. Hicks
 */
public class TestCompression {
	public static void main(String[] args) throws IOException {
		JGN.registerMessage(BasicMessage.class, (short)1);
		
		final MessageServer server1 = new UDPMessageServer(null, 1000);
		server1.setCompressionHandler(new GZipCompressionHandler());
		server1.startUpdateThread();
		
		final MessageServer server2 = new UDPMessageServer(null, 2000);
		server2.setCompressionHandler(new GZipCompressionHandler());
		server2.startUpdateThread();
		
		server2.addMessageListener(new MessageListener() {
			public void messageReceived(Message message) {
			}
			
			public void messageReceived(BasicMessage message) {
				System.out.println("Received:" + message.getText() + ":");
				server1.shutdown();
				server2.shutdown();
			}

			public int getListenerMode() {
				return MessageListener.CLOSEST;
			}
			
		});
		
		BasicMessage message = new BasicMessage();
		message.setText("Hello World!");
		server1.sendMessage(message, null, 2000);
	}

}
