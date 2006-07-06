/**
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
 *
 * Created: Jun 3, 2006
 */
package com.captiveimagination.jgn.convert;

import java.lang.reflect.*;
import java.nio.*;
import java.util.*;

import com.captiveimagination.jgn.message.*;

/**
 * Converter defines the methods necessary to convert from a ByteBuffer
 * and apply the data to a Message as well as from a Message and apply
 * it to a ByteBuffer.
 * 
 * @author Matthew D. Hicks
 */
public interface Converter {
	/**
	 * Represents an empty byte array with a length of 0.
	 */
	public static final Object[] EMPTY_ARRAY = new Object[0];

	/**
	 * Defines the mappings of classes to converters.
	 */
	public static final HashMap<Class, Converter> CONVERTERS = new HashMap<Class, Converter>();

	/**
	 * Reads content from <code>buffer</code> and applies it via
	 * <code>setter</code> to <code>message</code>.
	 * 
	 * @param message
	 * @param setter
	 * @param buffer
	 */
	public void set(Message message, Method setter, ByteBuffer buffer) throws IllegalArgumentException,
					IllegalAccessException, InvocationTargetException;

	/**
	 * Reads content from <code>message</code> via <code>getter</code>
	 * and applies it to <code>buffer</code>.
	 * 
	 * @param message
	 * @param getter
	 * @param buffer
	 */
	public void get(Message message, Method getter, ByteBuffer buffer) throws IllegalArgumentException,
					IllegalAccessException, InvocationTargetException;
}
