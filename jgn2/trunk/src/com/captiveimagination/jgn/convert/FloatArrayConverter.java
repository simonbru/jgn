/**
 * FloatArrayConverter.java
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
public class FloatArrayConverter implements Converter {
	public void set(Message message, Method setter, ByteBuffer buffer) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		int length = buffer.getInt();
		float[] array = null;
		if (length != -1) {
			array = new float[length];
			for (int i = 0; i < length; i++) {
				array[i] = buffer.getFloat();
			}
		}
		setter.invoke(message, new Object[] {array});
	}

	public void get(Message message, Method getter, ByteBuffer buffer) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		float[] array = (float[])getter.invoke(message, EMPTY_ARRAY);
		if (array == null) {
			buffer.putInt(-1);
		} else {
			buffer.putInt(array.length);
			for (float b : array) {
				buffer.putFloat(b);
			}
		}
	}
}
