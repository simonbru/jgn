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
package com.captiveimagination.jgn.core.compression.methods;

import java.io.UnsupportedEncodingException;

import com.captiveimagination.jgn.core.compression.CompressionMethod;
import com.captiveimagination.jgn.core.compression.InvalidCompressionMethodException;
import com.captiveimagination.jgn.core.compression.InvalidPropertyValueException;
import com.captiveimagination.jgn.core.compression.LevelUnsupportedException;
import com.captiveimagination.jgn.core.compression.UnknownPropertyException;

/**
 * This implementation performs no compression at all.<br>
 * 
 * @author Christian Laireiter
 */
public class NoneMethod implements CompressionMethod {

	/**
	 * This property specifies whether the {@link #compress(byte[])} and
	 * {@link #decompress(byte[])} methods should create no copy of the given
	 * data.<br>
	 * Value class for this property is {@link Boolean}.<br>
	 * If value is set to {@link Boolean#TRUE}, the methods directly return the
	 * input.<br>
	 * If value is set to {@link Boolean#FALSE}, the methods return a copy of
	 * the input. So the original data array can be manipulated safely.<br>
	 */
	public final static String PROPERTY_NOCOPY = "nocopy";

	/**
	 * Store the falue for {@link #PROPERTY_NOCOPY}.<br>
	 */
	private boolean flag_nocopy = false;

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.core.compression.Compressor#compress(byte[])
	 */
	public byte[] compress(byte[] data) {
		if (flag_nocopy) {
			return data;
		}
		byte[] copy = new byte[data.length];
		System.arraycopy(data, 0, copy, 0, copy.length);
		return copy;
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.core.compression.Compressor#decompress(byte[])
	 */
	public byte[] decompress(byte[] compressedData)
			throws InvalidCompressionMethodException {
		if (flag_nocopy) {
			return compressedData;
		}
		byte[] copy = new byte[compressedData.length];
		System.arraycopy(compressedData, 0, copy, 0, copy.length);
		return copy;
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.core.compression.CompressionMethod#getCompressionLevel()
	 */
	public int getCompressionLevel() {
		return 0;
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.core.compression.CompressionMethod#getMaximumCompressionLevel()
	 */
	public int getMaximumCompressionLevel() {
		return 0;
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.core.compression.CompressionMethod#getMethod()
	 */
	public String getMethod() {
		return "none";
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.core.compression.CompressionMethod#getMethodId()
	 */
	public byte[] getMethodId() {
		try {
			return getMethod().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.core.compression.CompressionMethod#getProperty(java.lang.String)
	 */
	public Object getProperty(String property) throws UnknownPropertyException {
		if (PROPERTY_NOCOPY.equals(property)) {
			return Boolean.valueOf(flag_nocopy);
		}
		throw new UnknownPropertyException(property, this);
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.core.compression.CompressionMethod#setCompressionLevel(int)
	 */
	public void setCompressionLevel(int level) throws LevelUnsupportedException {
		if (level != 0) {
			throw new LevelUnsupportedException(level, this, null);
		}
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.core.compression.CompressionMethod#setProperty(java.lang.String,
	 *      java.lang.Object)
	 */
	public void setProperty(String property, Object value)
			throws UnknownPropertyException, InvalidPropertyValueException {
		if (PROPERTY_NOCOPY.equals(property)) {
			if (!(value instanceof Boolean)) {
				throw new InvalidPropertyValueException();
			}
			flag_nocopy = Boolean.TRUE.equals(value);
		} else
			throw new UnknownPropertyException(property, this);
	}

}
