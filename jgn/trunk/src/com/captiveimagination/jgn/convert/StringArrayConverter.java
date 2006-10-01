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
 * Created: Jun 7, 2006
 */
package com.captiveimagination.jgn.convert;

import java.io.*;
import java.lang.reflect.*;
import java.nio.*;

/**
 * @author Matthew D. Hicks
 */
public class StringArrayConverter implements Converter {
	public Object set(Object object, Method setter, ByteBuffer buffer) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		int length = buffer.getInt();
		String[] array = null;
		if (length != -1) {
			array = new String[length];
			byte[] b;
			for (int i = 0; i < length; i++) {
				int l = buffer.getInt();
				if (l == -1) continue;
				b = new byte[l];
				buffer.get(b);
				try {
					array[i] = new String(b, "UTF-8");
				} catch (UnsupportedEncodingException exc) {
					// never happens
					exc.printStackTrace(); // just be paranoid
				}
			}
		}
		if (setter != null) setter.invoke(object, new Object[] {array});
		return array;
	}

	public void get(Object object, Method getter, ByteBuffer buffer) throws IllegalArgumentException,
					IllegalAccessException, InvocationTargetException {
		String[] array = (String[])getter.invoke(object, EMPTY_ARRAY);
		if (array == null) {
			buffer.putInt(-1);
		} else {
			buffer.putInt(array.length);
			for (String s : array) {
				//buffer.put(b);
				if (s == null) {
					buffer.putInt(-1);
				} else {
					try {
						byte[] b = s.getBytes("UTF-8");
						buffer.putInt(b.length);
						buffer.put(b);
					} catch (UnsupportedEncodingException exc) {
						// never happens
						exc.printStackTrace(); // just be paranoid
					}
				}
			}
		}
	}

}
