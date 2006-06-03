/**
 * BooleanConverter.java
 *
 * Created: Jun 3, 2006
 */
package com.captiveimagination.jgn.convert;

import java.lang.reflect.*;
import java.nio.*;

import com.captiveimagination.jgn.*;

/**
 * @author Matthew D. Hicks
 */
public class BooleanConverter implements Converter {
	public void set(Message message, Method setter, ByteBuffer buffer) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		setter.invoke(message, new Object[] {new Boolean(buffer.get() == 1)});
	}

	public void get(Message message, Method getter, ByteBuffer buffer) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		buffer.put(((Boolean)getter.invoke(message, EMPTY_ARRAY)).booleanValue() ? (byte)1 : (byte)0);
	}
}
