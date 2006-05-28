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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;

import com.captiveimagination.jgn.core.compression.CompressionMethod;
import com.captiveimagination.jgn.core.compression.InvalidCompressionMethodException;
import com.captiveimagination.jgn.core.compression.InvalidPropertyValueException;
import com.captiveimagination.jgn.core.compression.LevelUnsupportedException;
import com.captiveimagination.jgn.core.compression.UnknownPropertyException;

/**
 * Implememntation which uses &quot;JZLib&quot;.<br>
 * 
 * @author Christian Laireiter
 */
public class ZMethod implements CompressionMethod {

	/**
	 * <code>true</code> if library &quot;JZLib&quot; is available.<br>
	 */
	public final static boolean AVAILABLE;

	/**
	 * Value of {@link JZlib#Z_DEFAULT_COMPRESSION} if library is available.<br>
	 */
	public final static int DEFAULT_LEVEL;

	/**
	 * Value of {@link JZlib#Z_BEST_COMPRESSION} if library is available.<br>
	 */
	public final static int MAX_LEVEL;

	/**
	 * Stores the constructor {@link ZInputStream#ZInputStream(InputStream)}.<br>
	 */
	public final static Constructor ZIN;

	/**
	 * Stores the constructor
	 * {@link ZOutputStream#ZOutputStream(OutputStream, int)}<br>
	 */
	private final static Constructor ZOUT;

	static {
		int defLvl = 0;
		int mxLvl = 0;
		boolean avail = false;
		Constructor zin, zout;
		try {
			Class jzlib = Class.forName("com.jcraft.jzlib.JZlib");
			defLvl = jzlib.getField("Z_DEFAULT_COMPRESSION").getInt(null);
			mxLvl = jzlib.getField("Z_BEST_COMPRESSION").getInt(null);
			zin = Class.forName("com.jcraft.jzlib.ZInputStream")
					.getConstructor(new Class[] { InputStream.class });
			zout = Class.forName("com.jcraft.jzlib.ZOutputStream")
					.getConstructor(
							new Class[] { OutputStream.class, int.class });
			avail = true;
		} catch (Exception e) {
			e.printStackTrace();
			defLvl = 0;
			mxLvl = 0;
			zin = null;
			zout = null;
		}
		DEFAULT_LEVEL = defLvl;
		MAX_LEVEL = mxLvl;
		AVAILABLE = avail;
		ZIN = zin;
		ZOUT = zout;
	}

	/**
	 * The current compression level.<br>
	 */
	int level = DEFAULT_LEVEL;

	/**
	 * Creates an instance.<br>
	 * 
	 * @throws IllegalStateException
	 *             If {@link JZlib}, {@link ZOutputStream} or
	 *             {@link ZInputStream} are not available in current classpath.<br>
	 */
	public ZMethod() throws IllegalStateException {
		if (!AVAILABLE) {
			throw new IllegalStateException(
					"JZLib is not available in current classpath (\"com.jcraft.jzlib.JZlib\")");
		}
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.core.compression.Compressor#compress(byte[])
	 */
	public byte[] compress(byte[] data) {
		byte[] result = null;
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			OutputStream fos = (OutputStream) ZOUT.newInstance(new Object[] {
					bos, new Integer(level) });
			fos.write(data);
			fos.flush();
			fos.close();
			result = bos.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.core.compression.Compressor#decompress(byte[])
	 */
	public byte[] decompress(byte[] compressedData)
			throws InvalidCompressionMethodException {
		byte[] result = null;
		try {
			InputStream fis = (InputStream) ZIN
					.newInstance(new Object[] { new ByteArrayInputStream(
							compressedData) });
			byte[] tmp = new byte[8192];
			int read;
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			while (fis.available() > 0 && ((read = fis.read(tmp)) > 0)) {
				bos.write(tmp, 0, read);
			}
			fis.close();
			result = bos.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.core.compression.CompressionMethod#getCompressionLevel()
	 */
	public int getCompressionLevel() {
		return level;
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.core.compression.CompressionMethod#getMaximumCompressionLevel()
	 */
	public int getMaximumCompressionLevel() {
		return MAX_LEVEL;
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.core.compression.CompressionMethod#getMethod()
	 */
	public String getMethod() {
		return "jzlib";
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
		throw new UnknownPropertyException(property, this);
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.core.compression.CompressionMethod#setCompressionLevel(int)
	 */
	public void setCompressionLevel(int compressionLevel)
			throws LevelUnsupportedException {
		if (compressionLevel < 0
				|| compressionLevel > getMaximumCompressionLevel()) {
			throw new LevelUnsupportedException(compressionLevel, this, null);
		}
		this.level = compressionLevel;
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.core.compression.CompressionMethod#setProperty(java.lang.String,
	 *      java.lang.Object)
	 */
	public void setProperty(String property, Object value)
			throws UnknownPropertyException, InvalidPropertyValueException {
		throw new UnknownPropertyException(property, this);
	}

}
