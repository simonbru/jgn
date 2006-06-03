/**
 * ConversionHandler.java
 *
 * Created: Jun 3, 2006
 */
package com.captiveimagination.jgn.convert;

import java.io.*;
import java.lang.reflect.*;
import java.nio.*;
import java.util.*;

import com.captiveimagination.jgn.*;

/**
 * ConversionHandlers exist to process incoming
 * and outgoing Messages
 * 
 * @author Matthew D. Hicks
 */
public class ConversionHandler {
	private static final MethodComparator methodComparator = new MethodComparator();
	
	private Converter[] converters;
	private Method[] getters;
	private Method[] setters;
	private Class messageClass;
	
	private ConversionHandler(Converter[] converters, Method[] getters, Method[] setters, Class messageClass) {
		this.converters = converters;
		this.getters = getters;
		this.setters = setters;
		this.messageClass = messageClass;
	}
	
	public Message receiveMessage(ByteBuffer buffer) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException, SecurityException, NoSuchMethodException {
		Message message = (Message)messageClass.newInstance();
		for (int i = 0; i < converters.length; i++) {
			converters[i].set(message, setters[i], buffer);
		}
		return message;
	}
	
	public void sendMessage(Message message, ByteBuffer buffer) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, IOException {
		for (int i = 0; i < converters.length; i++) {
			converters[i].get(message, getters[i], buffer);
		}
	}
	
	public static final ConversionHandler getConversionHandler(Class messageClass) {
		initConverters();
		
		// Introspect Class
		ArrayList<Converter> converters = new ArrayList<Converter>();
		ArrayList<Method> getters = new ArrayList<Method>();
		ArrayList<Method> setters = new ArrayList<Method>();
		Method[] ms = messageClass.getMethods();
		ArrayList<Method> methods = new ArrayList<Method>();
		Collections.addAll(methods, ms);
		Collections.sort(methods, methodComparator);
		for (Method getter : methods) {
			if (!getter.getName().startsWith("get")) continue;
			
			String name = getter.getName().substring(3);
			Method setter = null;
			for (Method m : methods) {
				if (m.getName().equals("set" + name)) {
					if ((m.getParameterTypes().length == 1) && (m.getParameterTypes()[0] == getter.getReturnType())) {
						setter = m;
						break;
					}
				}
			}
			
			if (setter == null) continue;
			
			Converter converter = Converter.CONVERTERS.get(getter.getReturnType());
			if (converter != null) {
				converters.add(converter);
				getter.setAccessible(true);
				setter.setAccessible(true);
				getters.add(getter);
				setters.add(setter);
			}
		}
		
		return new ConversionHandler(converters.toArray(new Converter[converters.size()]), getters.toArray(new Method[getters.size()]), setters.toArray(new Method[setters.size()]), messageClass);
	}
	
	private static final void initConverters() {
		if (Converter.CONVERTERS.size() == 0) {
			Converter.CONVERTERS.put(boolean.class, new BooleanConverter());
			Converter.CONVERTERS.put(byte.class, new ByteConverter());
			Converter.CONVERTERS.put(char.class, new CharacterConverter());
			Converter.CONVERTERS.put(short.class, new ShortConverter());
			Converter.CONVERTERS.put(int.class, new IntegerConverter());
			Converter.CONVERTERS.put(long.class, new LongConverter());
			Converter.CONVERTERS.put(float.class, new FloatConverter());
			Converter.CONVERTERS.put(double.class, new DoubleConverter());
			
			Converter.CONVERTERS.put(String.class, new StringConverter());
			
			Converter.CONVERTERS.put(boolean[].class, new BooleanArrayConverter());
			Converter.CONVERTERS.put(byte[].class, new ByteArrayConverter());
			Converter.CONVERTERS.put(char[].class, new CharacterArrayConverter());
			Converter.CONVERTERS.put(short[].class, new ShortArrayConverter());
			Converter.CONVERTERS.put(int[].class, new IntegerArrayConverter());
			Converter.CONVERTERS.put(long[].class, new LongArrayConverter());
			Converter.CONVERTERS.put(float[].class, new FloatArrayConverter());
			Converter.CONVERTERS.put(double[].class, new DoubleArrayConverter());
		}
	}
}
