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
 * Created: Jun 7, 2006
 */
package com.captiveimagination.jgn.tcp;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.*;

import com.captiveimagination.jgn.*;

/**
 * @author Matthew D. Hicks
 */
public class TCPMessageServer extends MessageServer {
	private Selector selector;
	
	public TCPMessageServer(InetSocketAddress address) throws IOException {
		super(address);
		selector = Selector.open();
		
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.socket().bind(address);
		ssc.configureBlocking(false);
		ssc.register(selector, SelectionKey.OP_ACCEPT);
	}

	public void close() {
		// TODO call disconnect on all MessageClients and then shutdown Selector
	}
	
	public synchronized void updateTraffic() throws IOException {
		// Handle Accept, Read, and Write
		if (selector.selectNow() > 0) {
			Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
			while (keys.hasNext()) {
				SelectionKey activeKey = keys.next();
				keys.remove();
				if (activeKey.isAcceptable()) {
					accept((ServerSocketChannel)activeKey.channel());
				} else if (activeKey.isReadable()) {
					read(activeKey.channel());
				} else if (activeKey.isWritable()) {
					write(activeKey.channel());
				} else if (activeKey.isConnectable()) {
					connect((SocketChannel)activeKey.channel());
				}
			}
		}
		
		// Handle Outgoing Connections
		while (!getOutgoingConnectionQueue().isEmpty()) {
			MessageClient client = getOutgoingConnectionQueue().poll();
			SocketChannel channel = SocketChannel.open();
			channel.configureBlocking(false);
			SelectionKey key = channel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			key.attach(client);
			channel.connect(client.getAddress());
		}
	}
	
	private void accept(ServerSocketChannel channel) throws IOException {
		// TODO validate connections
		SocketChannel connection = channel.accept();
		connection.configureBlocking(false);
		SelectionKey key = connection.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		MessageClient client = new MessageClient((InetSocketAddress)connection.socket().getRemoteSocketAddress(), this);
		key.attach(client);
		getIncomingConnectionQueue().add(client);
	}
	
	private void read(SelectableChannel channel) {
		// TODO read the incoming message
		System.out.println("Incoming read");
	}
	
	private void write(SelectableChannel channel) {
		SelectionKey key = channel.keyFor(selector);
		MessageClient client = (MessageClient)key.attachment();
		if (!client.getOutgoingQueue().isEmpty()) {
			// TODO writable...
			System.out.println("Incoming write?");	
		}
	}
	
	private void connect(SocketChannel channel) throws IOException {
		channel.finishConnect();
		MessageClient client = (MessageClient)channel.keyFor(selector).attachment();
		getIncomingConnectionQueue().add(client);
	}
}
