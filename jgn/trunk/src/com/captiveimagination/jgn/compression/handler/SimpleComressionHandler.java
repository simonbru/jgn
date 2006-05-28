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
package com.captiveimagination.jgn.compression.handler;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import com.captiveimagination.jgn.compression.CompressionHandler;
import com.captiveimagination.jgn.compression.CompressionMethod;
import com.captiveimagination.jgn.compression.InvalidCompressionMethodException;

/**
 * This simple class is used for creating a {@link CompressionHandler} which is
 * directly connected to a single {@link CompressionMethod}.<br>
 * 
 * @author Christian Laireiter
 */
public class SimpleComressionHandler implements CompressionHandler {

	/**
	 * This instance is used for compression.
	 */
	private final CompressionMethod method;

	/**
	 * Stores the {@link CompressionMethod#getMethod()} as a byte array.<br>
	 */
	private final byte[] methodId;

	/**
	 * Creates an isntance which uses the given Method for compressing data.
	 * 
	 * @param compressionMethod
	 *            Method to use.
	 */
	public SimpleComressionHandler(CompressionMethod compressionMethod) {
		assert compressionMethod != null;
		this.method = compressionMethod;
		try {
			methodId = this.method.getMethod().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			// Should not happen
			e.printStackTrace();
			/*
			 * However if this happens, throw this runtime exception.
			 */
			throw new IllegalStateException("UTF-8 encoding must be available");
		}
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.compression.CompressionHandler#canHandle(byte[])
	 */
	public boolean canHandle(byte[] compressedData) {
		boolean result = methodId.length < compressedData.length;
		for (int i = 0; i < methodId.length && result; i++) {
			result = compressedData[i] == methodId[i];
		}
		return result;
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.compression.Compressor#compress(byte[])
	 */
	public byte[] compress(byte[] data) {
		byte[] compressed = this.method.compress(data);
		byte[] result = new byte[methodId.length + compressed.length];
		System.arraycopy(this.methodId, 0, result, 0, methodId.length);
		System.arraycopy(compressed, 0, result, methodId.length,
				compressed.length);
		return result;
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.compression.Compressor#decompress(byte[])
	 */
	public byte[] decompress(byte[] compressedData)
			throws InvalidCompressionMethodException {
		if (compressedData.length <= methodId.length) {
			throw new InvalidCompressionMethodException(
					"Given data is smaller than the identifier of this method.");
		}
		byte[] identifier = new byte[this.methodId.length];
		System
				.arraycopy(compressedData, 0, identifier, 0,
						this.methodId.length);
		if (!Arrays.equals(identifier, this.methodId)) {
			throw new InvalidCompressionMethodException(
					"Method identifiers do not match.");
		}
		byte[] realData = new byte[compressedData.length - methodId.length];
		System.arraycopy(compressedData, methodId.length, realData, 0,
				realData.length);
		return this.method.decompress(realData);
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.compression.CompressionHandler#getMethod()
	 */
	public CompressionMethod getMethod() {
		return this.method;
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.MessageProcessor#getPriority()
	 */
	public short getPriority() {
		return 1000;
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.MessageProcessor#inbound(byte[])
	 */
	public byte[] inbound(byte[] b) {
		try {
			return decompress(b);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.MessageProcessor#outbound(byte[])
	 */
	public byte[] outbound(byte[] b) {
		return compress(b);
	}

}
