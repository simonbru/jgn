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

/**
 * ConversionHandlers exist to process incoming and outgoing Messages
 *
 * This class knows how to derive and use a De/Serializer for complete Messages.
 * For each Messagetype there exists one ConversionHandler.
 *
 * A Message must conform to the ubiquitous Bean conventions, eg. have a no argument
 * constructor and access it's attributes with getter/setters.
 *
 * This implementation doesn't look for the getter/setter but instead analyses the
 * fields directly (using reflection).
 *
 * All fields from the given Message class, and all Superclasses upto Message.class will
 * be collected for de/serialization, provided, the field is NOT
 * - static    (wouldn't change for a given instance)
 * - final     (cannot change anymore)
 * - transient (the author of the message didn't want to send these over the wire)
 *
 * Currently all primitive types and Strings are supported, including their Array variants.
 * enums now are also efficiently converted
 * As a last ressort, a field will be included, if it is a Serializable.
 *
 * NOTE: if, according to the rules above, a field can not be converted, it will
 * silently be excluded from the transmission.
 *
 * @author Matthew D. Hicks
 * @author Alfons Seul
 */
public class ConversionHandler {
	private static final FieldsComparator fieldComparator = new FieldsComparator();
	private static final HashMap<Class<? extends Message>, ConversionHandler> messageToHandler = new HashMap<Class<? extends Message>, ConversionHandler>();
	// Defines the mappings of classes to converters.
	private static final HashMap<Class, Converter> FIELD_CONVERTERS = new HashMap<Class, Converter>();

	static {
		FIELD_CONVERTERS.put(boolean.class, new BooleanConverter());
		FIELD_CONVERTERS.put(byte.class, new ByteConverter());
		FIELD_CONVERTERS.put(char.class, new CharacterConverter());
		FIELD_CONVERTERS.put(short.class, new ShortConverter());
		FIELD_CONVERTERS.put(int.class, new IntegerConverter());
		FIELD_CONVERTERS.put(long.class, new LongConverter());
		FIELD_CONVERTERS.put(float.class, new FloatConverter());
		FIELD_CONVERTERS.put(double.class, new DoubleConverter());

		FIELD_CONVERTERS.put(String.class, new StringConverter());
		FIELD_CONVERTERS.put(String[].class, new StringArrayConverter());

		FIELD_CONVERTERS.put(boolean[].class, new BooleanArrayConverter());
		FIELD_CONVERTERS.put(byte[].class, new ByteArrayConverter());
		FIELD_CONVERTERS.put(char[].class, new CharacterArrayConverter());
		FIELD_CONVERTERS.put(short[].class, new ShortArrayConverter());
		FIELD_CONVERTERS.put(int[].class, new IntegerArrayConverter());
		FIELD_CONVERTERS.put(long[].class, new LongArrayConverter());
		FIELD_CONVERTERS.put(float[].class, new FloatArrayConverter());
		FIELD_CONVERTERS.put(double[].class, new DoubleArrayConverter());
		FIELD_CONVERTERS.put(Serializable.class, new SerializableConverter());
    // fieldconverter for enums, note the key is a 'generic' and not the real class
		FIELD_CONVERTERS.put(Enum.class, new EnumConverter());
	}

	private Converter[] converters;
	private Field[] fields;
	private Class messageClass;

	/**
	 * define a handler for a given messageclass, if it didn't exist in the
	 * messageToHandler map before.
	 * this is private and will be called from getConversionHandler().
	 * each <fields> of <messageClass> will be served by one <converters>
	 * @param converters    one of the ./convert/xxxConverter
	 * @param fields        a java.lang.reflect.Field (that will be handled)
	 * @param messageClass  the corresponding messageclass
	 */
	private ConversionHandler(Converter[] converters, Field[] fields, Class messageClass) {
		this.converters = converters;
		this.fields = fields;
		this.messageClass = messageClass;
	}

	/**
	 * deserialize a message representation in the given Bytebuffer into a Message instance
	 * @param buffer the buffer as filled by NIO procedures
	 * @return       a Message object, setup as in the buffer
	 * @throws MessageHandlingException when there were on of following problems:
	 *         IllegalAccess on newInstance()
	 *         Instantiation no default-constructor
	 *         IllegalArgument message format corrupt
	 *         InvocationTarget init of Message failed
	 */
	public Message deserializeMessage(ByteBuffer buffer) throws MessageHandlingException {
		Message message = null;
		try {
			try {
				message = (Message)messageClass.newInstance();
			} catch(IllegalAccessException exc) { // if the class or its nullary constructor is not accessible.
				throw new MessageHandlingException("Unable to instantiate message (make sure the constructor is visible).", message, exc);
			}
			for (int i = 0; i < converters.length; i++) {
				Object res = converters[i].set(buffer);
				if (converters[i] instanceof EnumConverter) {
					Class defClass = fields[i].getType(); // the enum definition
					// defClass.getEnumConstants returns all EnumConstants, get the correct one by using the
					// serialized index
					fields[i].set(message, defClass.getEnumConstants()[(Short)res]);
				}
				else // not an enum, use directly
					fields[i].set(message, res);
			}
			return message;
		} catch (InstantiationException exc) { // if this Class represents an abstract class etc.
			throw new MessageHandlingException("Received message-type doesn't have default-constructor", message, exc);
		} catch (IllegalArgumentException exc) {
			throw new MessageHandlingException("Corrupt message-format", message, exc);
		} catch (IllegalAccessException exc) {
			throw new MessageHandlingException("Corrupt message-format", message, exc);
		} catch (InvocationTargetException exc) {
			throw new MessageHandlingException("Message crashed during initialization", message, exc);
		}
	}

	/**
	 * serialize the Message into ByteBuffer
	 * @param message THE Message to be handled
	 * @param buffer  to be used later on for processing the serialized form
	 * @param remoteMessId the messageId at the remote receiver
	 * @throws MessageHandlingException when there are problems:
	 *         IllegalArgumentException: corrupt message-format
	 *         IllegalAccessException: corrupt message-format
	 * 		     InvocationTargetException: message crashed during serialization
	 */
	public void serializeMessage(Message message, ByteBuffer buffer, short remoteMessId) throws MessageHandlingException {
		try {// Write the message type
			buffer.putShort(remoteMessId);
			for (int i = 0; i < converters.length; i++) {
				Object content = fields[i].get(message); // the field's content

				// special (indirect) handling for enum's
				if (converters[i] instanceof EnumConverter) {
					// following is (reflective) equivalent to enum.values().
					// note that Class lazyly creates and stores this for us, so access will be
					// really fast after first time.
					Object[] ec = fields[i].getType().getEnumConstants();
					int j;
					for (j = 0; j < ec.length; j++) { //get the index of the current content within the values
						if (ec[j] == content) {
							buffer.putShort((short)j); // serialize the index instead of the Class instance :_)
							break;
						}
					}
				}

				else // normal: tell the appropriate converter to store <content> into the buffer
					converters[i].get(content, buffer);
			}
		} catch (IllegalArgumentException exc) {
			throw new MessageHandlingException("Corrupt message-format", message, exc);
		} catch (IllegalAccessException exc) {
			throw new MessageHandlingException("Corrupt message-format", message, exc);
		} catch (InvocationTargetException exc) {
			throw new MessageHandlingException("Message crashed during serialization", message, exc);
		}
	}

	// TODO ase: check if synchronized is really necessary
	/**
	 * create an instance of this class, that knows how to de/serialize the given message
	 * but only, if we didn't hear from this message before. Otherwise we'll take it from cache ...
	 * @param messageClass we are interested in
	 * @return a MessageConversionHandler
	 */
	public static final synchronized ConversionHandler getConversionHandler(Class<? extends Message> messageClass) {
		ConversionHandler handler = messageToHandler.get(messageClass);

		if (handler != null) return handler;
		// Introspect Class
		ArrayList<Converter> converters = new ArrayList<Converter>();
		ArrayList<Field> fields = new ArrayList<Field>();
		ArrayList<Field> allFields = new ArrayList<Field>();
		Class c = messageClass;

		while (c != Message.class) { // stop at Message superclass !!
			Collections.addAll(allFields, c.getDeclaredFields());
			c = c.getSuperclass();
		}
    // note, sorting IS needed, because getDeclaredFields imposes no special ordering
    // so different implementations of the VM may return different orderings. But
    // we need a systematic access across all possible VM-implementation here.
		Collections.sort(allFields, fieldComparator);

		// Special circumstances handled here
		if ((UniqueMessage.class.isAssignableFrom(messageClass)) ||
				(IdentityMessage.class.isAssignableFrom(messageClass))) {
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
			if (field.isSynthetic()) continue;		      // We don't like fields we didn't write srv code for
			addField(field.getDeclaringClass(), converters, fields, field.getName(), field.getType());
		}

		// create a new handler
		handler = new ConversionHandler(
						converters.toArray(new Converter[converters.size()]),
						fields.toArray(new Field[fields.size()]),
						messageClass);
		// and store it for later reference
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
		// TODO what shall we do on error here ???
	}

	/**
	 * look for a Converter that is able to read/write a value of the given type
	 * to the wire.
	 * There are converters for each primitive types and their arrays, also
	 * enums and serializables will be supported.
	 *
	 * @param c a type we want to be handled
	 * @return an instance of convert.XxxConverter, or null if none exists
	 * @see "Converter"
	 */
	public static final Converter getConverter(Class c) {
		Converter converter = FIELD_CONVERTERS.get(c);
    if (converter == null) {
      if (c.isEnum()) // if c is an enum, take special handling
        converter = FIELD_CONVERTERS.get(Enum.class); // this is really a fake
      else if (Serializable.class.isAssignableFrom(c)) // last ressort
        converter = FIELD_CONVERTERS.get(Serializable.class); // rather ineffective, take care!
		}
    return converter;
	}

	/**
	 * we compare two Fields based on their name
	 * this will be used for sorting all the fields collected in
	 * ConversionHandler.getConversionHandler()
	 */
	private static class FieldsComparator implements Comparator<Field> {
		public int compare(Field field1, Field field2) {
			return field1.getName().compareTo(field2.getName());
		}
	}

}
