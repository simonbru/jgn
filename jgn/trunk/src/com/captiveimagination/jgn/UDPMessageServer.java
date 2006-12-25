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
 * Created: Jul 6, 2006
 */
package com.captiveimagination.jgn;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

import com.captiveimagination.jgn.message.*;

/**
 * @author Matthew D. Hicks
 */
public final class UDPMessageServer extends NIOMessageServer {
	private DatagramChannel channel;
	private ByteBuffer readLookup;
	
	public UDPMessageServer(SocketAddress address) throws IOException {
		this(address, 1024);
	}
	
	public UDPMessageServer(SocketAddress address, int maxQueueSize) throws IOException {
		super(address, maxQueueSize);
		readLookup = ByteBuffer.allocateDirect(1024 * 5);
	}

	protected SelectableChannel bindServer(SocketAddress address) throws IOException {
		channel = selector.provider().openDatagramChannel();
		channel.socket().bind(address);
		channel.configureBlocking(false);
		channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		return channel;
	}
	
	protected void accept(SelectableChannel channel) throws IOException {
		// UDP Message Server will never receive an accept event
	}

	protected void connect(SelectableChannel channel) throws IOException {
		// UDP Message Server will never receive a connect event
	}

	protected void read(SelectableChannel c) throws IOException {
		MessageClient client = null;
		try {
			InetSocketAddress address = (InetSocketAddress)channel.receive(readLookup);
			if (address == null) {
				// TODO a message was sent but never reached the host - figure out how to use this
				return;
			}
			
			readLookup.limit(readLookup.position());
			readLookup.position(0);
			client = getMessageClient(address);
			
			if (client == null) {
				client = new MessageClient(address, this);
				client.setStatus(MessageClient.Status.NEGOTIATING);
				getIncomingConnectionQueue().add(client);
				getMessageClients().add(client);
			}
			client.getReadBuffer().put(readLookup);
			readLookup.clear();
			
			Message message;
			while ((message = readMessage(client)) != null) {
				client.receiveMessage(message);
			}
		} catch(MessageHandlingException exc) {
			// FIXME we need to show the cause!
			// appearantly IOE is not suitable for this
			throw new RuntimeException(exc);
		}
	}

	protected boolean write(SelectableChannel c) throws IOException {
		for (MessageClient client : getMessageClients()) {
			if (client.getCurrentWrite() != null) {
				client.sent();		// Let the system know something has been written
				channel.send(client.getCurrentWrite().getBuffer(), client.getAddress());
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
					channel.send(combined.getBuffer(), client.getAddress());
					if (combined.getBuffer().hasRemaining()) {
						client.setCurrentWrite(combined);
						
						// Take completed messages and add them to the sent queue
						combined.process();
						
						return false;	// No more room for sending
					} else {
						client.setCurrentWrite(null);
						
						// Write all messages in combined to sent queue
						combined.process();
					}
				} else if (client.getStatus() == MessageClient.Status.DISCONNECTING) {
					disconnectInternal(client, true);
				}
			}
		}
		return false;
	}

	public MessageClient connect(SocketAddress address) throws IOException {
		MessageClient client = getMessageClient(address);
		if ((client != null) && (client.getStatus() != MessageClient.Status.DISCONNECTING) && (client.getStatus() != MessageClient.Status.DISCONNECTED)) {
			return client;		// Client already connected, simply return it
		}
		client = new MessageClient(address, this);
		client.setStatus(MessageClient.Status.NEGOTIATING);
		getMessageClients().add(client);
		getIncomingConnectionQueue().add(client);
		getConnectionController().negotiate(client);
		return client;
	}
}
