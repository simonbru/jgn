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
	private int readPosition;
	private ByteBuffer readBuffer;

	public TCPMessageServer(InetSocketAddress address, int maxQueueSize) throws IOException {
		super(address, maxQueueSize);
		selector = Selector.open();

		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.socket().bind(address);
		ssc.configureBlocking(false);
		ssc.register(selector, SelectionKey.OP_ACCEPT);

		readPosition = 0;
		readBuffer = ByteBuffer.allocateDirect(1024 * 10);
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
		SelectionKey key = channel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ
						| SelectionKey.OP_WRITE);
		key.attach(client);
		channel.connect(client.getAddress());
		return null;
	}
	
	public void disconnect(MessageClient client) throws IOException {
		// TODO sendCertified DisconnectMessage
		Iterator<SelectionKey> iterator = selector.keys().iterator();
		while (iterator.hasNext()) {
			SelectionKey key = iterator.next();
			if (key.attachment() == client) {
				key.channel().close();
				key.cancel();
			}
		}
		client.setStatus(MessageClient.STATUS_DISCONNECTED);
		getDisconnectedConnectionQueue().add(client);
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
					accept((ServerSocketChannel) activeKey.channel());
				} else if (activeKey.isReadable()) {
					read((SocketChannel) activeKey.channel());
				} else if (activeKey.isWritable()) {
					while (write((SocketChannel) activeKey.channel()))
						continue;
				} else if (activeKey.isConnectable()) {
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
		channel.read(readBuffer);
		MessageClient client = (MessageClient) channel.keyFor(selector).attachment();
		Message message;
		try {
			while ((message = readMessage(client)) != null) {
				client.getIncomingMessageQueue().add(message);
			}
		} catch (MessageHandlingException exc) {
			// FIXME we need to show the cause!
			// appearantly IOE is not suitable for this
			throw new IOException(exc.getMessage());
		}
	}

	private Message readMessage(MessageClient client) throws MessageHandlingException {
		client.received();
		int position = readBuffer.position();
		readBuffer.position(readPosition);
		int messageLength = readBuffer.getInt();
		if (messageLength <= position - 4 - readPosition) {
			// Read message
			short typeId = readBuffer.getShort();
			Class<? extends Message> c = client.getMessageClass(typeId);
			if (c == null) {
				if (client.isConnected()) {
					throw new MessageHandlingException("Message received from unknown messageTypeId: " + typeId);
				}
				readBuffer.position(position);
				return null;
			}
			Message message = JGN.getConverter(c).receiveMessage(readBuffer);
			if (messageLength < position - 4 - readPosition) {
				// Still has content
				readPosition = messageLength + 4 + readPosition;
				readBuffer.position(position);
			} else {
				// Clear the buffer
				readBuffer.clear();
				readPosition = 0;
			}
			message.setMessageClient(client);
			return message;
		} else {
			// If the capacity of the buffer has been reached
			// we must compact it
			// FIXME this involves a data-copy, don't
			readBuffer.position(readPosition);
			readBuffer.compact();
			position = position - readPosition;
			readPosition = 0;
			readBuffer.position(position);
		}
		return null;
	}

	private boolean write(SocketChannel channel) throws IOException {
		SelectionKey key = channel.keyFor(selector);
		MessageClient client = (MessageClient) key.attachment();

		client.sent();
		
		if (client.getCurrentWrite() != null) {
			//
			// (riven) [reference A] -----------------------------vv
			//
			channel.write(client.getCurrentWrite());
			if (!client.getCurrentWrite().hasRemaining()) {
				client.setCurrentWrite(null);
			}
		} else {
			ByteBuffer buffer;

			try {
				// TODO make 50000 adjustable (getter/setter)
				buffer = PacketCombiner.combine(client, 50000);
			} catch (MessageHandlingException exc) {
				// FIXME handle properly
				exc.printStackTrace();
				buffer = null;
			}

			if (buffer != null) {
				channel.write(buffer);
				if (buffer.hasRemaining()) {
					client.setCurrentWrite(buffer);
				}
				// (riven) added this here: otherwise
				// we send an empty BB to [reference A]  ----------^^
				else client.setCurrentWrite(null);
			}
		}

		/*SelectionKey key = channel.keyFor(selector);
		 MessageClient client = (MessageClient)key.attachment();
		 //System.out.println("Client Can Write: " + client.getAddress().getPort());
		 if (client.getCurrentWrite() != null) {
		 channel.write(client.getCurrentWrite());
		 if (!client.getCurrentWrite().hasRemaining()) {
		 client.setCurrentWrite(null);
		 client.getOutgoingMessageQueue().add(client.getCurrentMessage());
		 client.setCurrentMessage(null);
		 // If there are still messages to write and the buffer isn't full,
		 // we return true;
		 if (!client.getOutgoingQueue().isEmpty()) return true;
		 }
		 } else if (!client.getOutgoingQueue().isEmpty()) {
		 Message message = client.getOutgoingQueue().poll();
		 if (message == null) return false;		// TODO figure out why this is necessary
		 writeBuffer.clear();
		 writeBuffer.putShort(JGN.getMessageTypeId(message.getClass()));
		 ByteBuffer buffer = convertMessage(message, writeBuffer);
		 writeMessageLength.clear();
		 writeMessageLength.putInt(buffer.position());
		 writeMessageLength.flip();
		 buffer.flip();
		 channel.write(writeMessageLength);
		 channel.write(buffer);
		 if (buffer.hasRemaining()) {
		 client.setCurrentWrite(buffer);
		 client.setCurrentMessage(message);
		 } else {
		 client.getOutgoingMessageQueue().add(message);
		 // If there are still messages to write and the buffer isn't full,
		 // we return true;
		 if (!client.getOutgoingQueue().isEmpty()) return true;
		 }
		 }*/
		return false;
	}

	private void connect(SocketChannel channel) throws IOException {
		channel.finishConnect();
		MessageClient client = (MessageClient) channel.keyFor(selector).attachment();
		getIncomingConnectionQueue().add(client);
	}
}
