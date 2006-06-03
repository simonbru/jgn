/**
 * Converter.java
 *
 * Created: Jun 2, 2006
 */
package com.captiveimagination.jgn.convert;

import java.io.*;
import java.lang.reflect.*;
import java.nio.*;
import java.util.*;

import com.captiveimagination.jgn.*;

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
	public static final byte[] EMPTY_ARRAY = new byte[0];
	
	/**
	 * Defines the mappings of classes to converters.
	 */
	public static final HashMap<Class,Converter> CONVERTERS = new HashMap<Class,Converter>();
	
	/**
	 * Reads content from <code>buffer</code> and applies it via
	 * <code>setter</code> to <code>message</code>.
	 * 
	 * @param message
	 * @param setter
	 * @param buffer
	 */
	public void set(Message message, Method setter, ByteBuffer buffer) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, IOException;
	
	/**
	 * Reads content from <code>message</code> via <code>getter</code>
	 * and applies it to <code>buffer</code>.
	 * 
	 * @param message
	 * @param getter
	 * @param buffer
	 */
	public void get(Message message, Method getter, ByteBuffer buffer) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, IOException;
}
