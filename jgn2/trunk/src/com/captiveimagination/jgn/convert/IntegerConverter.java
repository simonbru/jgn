/**
 * IntegerConverter.java
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
public class IntegerConverter implements Converter {
	public void set(Message message, Method setter, ByteBuffer buffer) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		setter.invoke(message, new Object[] {new Integer(buffer.getInt())});
	}

	public void get(Message message, Method getter, ByteBuffer buffer) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		buffer.putInt(((Integer)getter.invoke(message, EMPTY_ARRAY)).intValue());
	}
}
