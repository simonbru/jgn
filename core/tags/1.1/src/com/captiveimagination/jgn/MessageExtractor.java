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

import com.captiveimagination.jgn.compression.CompressionHandler;
import com.captiveimagination.jgn.compression.InvalidCompressionMethodException;
import com.captiveimagination.jgn.message.Message;

/**
 * The purpose of this class ist to extract {@link Message} objects from a
 * binary data stream.<br>
 * <b>Background</b>:<br>
 * <br>
 * The {@linkplain MessageServer messageservers} read blocks of binary data each
 * time they access the network interfaces. These block may contain just one
 * message, a half message or even multiple message. To ease the handling of
 * such situations, this class was created.<br>
 * <br>
 * <b>Use:</b><br>
 * <br>
 * Each block of data is to be appended to the interal buffers by
 * {@link #appendData(byte[], int, int, SocketDescriptor)} where the source is
 * specified. If {@link #updateAvailableMessages()} the internal buffers are
 * searched for complete arrived messages. These are extracted and are available
 * by invoking {@link #nextMessage()}.<br>
 * 
 * @author Christian Laireiter
 */
public class MessageExtractor {

	/**
	 * This internal structure is used to provide a buffer for each known
	 * network interface.<br>
	 * It accepts binary data in the order it arrives on the network interface.
	 * However the order may not be mixed.<br>
	 * By invoking {@link #extractMessages()} the internal buffer is searched
	 * for completed messages.<br>
	 * 
	 * @author Christian Laireiter
	 */
	protected class SourceInfo {

		/**
		 * Internal buffer for this instance.
		 */
		private ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		/**
		 * If a message length has already been extracted, but the internal
		 * buffer has not the full message yet, the size is stored here.<br>
		 */
		private int bytesLeft = 0;

		/**
		 * Stores the state of the processing.<br>
		 * If <code>true</code> data has been appended to {@link #buffer} and
		 * its worth to invest time to look for possible completed messages.<br>
		 * If <code>false</code> {@link #extractMessages()} will return
		 * immediately.<br>
		 */
		private boolean dirty = false;

		/**
		 * Stores the source for this info instance.<br>
		 * Each extracted message gets its originator set, which is available
		 * from this object.
		 */
		private SocketDescriptor socketDescriptor;

		/**
		 * Creates an instance.<br>
		 * 
		 * @param socketAddress
		 *            The descriptor for the network interface to represent.
		 */
		public SourceInfo(SocketDescriptor socketAddress) {
			assert socketAddress != null;
			this.socketDescriptor = socketAddress;
		}

		/**
		 * Appends the <code>data</code> to the internal {@link #buffer}.<br>
		 * 
		 * @param data
		 *            binary chunk to append to the buffer.
		 * @param offset
		 *            offset in <code>data</code> from where to read from.
		 * @param length
		 *            amount of bytes to read from <code>data</code> and
		 *            append to the {@link #buffer}.
		 */
		public synchronized void appendData(byte[] data, int offset, int length) {
			assert data != null && offset >= 0 && length >= 0
					&& data.length > offset + length;
			buffer.write(data, offset, length);
			// Set dirty if data has been appended. (it is possible that a full
			// message has become available).
			dirty = length > 0;
		}

		/**
		 * This method searches the {@link #buffer} and tries to extract one or
		 * more messages from it.<br>
		 * 
		 * @return All extracted (available) messages. An empty collection if
		 *         none available.
		 * @throws IOException
		 *             On I/O Errors.
		 * @throws InvalidCompressionMethodException
		 *             If sender used a compression method that is not
		 *             configured for {@link MessageExtractor#messageServer}.
		 * @throws IllegalArgumentException
		 *             If compilation units of sender and receiver differs, it
		 *             can lead to this exception. (due to usage of reflection
		 *             API).
		 * @throws InstantiationException
		 *             If compilation units of sender and receiver differs, it
		 *             can lead to this exception. (due to usage of reflection
		 *             API).
		 * @throws IllegalAccessException
		 *             If compilation units of sender and receiver differs, it
		 *             can lead to this exception. (due to usage of reflection
		 *             API).
		 * @throws InvocationTargetException
		 *             If compilation units of sender and receiver differs, it
		 *             can lead to this exception. (due to usage of reflection
		 *             API).
		 */
		protected synchronized Collection extractMessages() throws IOException,
				InvalidCompressionMethodException, IllegalArgumentException,
				InstantiationException, IllegalAccessException,
				InvocationTargetException {
			ArrayList result = new ArrayList();
			// If buffer has unprocessed data...
			if (dirty) {
				// ... continue
				byte[] bufferBytes = buffer.toByteArray();
				CustomByteArrayInputStream byteInput = new CustomByteArrayInputStream(
						bufferBytes, 0, bufferBytes.length);
				DataInputStream dis = new DataInputStream(byteInput);
				// Is it possible to extract a message
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
					// it is completely available perform the next block
					if (bytesLeft != 0 && bytesLeft <= byteInput.available()) {
						byte[] messageBytes = new byte[bytesLeft];
						dis.readFully(messageBytes);
						// decompress data
						messageBytes = messageServer.getCompressionHandler()
								.decompress(messageBytes);
						// extract message object
						Message message = JGN.receiveMessage(messageBytes, 0,
								messageBytes.length, socketDescriptor.getIp(),
								socketDescriptor.getPort());
						// set message server
						message.setMessageServer(messageServer);
						// add to the result
						result.add(message);
						// Set mark for "next message expected".
						bytesLeft = 0;
					} else {
						/*
						 * No message can be extracted because there is nothing
						 * in buffer of the buffer does not contain a full
						 * message (see bytesLeft), so break the loop.
						 */
						break;
					}
				}
				// create a new buffer, discards already interpreted data.
				buffer = new ByteArrayOutputStream();
				if (byteInput.available() > 0) {
					buffer.write(byteInput.getRemaining());
				}
				// The buffer does not contain any more processable messages.
				dirty = false;
			}
			return result;
		}
	}

	/**
	 * This field is filled up with completed {@link Message} objects on each
	 * call of {@link #updateAvailableMessages()}.<br>
	 * {@link #nextMessage()} takes the first {@link Message} object and returns
	 * it.<br>
	 */
	private Vector messages = new Vector();

	/**
	 * This is the message server for which the current instance is extracting
	 * the messages.<br>
	 * For the time it is needed to get the right {@link CompressionHandler} for
	 * deflating the binary data.<br>
	 */
	protected MessageServer messageServer;

	/**
	 * A instance buffer for constructing binary chunks to be send through a
	 * network interface.<br>
	 * Used to prevent massive creation of objects and extensions of the
	 * internal byte[].
	 */
	private ByteArrayOutputStream sendBuffer;

	/**
	 * Connected to {@link #sendBuffer} to ease the writing of typed data.<br>
	 * (e.g. {@link DataOutputStream#writeInt(int)}.<br>
	 */
	private DataOutputStream sendBufferDos;

	/**
	 * This field stores {@link SourceInfo} to {@link SocketDescriptor} objects.<br>
	 */
	private HashMap sources = new HashMap();

	/**
	 * Creates an instance for the specified message server.<br>
	 * 
	 * @param server
	 *            The message server owning the instance.
	 */
	public MessageExtractor(MessageServer server) {
		assert server != null;
		this.messageServer = server;
		sources = new HashMap();
		sendBuffer = new ByteArrayOutputStream();
		sendBufferDos = new DataOutputStream(sendBuffer);
	}

	/**
	 * This method appends a data package to the internal buffers.<br>
	 * The data will be assigned to an internal buffer which is solely created
	 * to receive the data for the specified <code>source</code>.<br>
	 * 
	 * @param data
	 *            The array containing the receifed data.
	 * @param offset
	 *            the offset where the current package starts in
	 *            <code>data</code>
	 * @param length
	 *            The amount of byte to take from the specified offset.
	 * @param source
	 *            The descriptor specifiyng the source. (Who send this data).
	 */
	public void appendData(byte[] data, int offset, int length,
			SocketDescriptor source) {
		assert source != null && data != null && data.length >= offset + length
				&& offset >= 0 && length >= 0;
		getSource(source).appendData(data, offset, length);
	}

	/**
	 * Returns the amount of already extracted and available
	 * {@linkplain Message messages}.<br>
	 * 
	 * @see #nextMessage()
	 * @return the number of available messages.
	 */
	public int available() {
		return messages.size();
	}

	/**
	 * This method is for convenience. It allows to convert a {@link Message}
	 * into a binary form to be send over a network interface.<br>
	 * If this method is used to create the binary data, the developer won't
	 * have to know the internal fromat and needs for JGN's message conversion.<br>
	 * <br>
	 * <b>Internal:</b><br>
	 * <br>
	 * The given message is first converted into a <code>byte[]</code> by
	 * {@link JGN#convertMessage(Message)}.<br>
	 * After that it is compressed by the message server's
	 * {@linkplain MessageServer#getCompressionHandler() compression handler}
	 * The resulting <code>byte[]</code> consits of a 4 byte integer
	 * representation which stores the length of the prior created
	 * <code>byte[]</code> followed by this data itself.<br>
	 * 
	 * @param message
	 *            The message to convert.
	 * @return The binary and compressed (encrypted) data package representation
	 *         of this message.<br>
	 * @throws IllegalArgumentException
	 *             If compilation units of sender and receiver differs, it can
	 *             lead to this exception. (due to usage of reflection API).
	 * @throws IOException
	 *             On I/O Errors
	 * @throws IllegalAccessException
	 *             If compilation units of sender and receiver differs, it can
	 *             lead to this exception. (due to usage of reflection API).
	 * @throws InvocationTargetException
	 *             If compilation units of sender and receiver differs, it can
	 *             an lead to this exception. (due to usage of reflection API).
	 */
	public byte[] convertMessage(Message message)
			throws IllegalArgumentException, IOException,
			IllegalAccessException, InvocationTargetException {
		// Clear buffer
		sendBuffer.reset();
		// Use JGN facility to create binary representation.
		byte[] messageBytes = JGN.convertMessage(message);
		// Compress the data
		messageBytes = messageServer.getCompressionHandler().compress(
				messageBytes);
		// write the length of the message's binary representation
		sendBufferDos.writeInt(messageBytes.length);
		// write the binary message itself
		sendBufferDos.write(messageBytes);
		// Return the chunk
		return sendBuffer.toByteArray();
	}

	/**
	 * Internal convenience method for getting a {@link SourceInfo} for the
	 * specified <code>source</code>.<br>
	 * 
	 * @param source
	 *            The network source for wich a info object should be obtained.
	 * @return {@link SourceInfo} for the specified <code>source</code>
	 */
	protected SourceInfo getSource(SocketDescriptor source) {
		SourceInfo result = (SourceInfo) sources.get(source);
		if (result == null) {
			result = new SourceInfo(source);
			sources.put(source, result);
		}
		return result;
	}

	/**
	 * This method returns the next available (already extracted) message.<br>
	 * The method does not block.<br>
	 * The source (sender) of the message that comes next is relatively random.
	 * 
	 * @return Next message if available, else <code>null</code>.
	 */
	public Message nextMessage() {
		return available() > 0 ? (Message) messages.remove(0) : null;
	}

	/**
	 * This method will process the internal buffers and extract completed
	 * message chunks.<br>
	 * 
	 * @throws IllegalArgumentException
	 *             If compilation units of sender and receiver differs, it can
	 *             lead to this exception. (due to usage of reflection API).
	 * @throws IOException
	 *             On I/O Errors
	 * @throws InvalidCompressionMethodException
	 *             If the compression method used by the sender is cannot be
	 *             handled by the {@link #messageServer}'s compression handler.
	 * @throws InstantiationException
	 *             If compilation units of sender and receiver differs, it can
	 *             lead to this exception. (due to usage of reflection API).
	 * @throws IllegalAccessException
	 *             If compilation units of sender and receiver differs, it can
	 *             lead to this exception. (due to usage of reflection API).
	 * @throws InvocationTargetException
	 *             If compilation units of sender and receiver differs, it can
	 *             lead to this exception. (due to usage of reflection API).
	 */
	public void updateAvailableMessages() throws IllegalArgumentException,
			IOException, InvalidCompressionMethodException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException {
		// Iterate through all SourceInfo
		Iterator it = sources.values().iterator();
		while (it.hasNext()) {
			SourceInfo current = (SourceInfo) it.next();
			// Extract available messages
			/*
			 * extractMessages() will return a empty collection if no message is
			 * available, so its safe to always call addAll(..)
			 */
			messages.addAll(current.extractMessages());
		}
	}

}
