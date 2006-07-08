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

import java.util.Vector;

import com.captiveimagination.jgn.compression.CompressionHandler;
import com.captiveimagination.jgn.compression.CompressionMethod;
import com.captiveimagination.jgn.compression.InvalidCompressionMethodException;
import com.captiveimagination.jgn.compression.LevelUnsupportedException;

/**
 * This implementation is used to select between multiple
 * {@link CompressionHandler} upon compression ratio decisions.<br>
 * 
 * 
 * @author Christian Laireiter
 */
public class SelectiveCompressionHandler implements CompressionHandler {

	/**
	 * Stores the {@link CompressionHandler} instances which are to be used for
	 * compression.<br>
	 */
	private final Vector alternatives = new Vector();

	/**
	 * This field stores the ratio of compression which approves the use of an
	 * alternative.
	 */
	private float approvalRatio = 0.25f;

	/**
	 * This field stores the amount of bytes which approves the use of an
	 * alternative.<br>
	 */
	private int approvalSize = 512 * 1024 * 1024;

	/**
	 * This compression handler will be utilized if the compression ratio does
	 * not match the excpectations.<br>
	 */
	private final CompressionHandler fallback;

	/**
	 * Creates an instance with the fallback handler
	 * {@link NoneCompressionHandler} and all std. handlers as alternatives.<br>
	 * 
	 */
	public SelectiveCompressionHandler() {
		this(new NoneCompressionHandler());
		try {
			addAlternative(new ZipCompressionHandler(5));
		} catch (LevelUnsupportedException e) {
			e.printStackTrace();
		}
		addAlternative(new GZipCompressionHandler());
	}

	/**
	 * Creates an instance.<br>
	 * 
	 * @param fallbackHandler
	 *            The compressionhandler that should be used if none of the
	 *            other matches the compression ratio excpectations.<br>
	 */
	public SelectiveCompressionHandler(CompressionHandler fallbackHandler) {
		assert fallbackHandler != null;
		this.fallback = fallbackHandler;
	}

	/**
	 * Returns <code>true</code> if the compressed size matches the rules.
	 * 
	 * @param originalLen
	 *            the size of the original data
	 * @param compressedLen
	 *            the size of the compressed data
	 * @return <code>true</code> if compression method is accepted.
	 */
	protected boolean accept(int originalLen, int compressedLen) {
		return ((originalLen - compressedLen) > approvalSize)
				|| (((float) compressedLen / originalLen) <= (1.0f - approvalRatio));
	}

	/**
	 * Adds a compression handler which should be used.<br>
	 * 
	 * @param handler
	 *            compression handler to be used.<br>
	 */
	public void addAlternative(CompressionHandler handler) {
		this.alternatives.add(handler);
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.compression.CompressionHandler#canHandle(byte[])
	 */
	public boolean canHandle(byte[] compressedData) {
		// Test fallback first
		boolean result = fallback.canHandle(compressedData);
		// ask each alternative
		for (int i = 0; i < alternatives.size() && !result; i++) {
			result = ((CompressionHandler) alternatives.get(i))
					.canHandle(compressedData);
		}
		return result;
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.compression.Compressor#compress(byte[])
	 */
	public byte[] compress(byte[] data) {
		for (int i = 0; i < alternatives.size(); i++) {
			CompressionHandler current = (CompressionHandler) alternatives
					.get(i);
			byte[] candidate = current.compress(data);
			if (accept(data.length, candidate.length)) {
				return candidate;
			}
		}
		return fallback.compress(data);
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.compression.Compressor#decompress(byte[])
	 */
	public byte[] decompress(byte[] compressedData)
			throws InvalidCompressionMethodException {
		if (!canHandle(compressedData)) {
			throw new InvalidCompressionMethodException(
					"No decompression method available for given data");
		}
		byte[] result = null;
		if (fallback.canHandle(compressedData)) {
			result = fallback.decompress(compressedData);
		} else {
			for (int i = 0; i < alternatives.size() && result == null; i++) {
				if (((CompressionHandler) alternatives.get(i))
						.canHandle(compressedData)) {
					result = ((CompressionHandler) alternatives.get(i))
							.decompress(compressedData);
				}
			}
		}
		return result;
	}

	/**
	 * Returns the number of registered {@link CompressionHandler} which have
	 * been added using {@link #addAlternative(CompressionHandler)}.
	 * 
	 * @return number of alternative {@link CompressionHandler} instances.<br>
	 */
	public int getAlternativeCount() {
		return this.alternatives.size();
	}

	/**
	 * @return the approvalRatio
	 */
	public float getApprovalRatio() {
		return this.approvalRatio;
	}

	/**
	 * @return the approvalSize
	 */
	public int getApprovalSize() {
		return this.approvalSize;
	}

	/**
	 * Returns the fallback compression handler.
	 * 
	 * @return the fallback compression handler.
	 */
	public CompressionHandler getFallBack() {
		return this.fallback;
	}

	/**
	 * (overridden) Returns the {@linkplain #fallback fallback} compression
	 * handler.<br>
	 * 
	 * @see com.captiveimagination.jgn.compression.CompressionHandler#getMethod()
	 */
	public CompressionMethod getMethod() {
		return fallback.getMethod();
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
		} catch (InvalidCompressionMethodException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Tests whether the given handler has compressed the given data.<br>
	 * 
	 * @param compressedData
	 *            the compressed date
	 * @param handler
	 *            potential handler
	 * @return <code>true</code> if handler mathches.<br>
	 */
	protected boolean isDataCompatibleTo(byte[] compressedData,
			CompressionHandler handler) {
		assert compressedData != null && handler != null;
		return handler.canHandle(compressedData);
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.MessageProcessor#outbound(byte[])
	 */
	public byte[] outbound(byte[] b) {
		return compress(b);
	}

	/**
	 * Removes the alternative handler at specified index.<br>
	 * 
	 * @param index
	 *            index of alternative compression handler (which is to be
	 *            removed).<br>
	 */
	public void removeAlternative(int index) {
		this.alternatives.remove(index);
	}

	/**
	 * @param ratio
	 *            the approvalRatio to set
	 */
	public void setApprovalRatio(float ratio) {
		this.approvalRatio = ratio;
	}

	/**
	 * @param size
	 *            the approvalSize to set
	 */
	public void setApprovalSize(int size) {
		this.approvalSize = size;
	}

}
