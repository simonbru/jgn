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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.captiveimagination.jgn.message.Message;

/**
 * Specifies methods for custom (de)serialization operations.<br>
 * If a developer needs (or wants) an own way of how his {@link Message} objects
 * (classes) are converted into a binary represenentation, he may register a
 * specific {@link Class} to such a message handler.<br>
 * 
 * @see JGN#setHandler(Class, MessageHandler)
 * 
 * @author Matthew D. Hicks
 */
public interface MessageHandler {

	/**
	 * Theis method extracts a {@link Message} object of the given stream.<br>
	 * 
	 * @param dis
	 *            Stream which contains a serialized message object which can be
	 *            interpreted by the implementation.<br>
	 * @return The message object contained in the stream.
	 * @throws IOException
	 *             on I/O Errors.
	 */
	public Message receiveMessage(DataInputStream dis) throws IOException;

	/**
	 * This method write the specified <code>message</code> into the given
	 * outputstream.
	 * 
	 * @param message
	 *            the message object to be serialized.
	 * @param dos
	 *            stream which receives the binary representation.
	 * @throws IOException
	 *             on I/O Errors.
	 */
	public void sendMessage(Message message, DataOutputStream dos)
			throws IOException;
}
