/*
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
 */
package com.captiveimagination.jgn;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import com.captiveimagination.jgn.compression.InvalidCompressionMethodException;
import com.captiveimagination.jgn.message.Message;

/**
 * @author Christian Laireiter
 * 
 */
public class MessageExtractor {

	protected class SourceInfo {

		private ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		private int bytesLeft = 0;

		private boolean dirty = false;

		private SocketDescriptor socketDescriptor;

		public SourceInfo(SocketDescriptor socketAddress) {
			this.socketDescriptor = socketAddress;
		}

		/**
		 * @param data
		 * @param offset
		 * @param length
		 */
		public synchronized void appendData(byte[] data, int offset, int length) {
			buffer.write(data, offset, length);
			dirty = length > 0;
		}

		protected synchronized Collection extractMessages() throws IOException,
				InvalidCompressionMethodException, IllegalArgumentException,
				InstantiationException, IllegalAccessException,
				InvocationTargetException {
			ArrayList result = new ArrayList();
			if (dirty) {
				byte[] bufferBytes = buffer.toByteArray();
				CustomByteArrayInputStream byteInput = new CustomByteArrayInputStream(
						bufferBytes, 0, bufferBytes.length);
				DataInputStream dis = new DataInputStream(byteInput);
				while (bytesLeft <= byteInput.available()) {
					/*
					 * Since javadoc of DataInputStream.readInt() declares to
					 * read only the next 4 bytes, it is save to ask for it
					 * without breaking with other environments.
					 */
					// Only read a size descriptor if bytesLeft is zero which
					// indicates
					// that there is still
					// a message left to read.
					if (bytesLeft == 0 && byteInput.available() >= 4) {
						bytesLeft = dis.readInt();
					}
					// If the length of a message is known (bytesLeft != 0) and
					// it
					// is
					// completely available
					// perform the next block
					if (bytesLeft != 0 && bytesLeft <= byteInput.available()) {
						byte[] messageBytes = new byte[bytesLeft];
						dis.readFully(messageBytes);
						messageBytes = messageServer.getCompressionHandler()
								.decompress(messageBytes);
						Message message = JGN.receiveMessage(messageBytes, 0,
								messageBytes.length, socketDescriptor.getIp(),
								socketDescriptor.getPort());
						message.setMessageServer(messageServer);
						result.add(message);
						bytesLeft = 0;
					} else {
						break;
					}
				}
				// create a new buffer
				buffer = new ByteArrayOutputStream();
				if (byteInput.available() > 0) {
					buffer.write(byteInput.getRemaining());
				}
				dirty = false;
			}
			return result;
		}
	}

	private Vector messages = new Vector();

	protected MessageServer messageServer;

	private ByteArrayOutputStream sendBuffer;

	private DataOutputStream sendBufferDos;

	private HashMap sources = new HashMap();

	public MessageExtractor(MessageServer server) {
		assert server != null;
		this.messageServer = server;
		sources = new HashMap();
		sendBuffer = new ByteArrayOutputStream();
		sendBufferDos = new DataOutputStream(sendBuffer);
	}

	public void appendData(byte[] data, int offset, int length,
			SocketDescriptor source) {
		assert source != null && data != null && data.length >= offset + length
				&& offset >= 0 && length >= 0;
		getSource(source).appendData(data, offset, length);
	}

	public int available() {
		return messages.size();
	}

	public byte[] convertMessage(Message message)
			throws IllegalArgumentException, IOException,
			IllegalAccessException, InvocationTargetException {
		sendBuffer.reset();
		byte[] messageBytes = JGN.convertMessage(message);
		messageBytes = messageServer.getCompressionHandler().compress(
				messageBytes);
		sendBufferDos.writeInt(messageBytes.length);
		sendBufferDos.write(messageBytes);
		return sendBuffer.toByteArray();
	}

	protected SourceInfo getSource(SocketDescriptor source) {
		SourceInfo result = (SourceInfo) sources.get(source);
		if (result == null) {
			result = new SourceInfo(source);
			sources.put(source, result);
		}
		return result;
	}

	public Message nextMessage() {
		return available() > 0 ? (Message) messages.remove(0) : null;
	}

	protected void updateAvailableMessages() throws IllegalArgumentException,
			IOException, InvalidCompressionMethodException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException {
		Iterator it = sources.values().iterator();
		while (it.hasNext()) {
			SourceInfo current = (SourceInfo) it.next();
			messages.addAll(current.extractMessages());
		}
	}

}
