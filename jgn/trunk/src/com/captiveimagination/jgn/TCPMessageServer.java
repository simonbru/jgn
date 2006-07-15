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
package com.captiveimagination.jgn;

import java.io.*;
import java.net.*;
import java.nio.channels.*;

import com.captiveimagination.jgn.message.*;

/**
 * @author Matthew D. Hicks
 */
public class TCPMessageServer extends NIOMessageServer {
	public TCPMessageServer(SocketAddress address) throws IOException {
		this(address, 1024);
	}
	
	public TCPMessageServer(SocketAddress address, int maxQueueSize) throws IOException {
		super(address, maxQueueSize);
	}
	
	protected SelectableChannel bindServer(SocketAddress address) throws IOException {
		ServerSocketChannel channel = selector.provider().openServerSocketChannel();
		channel.socket().bind(address);
		channel.configureBlocking(false);
		channel.register(selector, SelectionKey.OP_ACCEPT);
		return channel;
	}

	protected void accept(SelectableChannel channel) throws IOException {
		// TODO validate connections
		SocketChannel connection = ((ServerSocketChannel)channel).accept();
		connection.configureBlocking(false);
		connection.socket().setTcpNoDelay(true);
		SelectionKey key = connection.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		MessageClient client = new MessageClient((SocketAddress) connection.socket().getRemoteSocketAddress(), this);
		client.setStatus(MessageClient.Status.NEGOTIATING);
		key.attach(client);
		getIncomingConnectionQueue().add(client);
		getMessageClients().add(client);
	}

	protected void connect(SelectableChannel channel) throws IOException {
		((SocketChannel)channel).finishConnect();
		MessageClient client = (MessageClient)channel.keyFor(selector).attachment();
		getIncomingConnectionQueue().add(client);
	}
	
	protected void read(SelectableChannel channel) throws IOException {
		MessageClient client = (MessageClient)channel.keyFor(selector).attachment();
		try {
			((SocketChannel)channel).read(client.getReadBuffer());
		} catch(IOException exc) {
			// Handle connections being closed
			disconnectInternal(client, false);
		}
		Message message;
		try {
			while ((message = readMessage(client)) != null) {
				client.getIncomingMessageQueue().add(message);
			}
		} catch (MessageHandlingException exc) {
			// FIXME we need to show the cause!
			// appearantly IOE is not suitable for this
			throw new RuntimeException(exc);
		}
	}

	protected boolean write(SelectableChannel channel) throws IOException {
		SelectionKey key = channel.keyFor(selector);
		MessageClient client = (MessageClient)key.attachment();
				
		if (client.getCurrentWrite() != null) {
			client.sent();		// Let the system know something has been written
			((SocketChannel)channel).write(client.getCurrentWrite().getBuffer());
			if (!client.getCurrentWrite().getBuffer().hasRemaining()) {
				// Write all messages in combined to sent queue
				client.getCurrentWrite().process();
				
				client.setCurrentWrite(null);
			} else {
				// Take completed messages and add them to the sent queue
				client.getCurrentWrite().process();
			}
		} else {
			CombinedPacket combined;
			try {
				// TODO make 50000 adjustable (getter/setter)
				combined = PacketCombiner.combine(client, 50000);
			} catch (MessageHandlingException exc) {
				// FIXME handle properly
				exc.printStackTrace();
				combined = null;
			}

			if (combined != null) {
				((SocketChannel)channel).write(combined.getBuffer());
				if (combined.getBuffer().hasRemaining()) {
					client.setCurrentWrite(combined);
					
					// Take completed messages and add them to the sent queue
					combined.process();
				} else {
					client.setCurrentWrite(null);
					
					// Write all messages in combined to sent queue
					combined.process();
					
					return true;
				}
			} else if (client.getStatus() == MessageClient.Status.DISCONNECTING) {
				disconnectInternal(client, true);
			}
		}
		return false;
	}
	
	public MessageClient connect(SocketAddress address) throws IOException {
		MessageClient client = getMessageClient(address);
		if (client != null) {
			return client;	// Client already connected, simply return it
		}
		
		client = new MessageClient(address, this);
		client.setStatus(MessageClient.Status.NEGOTIATING);
		getMessageClients().add(client);
		SocketChannel channel = selector.provider().openSocketChannel();
		channel.socket().setTcpNoDelay(true);
		channel.configureBlocking(false);
		SelectionKey key = channel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		key.attach(client);
		channel.connect(address);
		return null;
	}
}
