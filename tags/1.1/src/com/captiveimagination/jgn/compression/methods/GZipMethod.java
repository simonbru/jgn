/*
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
package com.captiveimagination.jgn.compression.methods;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.captiveimagination.jgn.compression.CompressionMethod;
import com.captiveimagination.jgn.compression.InvalidCompressionMethodException;
import com.captiveimagination.jgn.compression.InvalidPropertyValueException;
import com.captiveimagination.jgn.compression.LevelUnsupportedException;
import com.captiveimagination.jgn.compression.UnknownPropertyException;

/**
 * This method uses {@link GZIPInputStream} and {@link GZIPOutputStream} for
 * compressing data.<br>
 * 
 * @author Christian Laireiter
 */
public class GZipMethod implements CompressionMethod {

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.compression.Compressor#compress(byte[])
	 */
	public byte[] compress(byte[] data) {
		byte[] result = null;
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			GZIPOutputStream gos = new GZIPOutputStream(bos);
			gos.write(data);
			gos.flush();
			gos.finish();
			gos.close();
			result = bos.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.compression.Compressor#decompress(byte[])
	 */
	public byte[] decompress(byte[] compressedData)
			throws InvalidCompressionMethodException {
		byte[] result = null;
		try {
			GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(
					compressedData));
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] tmp = new byte[8192];
			int read;
			while (gis.available() > 0 && ((read = gis.read(tmp)) > 0)) {
				bos.write(tmp, 0, read);
			}
			result = bos.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.compression.CompressionMethod#getCompressionLevel()
	 */
	public int getCompressionLevel() {
		return 0;
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.compression.CompressionMethod#getMaximumCompressionLevel()
	 */
	public int getMaximumCompressionLevel() {
		return 0;
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.compression.CompressionMethod#getMethod()
	 */
	public String getMethod() {
		return "gzip";
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.compression.CompressionMethod#getMethodId()
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
	 * @see com.captiveimagination.jgn.compression.CompressionMethod#getProperty(java.lang.String)
	 */
	public Object getProperty(String property) throws UnknownPropertyException {
		throw new UnknownPropertyException(property, this);
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.compression.CompressionMethod#setCompressionLevel(int)
	 */
	public void setCompressionLevel(int level) throws LevelUnsupportedException {
		if (level != 0) {
			throw new LevelUnsupportedException(level, this, null);
		}
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.compression.CompressionMethod#setProperty(java.lang.String,
	 *      java.lang.Object)
	 */
	public void setProperty(String property, Object value)
			throws UnknownPropertyException, InvalidPropertyValueException {
		throw new UnknownPropertyException(property, this);
	}

}
