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
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

import com.captiveimagination.jgn.message.*;

/**
 * @author Matthew D. Hicks
 */
public class TCPMessageServer extends MessageServer {
	private Selector selector;

	public TCPMessageServer(InetSocketAddress address, int maxQueueSize) throws IOException {
		super(address, maxQueueSize);
		selector = Selector.open();

		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.socket().bind(address);
		ssc.configureBlocking(false);
		ssc.register(selector, SelectionKey.OP_ACCEPT);
	}

	public TCPMessageServer(InetSocketAddress address) throws IOException {
		this(address, 1024);
	}

	public MessageClient connect(InetSocketAddress address) throws IOException {
		// TODO lookup to see if the connection already exists
		MessageClient client = new MessageClient(address, this);
		client.setStatus(MessageClient.STATUS_NEGOTIATING);
		getMessageClients().add(client);
		SocketChannel channel = SocketChannel.open();
		channel.configureBlocking(false);
		channel.socket().setTcpNoDelay(true);
		// TODO connect timeout?
		SelectionKey key = channel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		key.attach(client);
		channel.connect(client.getAddress());
		return null;
	}
	
	protected void disconnectInternal(MessageClient client, boolean graceful) throws IOException {
		Iterator<SelectionKey> iterator = selector.keys().iterator();
		while (iterator.hasNext()) {
			SelectionKey key = iterator.next();
			if (key.attachment() == client) {
				key.channel().close();
				key.cancel();
			}
		}
		
		// Parse through all the certified messages unsent
		Message message;
		while ((message = client.getCertifiableMessageQueue().poll()) != null) {
			client.getFailedMessageQueue().add(message);
		}
		
		// Execute events to invoke any visible events left
		updateEvents();		// TODO perhaps not remove from getMessageClients() until all events are finished?
		
		getMessageClients().remove(client);
		if (graceful) {
			client.setStatus(MessageClient.STATUS_DISCONNECTED);
		} else {
			client.setStatus(MessageClient.STATUS_TERMINATED);
		}
		getDisconnectedConnectionQueue().add(client);
	}
	
	public void closeAndWait(long timeout) throws IOException, InterruptedException {
		close();
		long time = System.currentTimeMillis();
		while (System.currentTimeMillis() <= time + timeout) {
			if (!isAlive()) return;
			synchronized (getMessageClients()) {
				if ((getMessageClients().size() > 0) && (getMessageClients().peek().getStatus() == MessageClient.STATUS_CONNECTED)) {
					getMessageClients().peek().disconnect();
				}
			}
			Thread.sleep(1);
		}
		throw new IOException("MessageServer did not shutdown within the allotted time (" + getMessageClients().size() + ").");
	}

	public synchronized void updateTraffic() throws IOException {
		// Ignore if no longer alive
		if (!isAlive()) return;
		
		// If should be shutting down, check
		if ((getMessageClients().size() == 0) && (!keepAlive)) {
			Iterator<SelectionKey> iterator = selector.keys().iterator();
			while (iterator.hasNext()) {
				SelectionKey key = iterator.next();
				key.channel().close();
				key.cancel();
			}
			selector.close();
			alive = false;
			return;
		}
		
		// Handle Accept, Read, and Write
		if (selector.selectNow() > 0) {
			Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
			while (keys.hasNext()) {
				SelectionKey activeKey = keys.next();
				keys.remove();
				if ((activeKey.isValid()) && (activeKey.isAcceptable())) {
					accept((ServerSocketChannel) activeKey.channel());
				}
				if ((activeKey.isValid()) && (activeKey.isReadable())) {
					read((SocketChannel) activeKey.channel());
				}
				if ((activeKey.isValid()) && (activeKey.isWritable())) {
					while (write((SocketChannel)activeKey.channel())) continue;
				}
				if ((activeKey.isValid()) && (activeKey.isConnectable())) {
					connect((SocketChannel) activeKey.channel());
				}
			}
		}
	}

	private void accept(ServerSocketChannel channel) throws IOException {
		// TODO validate connections
		SocketChannel connection = channel.accept();
		connection.configureBlocking(false);
		connection.socket().setTcpNoDelay(true);
		SelectionKey key = connection.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		MessageClient client = new MessageClient((InetSocketAddress) connection.socket().getRemoteSocketAddress(), this);
		client.setStatus(MessageClient.STATUS_NEGOTIATING);
		key.attach(client);
		getIncomingConnectionQueue().add(client);
		getMessageClients().add(client);
	}

	private void read(SocketChannel channel) throws IOException {
		MessageClient client = (MessageClient)channel.keyFor(selector).attachment();
		try {
			channel.read(client.getReadBuffer());
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

	private Message readMessage(MessageClient client) throws MessageHandlingException {
		client.received();
		int position = client.getReadBuffer().position();
		client.getReadBuffer().position(client.getReadPosition());
		int messageLength = client.getReadBuffer().getInt();
		//System.out.println("MessageLength(Read): " + messageLength);
		//System.out.println("ReadMessage: " + messageLength + ", " + (position - 4 - readPosition) + " - " + position + ", " + readPosition);
		if (messageLength <= position - 4 - client.getReadPosition()) {
			// Read message
			short typeId = client.getReadBuffer().getShort();
			Class<? extends Message> c = client.getMessageClass(typeId);
			if (c == null) {
				if (client.isConnected()) {
					client.getReadBuffer().position(client.getReadPosition());
					//System.err.println("Buffer: " + client.getReadBuffer().capacity() + ", " + client.getReadBuffer() + ", " + messageLength + ", " + position + ", " + readBuffer.getInt() + ", " + readBuffer.getShort());
					throw new MessageHandlingException("Message received from unknown messageTypeId: " + typeId);
				}
				client.getReadBuffer().position(position);
				return null;
			}
			Message message = JGN.getConverter(c).receiveMessage(client.getReadBuffer());
			if (messageLength < position - 4 - client.getReadPosition()) {
				// Still has content
				client.setReadPosition(messageLength + 4 + client.getReadPosition());
				client.getReadBuffer().position(position);
			} else {
				// Clear the buffer
				client.getReadBuffer().clear();
				client.setReadPosition(0);
			}
			message.setMessageClient(client);
			return message;
		} else {
			// If the capacity of the buffer has been reached
			// we must compact it
			// FIXME this involves a data-copy, don't
			client.getReadBuffer().position(client.getReadPosition());
			client.getReadBuffer().compact();
			position = position - client.getReadPosition();
			client.setReadPosition(0);
			client.getReadBuffer().position(position);
		}
		return null;
	}

	private boolean write(SocketChannel channel) throws IOException {
		SelectionKey key = channel.keyFor(selector);
		MessageClient client = (MessageClient)key.attachment();
				
		if (client.getCurrentWrite() != null) {
			client.sent();		// Let the system know something has been written
			channel.write(client.getCurrentWrite().getBuffer());
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
				channel.write(combined.getBuffer());
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
			} else if (client.getStatus() == MessageClient.STATUS_DISCONNECTING) {
				disconnectInternal(client, true);
			}
		}
		return false;
	}

	private void connect(SocketChannel channel) throws IOException {
		channel.finishConnect();
		MessageClient client = (MessageClient) channel.keyFor(selector).attachment();
		getIncomingConnectionQueue().add(client);
	}
}
