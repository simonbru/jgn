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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.captiveimagination.jgn.core.compression.CompressionMethod;
import com.captiveimagination.jgn.core.compression.InvalidCompressionMethodException;
import com.captiveimagination.jgn.core.compression.InvalidPropertyValueException;
import com.captiveimagination.jgn.core.compression.LevelUnsupportedException;
import com.captiveimagination.jgn.core.compression.UnknownPropertyException;

/**
 * This implementation uses the &quot;Huffman&quot; algorithm for compressing
 * data.<br>
 * <b>Hint:</b><br>
 * This class uses {@link ZipOutputStream} and {@link ZipInputStream} for
 * actually compressing /decompressing data. However, these implementations are
 * utilizing {@link Deflater} for compression. So
 * {@link #getMaximumCompressionLevel()} returns
 * {@link Deflater#BEST_COMPRESSION}
 * 
 * @author Christian Laireiter
 */
public class ZipMethod implements CompressionMethod {

	/**
	 * The name for {@link ZipEntry} objects compressed using this class.
	 */
	public final static String ENTRY_NAME = "entry";

	/**
	 * Stores the compression level to be used by {@link ZipOutputStream}.<br>
	 */
	int level;

	/**
	 * Creats an instance.<br>
	 */
	public ZipMethod() {
		level = 1;
	}

	/**
	 * Creates an instance.
	 * 
	 * @param compressionLevel
	 *            initial compression level to use.
	 * @throws LevelUnsupportedException
	 *             If specified level is not available.<br>
	 */
	public ZipMethod(int compressionLevel) throws LevelUnsupportedException {
		setCompressionLevel(compressionLevel);
	}

	/**
	 * (overridden)
	 * 
	 * @see com.captiveimagination.jgn.core.compression.Compressor#compress(byte[])
	 */
	public byte[] compress(byte[] data) {
		byte[] result = null;
		ZipEntry entry = new ZipEntry(ENTRY_NAME);
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ZipOutputStream zos = new ZipOutputStream(bos);
			zos.setLevel(level);
			zos.putNextEntry(entry);
			zos.write(data);
			zos.flush();
			zos.closeEntry();
			zos.close();
			result = bos.toByteArray();
		} catch (IOException e) {
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
			ByteArrayInputStream bis = new ByteArrayInputStream(compressedData);
			ZipInputStream zis = new ZipInputStream(bis);
			ZipEntry nextEntry = zis.getNextEntry();
			if (!nextEntry.getName().equals(ENTRY_NAME)) {
				throw new InvalidCompressionMethodException();
			}
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] tmp = new byte[8192];
			while (zis.available() > 0) {
				int read = zis.read(tmp);
				if (read > 0) {
					bos.write(tmp, 0, read);
				}
			}
			zis.closeEntry();
			zis.close();
			bos.flush();
			result = bos.toByteArray();
		} catch (IOException e) {
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
		return Deflater.BEST_COMPRESSION;
	}

	/**
	 * ENTRY_NAME (overridden)
	 * 
	 * @see com.captiveimagination.jgn.core.compression.CompressionMethod#getMethod()
	 */
	public String getMethod() {
		return "zip";
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
		if (compressionLevel > Deflater.BEST_COMPRESSION
				|| compressionLevel < 0) {
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
