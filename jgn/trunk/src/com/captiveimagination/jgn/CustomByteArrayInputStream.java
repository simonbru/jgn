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

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * This class is a direct extension to {@link ByteArrayInputStream} and provides
 * additional functionality.<br>
 * <br>
 * Features:<br>
 * <ol>
 * <li>The current position of the stream in wrapped <code>byte[]</code> ({@link #getPosition()})</li>
 * <li>Get the remaining bytes from current position ({@link #getRemaining()})</li>
 * </ol>
 * <br>
 * 
 * @author Matthew D. Hicks
 */
public class CustomByteArrayInputStream extends ByteArrayInputStream {

	/**
	 * Creates a <code>ByteArrayInputStream</code> so that it uses
	 * <code>buf</code> as its buffer array. The buffer array is not copied.
	 * The initial value of <code>pos</code> is <code>0</code> and the
	 * initial value of <code>count</code> is the length of <code>buf</code>.
	 * 
	 * @param buffer
	 *            the input buffer.
	 */
	public CustomByteArrayInputStream(byte[] buffer) {
		super(buffer);
	}

	/**
	 * Creates <code>ByteArrayInputStream</code> that uses <code>buf</code>
	 * as its buffer array. The initial value of <code>pos</code> is
	 * <code>offset</code> and the initial value of <code>count</code> is
	 * the minimum of <code>offset+length</code> and <code>buf.length</code>.
	 * The buffer array is not copied. The buffer's mark is set to the specified
	 * offset.
	 * 
	 * @param buffer
	 *            the input buffer.
	 * @param offset
	 *            the offset in the buffer of the first byte to read.
	 * @param length
	 *            the maximum number of bytes to read from the buffer.
	 */
	public CustomByteArrayInputStream(byte[] buffer, int offset, int length) {
		super(buffer, offset, length);
	}

	/**
	 * Returns the current readindex in internal wrapped byte[].<br>
	 * 
	 * @return position of next byte to read in wrapped buffer
	 */
	public int getPosition() {
		return pos;
	}

	/**
	 * This method returns the remaining byte from current read position (to the
	 * end of buffer).<br>
	 * 
	 * @return remaining bytes.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public byte[] getRemaining() throws IOException {
		byte[] remaining = new byte[available()];
		read(remaining);
		return remaining;
	}
}
