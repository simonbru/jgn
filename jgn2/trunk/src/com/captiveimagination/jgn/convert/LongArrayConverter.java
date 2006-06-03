/**
 * LongArrayConverter.java
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
public class LongArrayConverter implements Converter {
	public void set(Message message, Method setter, ByteBuffer buffer) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		int length = buffer.getInt();
		long[] array = null;
		if (length != -1) {
			array = new long[length];
			for (int i = 0; i < length; i++) {
				array[i] = buffer.getLong();
			}
		}
		setter.invoke(message, new Object[] {array});
	}

	public void get(Message message, Method getter, ByteBuffer buffer) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		long[] array = (long[])getter.invoke(message, EMPTY_ARRAY);
		if (array == null) {
			buffer.putInt(-1);
		} else {
			buffer.putInt(array.length);
			for (long b : array) {
				buffer.putLong(b);
			}
		}
	}
}
