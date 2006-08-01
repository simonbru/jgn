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
import java.nio.channels.*;
import java.util.*;

import com.captiveimagination.jgn.message.*;

/**
 * @author Matthew D. Hicks
 */
public abstract class NIOMessageServer extends MessageServer {
	protected Selector selector;
	
	public NIOMessageServer(SocketAddress address, int maxQueueSize) throws IOException {
		super(address, maxQueueSize);
		
		selector = Selector.open();
		bindServer(address);
	}
	
	protected abstract SelectableChannel bindServer(SocketAddress address) throws IOException;

	protected abstract void accept(SelectableChannel channel) throws IOException;
	
	protected abstract void connect(SelectableChannel channel) throws IOException;
	
	protected abstract void read(SelectableChannel channel) throws IOException;
	
	protected abstract boolean write(SelectableChannel channel) throws IOException;
	
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
			client.setStatus(MessageClient.Status.DISCONNECTED);
		} else {
			client.setStatus(MessageClient.Status.DISCONNECTED);
			// TODO implement a feature for knowing if it was gracefully closed
		}
		getDisconnectedConnectionQueue().add(client);
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
					accept(activeKey.channel());
				}
				if ((activeKey.isValid()) && (activeKey.isReadable())) {
					read(activeKey.channel());
				}
				if ((activeKey.isValid()) && (activeKey.isWritable())) {
					while (write(activeKey.channel())) continue;
				}
				if ((activeKey.isValid()) && (activeKey.isConnectable())) {
					connect(activeKey.channel());
				}
			}
		}
	}

	protected Message readMessage(MessageClient client) throws MessageHandlingException {
		client.received();
		int position = client.getReadBuffer().position();
		client.getReadBuffer().position(client.getReadPosition());
		int messageLength = client.getReadBuffer().getInt();
		if (messageLength <= position - 4 - client.getReadPosition()) {
			// Read message
			short typeId = client.getReadBuffer().getShort();
			Class<? extends Message> c = JGN.getMessageTypeClass(typeId); //client.getMessageClass(typeId);
			if (c == null) {
				if (client.isConnected()) {
					client.getReadBuffer().position(client.getReadPosition());
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
}
