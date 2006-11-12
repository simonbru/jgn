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

import java.io.*;
import java.lang.reflect.*;
import java.nio.*;
import java.util.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.message.type.*;
import com.captiveimagination.jgn.test.basic.*;

/**
 * ConversionHandlers exist to process incoming and outgoing Messages
 * 
 * @author Matthew D. Hicks
 */
public class ConversionHandler {
	private static final FieldComparator fieldComparator = new FieldComparator();
	private static final HashMap<Class<? extends Message>, ConversionHandler> messageToHandler = new HashMap<Class<? extends Message>, ConversionHandler>();
	static {
		initConverters();
	}

	private Converter[] converters;
	private Field[] fields;
	private Class messageClass;

	private ConversionHandler(Converter[] converters, Field[] fields, Class messageClass) {
		this.converters = converters;
		this.fields = fields;
		this.messageClass = messageClass;
	}

	public Message receiveMessage(ByteBuffer buffer) throws MessageHandlingException {
		Message message = null;

		try {
			try {
				message = (Message)messageClass.newInstance();
			} catch(IllegalAccessException exc) {
				throw new MessageHandlingException("Unable to instantiate message (make sure the constructor is visible).", message, exc);
			}
			for (int i = 0; i < converters.length; i++) {
				fields[i].set(message, converters[i].set(buffer));
			}
			return message;
		} catch (InstantiationException exc) {
			throw new MessageHandlingException("Received message-type doesn't have default-constructor", message, exc);
		} catch (IllegalArgumentException exc) {
			throw new MessageHandlingException("Corrupt message-format", message, exc);
		} catch (IllegalAccessException exc) {
			throw new MessageHandlingException("Corrupt message-format", message, exc);
		} catch (InvocationTargetException exc) {
			throw new MessageHandlingException("Message crashed during initialization", message, exc);
		}
	}

	public void sendMessage(Message message, ByteBuffer buffer) throws MessageHandlingException {
		try {// Write the message type
			buffer.putShort(message.getMessageClient().getMessageTypeId(message.getClass()));
			for (int i = 0; i < converters.length; i++) {
				converters[i].get(fields[i].get(message), buffer);
			}
		} catch (IllegalArgumentException exc) {
			throw new MessageHandlingException("Corrupt message-format", message, exc);
		} catch (IllegalAccessException exc) {
			throw new MessageHandlingException("Corrupt message-format", message, exc);
		} catch (InvocationTargetException exc) {
			throw new MessageHandlingException("Message crashed during serialization", message, exc);
		}
	}

	public static final synchronized ConversionHandler getConversionHandler(Class<? extends Message> messageClass) {
		ConversionHandler handler = messageToHandler.get(messageClass);

		if (handler != null) return handler;
		// Introspect Class
		ArrayList<Converter> converters = new ArrayList<Converter>();
		ArrayList<Field> fields = new ArrayList<Field>();
		
		ArrayList<Field> allFields = new ArrayList<Field>();
		Class c = messageClass;
		while (c != null) {
			Collections.addAll(allFields, c.getDeclaredFields());
			c = c.getSuperclass();
		}
		Collections.sort(allFields, fieldComparator);
		
		// Special circumstances handled here
		if ((UniqueMessage.class.isAssignableFrom(messageClass)) || (IdentityMessage.class.isAssignableFrom(messageClass))) {
			// Add validation for UniqueMessage
			addField(Message.class, converters, fields, "id", long.class);
		}
		if (PlayerMessage.class.isAssignableFrom(messageClass)) {
			// Add validation for PlayerMessage
			addField(Message.class, converters, fields, "playerId", short.class);
			addField(Message.class, converters, fields, "destinationPlayerId", short.class);
		}
		if (GroupMessage.class.isAssignableFrom(messageClass)) {
			// Add validation for GroupMessage
			addField(Message.class, converters, fields, "groupId", short.class);
		}
		if (TimestampedMessage.class.isAssignableFrom(messageClass)) {
			// Add validation for TimestampedMessage
			addField(Message.class, converters, fields, "timestamp", long.class);
		}
		
		// Add standard getter/setter aspects
		for (Field field : allFields) {
			if (Modifier.isTransient(field.getModifiers())) continue;	// Make sure it's not transient
			if (Modifier.isFinal(field.getModifiers())) continue;		// If it's final we can't change it
			if (Modifier.isStatic(field.getModifiers())) continue;		// We don't want to touch static fields
			addField(field.getDeclaringClass(), converters, fields, field.getName(), field.getType());
		}

		handler = new ConversionHandler(
						converters.toArray(new Converter[converters.size()]),
						fields.toArray(new Field[fields.size()]),
						messageClass);
		messageToHandler.put(messageClass, handler);
		return handler;
	}
	
	private static final void addField(Class messageClass, ArrayList<Converter> converters, ArrayList<Field> fields, String fieldName, Class fieldClass) {
		try {
			Converter converter = getConverter(fieldClass);
			if (converter != null) {
				converters.add(converter);
				Field field = messageClass.getDeclaredField(fieldName);
				field.setAccessible(true);
				fields.add(field);
			}
		} catch(SecurityException exc) {
			exc.printStackTrace();
		} catch(NoSuchFieldException exc) {
			System.err.println("No such field \"" + fieldName + "\" on \"" + messageClass.getName() + "\".");
			exc.printStackTrace();
		}
	}

	public static final Converter getConverter(Class c) {
		Converter converter = Converter.CONVERTERS.get(c);
        if ((converter == null) && (Serializable.class.isAssignableFrom(c))) {
            converter = Converter.CONVERTERS.get(Serializable.class);
        }
        return converter;
	}
	
	public static final void initConverters() {
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
			Converter.CONVERTERS.put(String[].class, new StringArrayConverter());

			Converter.CONVERTERS.put(boolean[].class, new BooleanArrayConverter());
			Converter.CONVERTERS.put(byte[].class, new ByteArrayConverter());
			Converter.CONVERTERS.put(char[].class, new CharacterArrayConverter());
			Converter.CONVERTERS.put(short[].class, new ShortArrayConverter());
			Converter.CONVERTERS.put(int[].class, new IntegerArrayConverter());
			Converter.CONVERTERS.put(long[].class, new LongArrayConverter());
			Converter.CONVERTERS.put(float[].class, new FloatArrayConverter());
			Converter.CONVERTERS.put(double[].class, new DoubleArrayConverter());
            Converter.CONVERTERS.put(Serializable.class, new SerializableConverter());
		}
	}
}
