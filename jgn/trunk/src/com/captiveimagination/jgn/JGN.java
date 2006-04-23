/*
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
 */
package com.captiveimagination.jgn;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.zip.*;

import com.captiveimagination.jgn.dynamic.*;
import com.captiveimagination.jgn.message.*;

/**
 * This is the base class for JavaGameNetworking.
 * Essentially, this is the static controller of
 * this API. Any overrides or message registration
 * is done here.
 * 
 * @author Matthew D. Hicks
 */
public class JGN {
	public static boolean ALWAYS_REBUILD = true;
	
    private static HashMap registered;
    private static HashMap getMethods;
    private static HashMap setMethods;
    
    private static File dynamicDirectory;
    private static HashMap handlers;
    private static DynamicClassLoader loader;
    
    /**
     * Messages must be registered with this method
     * before being used.
     * 
     * @param c
     *      This is a Class extending Message or CertifiedMessage
     */
    public static final synchronized void registerMessage(Class c, short type) {
        init();
        // Verify Class c extends Message
        if (!Message.class.isAssignableFrom(c)) {
            throw new ClassCastException(c.getCanonicalName() + " does not extend " + Message.class.getCanonicalName());
        }
        
        // Introspect class for getters and setters
        Method[] m = c.getMethods();
        ArrayList getList = new ArrayList();
        ArrayList setList = new ArrayList();
        for (int i = 0; i < m.length; i++) {
            if ((isValidMethod(m[i])) && (m[i].getName().startsWith("get"))) {
                try {
                    Method setter = c.getMethod("set" + m[i].getName().substring(3), new Class[] {m[i].getReturnType()});
                    if (setter != null) {
                        getList.add(m[i]);
                        setList.add(setter);
                    }
                } catch(NoSuchMethodException exc) {
                    System.err.println("Setter correspondent not found for " + m[i].getName() + ", ignoring.");
                } catch(Exception exc) {
                    exc.printStackTrace();
                }
            }
        }
        Comparator methodComparator = new Comparator() {
            public int compare(Object o1, Object o2) {
                Method m1 = (Method)o1;
                Method m2 = (Method)o2;
                return m1.getName().compareTo(m2.getName());
            }
        };
        Collections.sort(getList, methodComparator);
        Collections.sort(setList, methodComparator);
        Method[] getters = new Method[getList.size()];
        Method[] setters = new Method[setList.size()];
        for (int i = 0; i < getters.length; i++) {
            getters[i] = (Method)getList.get(i);
            setters[i] = (Method)setList.get(i);
        }
        getMethods.put(c, getters);
        setMethods.put(c, setters);

        // Register Message
        /*
        try {
            Message instance = (Message)c.newInstance();
            byte type = instance.getType();
            if ((registered.get(new Byte(id)) != null) && (registered.get(new Byte(id)) != c)) {
                throw new RuntimeException("Message Type " + id + " already registered to " + ((Class)registered.get(new Byte(id))).getCanonicalName() + " and trying to register same id to " + c.getCanonicalName());
            }
            registered.put(new Byte(id), c);
            
            if (handlers != null) {
                registerDynamic(c);
            }
        } catch(Exception exc) {
            throw new ClassCastException(exc.getMessage());
        }*/
        if ((registered.get(new Short(type)) != null) && (registered.get(new Short(type)) != c)) {
        	throw new RuntimeException("Message Type " + type + " already registered to " + ((Class)registered.get(new Short(type))).getCanonicalName() + " and trying to register same id to " + c.getCanonicalName());
        }
        registered.put(new Short(type), c);
        // If JDT is found then generate classes to handle message creation
        if (dynamicDirectory != null) {
        	registerDynamic(c);
        }
    }
    
    /**
     * This is used when a Message is instatiated to get the unique
     * <code>short</code> for this message.
     * 
     * @param m
     * @return
     * 		type as a short
     */
    public static short getType(Message m) {
    	init();
    	
    	Iterator i = registered.keySet().iterator();
    	Short s;
    	while (i.hasNext()) {
    		s = (Short)i.next();
    		if (registered.get(s) == m.getClass()) {
    			return s.shortValue();
    		}
    	}
    	throw new RuntimeException("The message " + m.getClass().getName() + " is not registered to a type. Use JGN.registerMessage(Class messageClass, short type).");
    }
    
    private static void registerDynamic(Class c) {
        try {
            String name = c.getName().substring(c.getName().lastIndexOf(".") + 1);
            
            if (!ALWAYS_REBUILD) {
                try {
                    Class handler = Class.forName(name + "_MessageHandler");
                    if (handler != null) {
                        System.out.println(name + "_MessageHandler found in classpath, using.");
                        handlers.put(c, handler.newInstance());
                        return;
                    }
                } catch(Throwable t) {
                    System.out.println(name + "_MessageHandler not found in the classpath.");
                }
            }
            
            File build = new File(dynamicDirectory, name + "_MessageHandler.class");
            if ((ALWAYS_REBUILD) || (!build.exists())) {
	            // Build MessageHandler implementation
	            File src = new File(dynamicDirectory, name + "_MessageHandler.java");
	            System.out.println("Generating: " + src.getName() + ", " + ALWAYS_REBUILD);
	            BufferedWriter writer = new BufferedWriter(new FileWriter(src));
	            if (c.getName().indexOf('.') > -1) {
	            	writer.write("import " + c.getName() + ";");
	            	writer.newLine();
	            	writer.newLine();
	            }
	            writer.write("import java.io.*;");;
	            writer.newLine();
	            writer.write("import com.captiveimagination.jgn.*;");
	            writer.newLine();
	            writer.write("import com.captiveimagination.jgn.message.*;");
	            writer.newLine();
	            writer.newLine();
	            writer.write("public class " + name + "_MessageHandler implements MessageHandler {");
	            writer.newLine();
	            writer.write("\tpublic Message receiveMessage(DataInputStream dis) throws IOException {");
	            writer.newLine();
	            
	            writer.write("\t\t" + name + " m = new " + name + "();");
	            writer.newLine();
	            Method[] setters = (Method[])setMethods.get(c);
	            Class setType;
	            // TODO provide functionality for null values in array
	            for (int i = 0; i < setters.length; i++) {
	                setType = setters[i].getParameterTypes()[0];
	                boolean array = true;
	                String arrayType = null;
	                String readType = null;
	                if (setType == Boolean[].class) {
	                    arrayType = "Boolean";
	                    readType = "new Boolean(dis.readBoolean())";
	                } else if (setType == boolean[].class) {
	                    arrayType = "boolean";
	                    readType = "dis.readBoolean()";
	                } else if (setType == Byte[].class) {
	                    arrayType = "Byte";
	                    readType = "new Byte(dis.readByte())";
	                } else if (setType == byte[].class) {
	                    arrayType = "byte";
	                    readType = "dis.readByte()";
	                } else if (setType == Short[].class) {
	                    arrayType = "Short";
	                    readType = "new Short(dis.readShort())";
	                } else if (setType == short[].class) {
	                    arrayType = "short";
	                    readType = "dis.readShort()";
	                } else if (setType == Integer[].class) {
	                    arrayType = "Integer";
	                    readType = "new Integer(dis.readInt())";
	                } else if (setType == int[].class) {
	                    arrayType = "int";
	                    readType = "dis.readInt()";
	                } else if (setType == Long[].class) {
	                    arrayType = "Long";
	                    readType = "new Long(dis.readLong())";
	                } else if (setType == long[].class) {
	                    arrayType = "long";
	                    readType = "dis.readLong()";
	                } else if (setType == Float[].class) {
	                    arrayType = "Float";
	                    readType = "new Float(dis.readFloat())";
	                } else if (setType == float[].class) {
	                    arrayType = "float";
	                    readType = "dis.readFloat()";
	                } else if (setType == Double[].class) {
	                    arrayType = "Double";
	                    readType = "new Double(dis.readDouble())";
	                } else if (setType == double[].class) {
	                    arrayType = "double";
	                    readType = "dis.readDouble()";
	                } else if (setType == Character[].class) {
	                    arrayType = "Character";
	                    readType = "new Character(dis.readChar())";
	                } else if (setType == char[].class) {
	                    arrayType = "char";
	                    readType = "dis.readChar()";
	                } else if (setType == String[].class) {
	                    arrayType = "String";
	                    readType = "JGN.readString(dis)";
	                } else {
	                    array = false;
	                }
	                if (array) {
	                    writer.write("\t\tint length" + i + " = dis.readInt();");
	                    writer.newLine();
	                    writer.write("\t\t" + arrayType + "[] array" + i + " = null;");
	                    writer.newLine();
	                    writer.write("\t\tif (length" + i + " > -1) {");
	                    writer.newLine();
	                    writer.write("\t\t\t array" + i + " = new " + arrayType + "[length" + i + "];");
	                    writer.newLine();
	                    writer.write("\t\t\tfor (int i = 0; i < array" + i + ".length; i++) {");
	                    writer.newLine();
	                    writer.write("\t\t\t\tarray" + i + "[i] = " + readType + ";");
	                    writer.newLine();
	                    writer.write("\t\t\t}");
	                    writer.newLine();
	                    writer.write("\t\t}");
	                    writer.newLine();
	                }
	                
	                writer.write("\t\tm." + setters[i].getName() + "(");
	                if (setType == Boolean.class) {
	                    writer.write("new Boolean(dis.readBoolean())");
	                } else if (setType == boolean.class) {
	                    writer.write("dis.readBoolean()");
	                } else if (setType == Byte.class) {
	                    writer.write("new Byte(dis.readByte())");
	                } else if (setType == byte.class) {
	                    writer.write("dis.readByte()");
	                } else if (setType == Short.class) {
	                    writer.write("new Short(dis.readShort())");
	                } else if (setType == short.class) {
	                    writer.write("dis.readShort()");
	                } else if (setType == Integer.class) {
	                    writer.write("new Integer(dis.readInt())");
	                } else if (setType == int.class) {
	                    writer.write("dis.readInt()");
	                } else if (setType == Long.class) {
	                    writer.write("new Long(dis.readLong())");
	                } else if (setType == long.class) {
	                    writer.write("dis.readLong()");
	                } else if (setType == Float.class) {
	                    writer.write("new Float(dis.readFloat())");
	                } else if (setType == float.class) {
	                    writer.write("dis.readFloat()");
	                } else if (setType == Double.class) {
	                    writer.write("new Double(dis.readDouble()");
	                } else if (setType == double.class) {
	                    writer.write("dis.readDouble()");
	                } else if (setType == Character.class) {
	                    writer.write("new Character(dis.readChar())");
	                } else if (setType == char.class) {
	                    writer.write("dis.readChar()");
	                } else if (setType == String.class) {
	                    writer.write("JGN.readString(dis)");
	                } else if (setType == Boolean[].class) {
	                    writer.write("array" + i);
	                } else if (setType == boolean[].class) {
	                    writer.write("array" + i);
	                } else if (setType == Byte[].class) {
	                    writer.write("array" + i);
	                } else if (setType == byte[].class) {
	                    writer.write("array" + i);
	                } else if (setType == Short[].class) {
	                    writer.write("array" + i);
	                } else if (setType == short[].class) {
	                    writer.write("array" + i);
	                } else if (setType == Integer[].class) {
	                    writer.write("array" + i);
	                } else if (setType == int[].class) {
	                    writer.write("array" + i);
	                } else if (setType == Long[].class) {
	                    writer.write("array" + i);
	                } else if (setType == long[].class) {
	                    writer.write("array" + i);
	                } else if (setType == Float[].class) {
	                    writer.write("array" + i);
	                } else if (setType == float[].class) {
	                    writer.write("array" + i);
	                } else if (setType == Double[].class) {
	                    writer.write("array" + i);
	                } else if (setType == double[].class) {
	                    writer.write("array" + i);
	                } else if (setType == Character[].class) {
	                    writer.write("array" + i);
	                } else if (setType == char[].class) {
	                    writer.write("array" + i);
	                } else if (setType == String[].class) {
	                    writer.write("array" + i);
	                } else {
	                    throw new RuntimeException("Unknown class type for setter (JGN.set): " + setType.getCanonicalName());
	                }
	                writer.write(");");
	                writer.newLine();
	            }
	            writer.write("\t\treturn m;");
	            writer.newLine();
	            
	            writer.write("\t}");
	            writer.newLine();
	            writer.newLine();
	            writer.write("\tpublic void sendMessage(Message message, DataOutputStream dos) throws IOException {");
	            writer.newLine();
	            writer.write("\t\t" + name + " m = (" + name + ")message;");
	            writer.newLine();
	            Method[] getters = (Method[])getMethods.get(c);
	            Class rt;
	            for (int i = 0; i < getters.length; i++) {
	                rt = getters[i].getReturnType();
	                boolean array = true;
	                String arrayType = null;
	                String writeType = null;
	                
	                if (rt == Boolean[].class) {
	                    arrayType = "Boolean";
	                    writeType = "dos.writeBoolean(array" + i + "[i].booleanValue())";
	                } else if (rt == boolean[].class) {
	                    arrayType = "boolean";
	                    writeType = "dos.writeBoolean(array" + i + "[i])";
	                } else if (rt == Byte[].class) {
	                    arrayType = "Byte";
	                    writeType = "dos.writeByte(array" + i + "[i].byteValue())";
	                } else if (rt == byte[].class) {
	                    arrayType = "byte";
	                    writeType = "dos.writeByte(array" + i + "[i])";
	                } else if (rt == Short[].class) {
	                    arrayType = "Short";
	                    writeType = "dos.writeShort(array" + i + "[i].shortValue())";
	                } else if (rt == short[].class) {
	                    arrayType = "short";
	                    writeType = "dos.writeShort(array" + i + "[i])";
	                } else if (rt == Integer[].class) {
	                    arrayType = "Integer";
	                    writeType = "dos.writeInt(array" + i + "[i].intValue())";
	                } else if (rt == int[].class) {
	                    arrayType = "int";
	                    writeType = "dos.writeInt(array" + i + "[i])";
	                } else if (rt == Long[].class) {
	                    arrayType = "Long";
	                    writeType = "dos.writeLong(array" + i + "[i].longValue())";
	                } else if (rt == long[].class) {
	                    arrayType = "long";
	                    writeType = "dos.writeLong(array" + i + "[i])";
	                } else if (rt == Float[].class) {
	                    arrayType = "Float";
	                    writeType = "dos.writeFloat(array" + i + "[i].floatValue())";
	                } else if (rt == float[].class) {
	                    arrayType = "float";
	                    writeType = "dos.writeFloat(array" + i + "[i])";
	                } else if (rt == Double[].class) {
	                    arrayType = "Double";
	                    writeType = "dos.writeDouble(array" + i + "[i].doubleValue)";
	                } else if (rt == double[].class) {
	                    arrayType = "double";
	                    writeType = "dos.writeDouble(array" + i + "[i])";
	                } else if (rt == Character[].class) {
	                    arrayType = "Character";
	                    writeType = "dos.writeChar(array" + i + "[i].charValue())";
	                } else if (rt == char[].class) {
	                    arrayType = "char";
	                    writeType = "dos.writeChar(array" + i + "[i])";
	                } else if (rt == String[].class) {
	                    arrayType = "String";
	                    writeType = "JGN.writeString(array" + i + "[i], dos)";
	                } else {
	                    array = false;
	                }
	                if (array) {
	                    writer.write("\t\t" + arrayType + "[] array" + i + " = m." + getters[i].getName() + "();");
	                    writer.newLine();
	                    writer.write("\t\tif (array" + i + " != null) {");
	                    writer.newLine();
	                    writer.write("\t\t\tdos.writeInt(array" + i + ".length);");
	                    writer.newLine();
	                    writer.write("\t\t\tfor (int i = 0; i < array" + i + ".length; i++) {");
	                    writer.newLine();
	                    writer.write("\t\t\t\t" + writeType + ";");
	                    writer.newLine();
	                    writer.write("\t\t\t}");
	                    writer.newLine();
	                    writer.write("\t\t} else {");
	                    writer.newLine();
	                    writer.write("\t\t\tdos.writeInt(-1);");
	                    writer.newLine();
	                    writer.write("\t\t}");
	                } else if (rt == Boolean.class) {
	                    writer.write("\t\tdos.writeBoolean(m." + getters[i].getName() + "().booleanValue());");
	                } else if (rt == boolean.class) {
	                    writer.write("\t\tdos.writeBoolean(m." + getters[i].getName() + "());");
	                } else if (rt == Byte.class) {
	                    writer.write("\t\tdos.writeByte(m." + getters[i].getName() + "().byteValue());");
	                } else if (rt == byte.class) {
	                    writer.write("\t\tdos.writeByte(m." + getters[i].getName() + "());");
	                } else if (rt == Short.class) {
	                    writer.write("\t\tdos.writeShort(m." + getters[i].getName() + "().shortValue());");
	                } else if (rt == short.class) {
	                    writer.write("\t\tdos.writeShort(m." + getters[i].getName() + "());");
	                } else if (rt == Integer.class) {
	                    writer.write("\t\tdos.writeInt(m." + getters[i].getName() + "().intValue());");
	                } else if (rt == int.class) {
	                    writer.write("\t\tdos.writeInt(m." + getters[i].getName() + "());");
	                } else if (rt == Long.class) {
	                    writer.write("\t\tdos.writeLong(m." + getters[i].getName() + "().longValue());");
	                } else if (rt == long.class) {
	                    writer.write("\t\tdos.writeLong(m." + getters[i].getName() + "());");
	                } else if (rt == Float.class) {
	                    writer.write("\t\tdos.writeFloat(m." + getters[i].getName() + "().floatValue());");
	                } else if (rt == float.class) {
	                    writer.write("\t\tdos.writeFloat(m." + getters[i].getName() + "());");
	                } else if (rt == Double.class) {
	                    writer.write("\t\tdos.writeDouble(m." + getters[i].getName() + "().doubleValue());");
	                } else if (rt == double.class) {
	                    writer.write("\t\tdos.writeDouble(m." + getters[i].getName() + "());");
	                } else if (rt == Character.class) {
	                    writer.write("\t\tdos.writeChar(m." + getters[i].getName() + "().charValue());");
	                } else if (rt == char.class) {
	                    writer.write("\t\tdos.writeChar(m." + getters[i].getName() + "());");
	                } else if (rt == String.class) {
	                    writer.write("\t\tJGN.writeString(m." + getters[i].getName() + "(), dos);");
	                }
	                writer.newLine();
	            }
	            
	            writer.write("\t}");
	            writer.newLine();
	            writer.write("}");
	            writer.flush();
	            writer.close();
	            
	            // Compile implementation
	            Class compiler = Class.forName("org.eclipse.jdt.internal.compiler.batch.Main");
	            Method m = compiler.getMethod("compile", new Class[] {String.class});
	            m.invoke(null, new Object[] {dynamicDirectory.getPath() + "/" + name + "_MessageHandler.java"});
        	}
            
            // Load implementation and set in handlers
            Class handler = loader.loadClass(name + "_MessageHandler");
            handlers.put(c, handler.newInstance());
        } catch(Throwable t) {
            System.err.println("Unable to generate dynamic handler for " + c.getName() + ": " + t.getMessage());
            t.printStackTrace();
        }
    }
    
    public static String readString(DataInputStream dis) throws IOException {
        int length = dis.readInt();
        if (length > -1) {
            byte[] b = new byte[length];
            dis.read(b);
            return new String(b);
        }
        return null;
    }
    
    public static void writeString(String s, DataOutputStream dos) throws IOException {
        if (s == null) {
            dos.writeInt(-1);
        } else {
            byte[] b = s.getBytes();
            dos.writeInt(b.length);
            dos.write(b);
        }
    }

    /**
     * This method is used internally for the MessageServer to dynamically
     * instantiate and populate Message objects.
     * 
     * @param buffer
     * @param length
     * @param address
     * @param port
     * @return
     * @throws IOException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    public static final Message receiveMessage(byte[] buffer, int start, int length, IP address, int port) throws IOException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    	init();
    	
    	CustomByteArrayInputStream bais = new CustomByteArrayInputStream(buffer, start, length);
        DataInputStream dis = new DataInputStream(bais);
        short type = dis.readShort();
        long id = dis.readLong();
        Class c = (Class)registered.get(new Short(type));
        
        Message m;
        if (handlers.get(c) != null) {
            MessageHandler handler = (MessageHandler)handlers.get(c);
            m = handler.receiveMessage(dis);
        } else {
            m = (Message)c.newInstance();

            // Call setter methods
            Method[] setters = (Method[])setMethods.get(c);
            for (int i = 0; i < setters.length; i++) {
                set(m, setters[i], dis);
            }
        }
        m.setId(id);
        m.setRemoteAddress(address);
        m.setRemotePort(port);
        m.setMessageLength(bais.getPosition() - start);
        
        return m;
    }

    public static final byte[] convertMessage(Message m) throws IOException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
    	init();
    	if (!registered.containsValue(m.getClass())) {
    		throw new RuntimeException(m.getClass().getCanonicalName() + " must be registered before use via JGN.registerMessage.");
    	}
    	
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeShort(m.getType());
        dos.writeLong(m.getId());
        
        if (handlers.get(m.getClass()) != null) {
        	// Use dynamically compiled classes
            MessageHandler handler = (MessageHandler)handlers.get(m.getClass());
            handler.sendMessage(m, dos);
        } else {
            // Call getter methods via reflection
            Method[] getters = (Method[])getMethods.get(m.getClass());
            for (int i = 0; i < getters.length; i++) {
                get(m, getters[i], dos);
            }
        }
        
        byte[] bytes = baos.toByteArray();
        baos.close();
        return bytes;
    }
    
    private static final void set(Message message, Method setter, DataInputStream dis) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, IOException {
        // TODO fix for null values being able to be passed
        Class setType = setter.getParameterTypes()[0];
        if ((setType == Boolean.class) || (setType == boolean.class)) {
            setter.invoke(message, new Object[] {new Boolean(dis.readBoolean())});
        } else if ((setType == Byte.class) || (setType == byte.class)) {
            setter.invoke(message, new Object[] {new Byte(dis.readByte())});
        } else if ((setType == Short.class) || (setType == short.class)) {
            setter.invoke(message, new Object[] {new Short(dis.readShort())});
        } else if ((setType == Integer.class) || (setType == int.class)) {
            setter.invoke(message, new Object[] {new Integer(dis.readInt())});
        } else if ((setType == Long.class) || (setType == long.class)) {
            setter.invoke(message, new Object[] {new Long(dis.readLong())});
        } else if ((setType == Float.class) || (setType == float.class)) {
            setter.invoke(message, new Object[] {new Float(dis.readFloat())});
        } else if ((setType == Double.class) || (setType == double.class)) {
            setter.invoke(message, new Object[] {new Short(dis.readShort())});
        } else if ((setType == Character.class) || (setType == char.class)) {
            setter.invoke(message, new Object[] {new Character(dis.readChar())});
        } else if (setType == String.class) {
            setter.invoke(message, new Object[] {dis.readUTF()});
        } else if (setType == Boolean[].class) {
            Boolean[] array = new Boolean[dis.readInt()];
            for (int i = 0; i < array.length; i++) {
                array[i] = new Boolean(dis.readBoolean());
            }
            setter.invoke(message, new Object[] {array});
        } else if (setType == boolean[].class) {
            boolean[] array = new boolean[dis.readInt()];
            for (int i = 0; i < array.length; i++) {
                array[i] = dis.readBoolean();
            }
            setter.invoke(message, new Object[] {array});
        } else if (setType == Byte[].class) {
            Byte[] array = new Byte[dis.readInt()];
            for (int i = 0; i < array.length; i++) {
                array[i] = new Byte(dis.readByte());
            }
            setter.invoke(message, new Object[] {array});
        } else if (setType == byte[].class) {
            byte[] array = new byte[dis.readInt()];
            for (int i = 0; i < array.length; i++) {
                array[i] = dis.readByte();
            }
            setter.invoke(message, new Object[] {array});
        } else if (setType == Short[].class) {
            Short[] array = new Short[dis.readInt()];
            for (int i = 0; i < array.length; i++) {
                array[i] = new Short(dis.readShort());
            }
            setter.invoke(message, new Object[] {array});
        } else if (setType == short[].class) {
            short[] array = new short[dis.readInt()];
            for (int i = 0; i < array.length; i++) {
                array[i] = dis.readShort();
            }
            setter.invoke(message, new Object[] {array});
        } else if (setType == Integer[].class) {
            Integer[] array = new Integer[dis.readInt()];
            for (int i = 0; i < array.length; i++) {
                array[i] = new Integer(dis.readInt());
            }
            setter.invoke(message, new Object[] {array});
        } else if (setType == int[].class) {
            int[] array = new int[dis.readInt()];
            for (int i = 0; i < array.length; i++) {
                array[i] = dis.readInt();
            }
            setter.invoke(message, new Object[] {array});
        } else if (setType == Long[].class) {
            Long[] array = new Long[dis.readInt()];
            for (int i = 0; i < array.length; i++) {
                array[i] = new Long(dis.readLong());
            }
            setter.invoke(message, new Object[] {array});
        } else if (setType == long[].class) {
            long[] array = new long[dis.readInt()];
            for (int i = 0; i < array.length; i++) {
                array[i] = dis.readLong();
            }
            setter.invoke(message, new Object[] {array});
        } else if (setType == Float[].class) {
            Float[] array = new Float[dis.readInt()];
            for (int i = 0; i < array.length; i++) {
                array[i] = new Float(dis.readFloat());
            }
            setter.invoke(message, new Object[] {array});
        } else if (setType == float[].class) {
            float[] array = new float[dis.readInt()];
            for (int i = 0; i < array.length; i++) {
                array[i] = dis.readFloat();
            }
            setter.invoke(message, new Object[] {array});
        } else if (setType == Double[].class) {
            Double[] array = new Double[dis.readInt()];
            for (int i = 0; i < array.length; i++) {
                array[i] = new Double(dis.readDouble());
            }
            setter.invoke(message, new Object[] {array});
        } else if (setType == double[].class) {
            double[] array = new double[dis.readInt()];
            for (int i = 0; i < array.length; i++) {
                array[i] = dis.readDouble();
            }
            setter.invoke(message, new Object[] {array});
        } else if (setType == Character[].class) {
            Character[] array = new Character[dis.readInt()];
            for (int i = 0; i < array.length; i++) {
                array[i] = new Character(dis.readChar());
            }
            setter.invoke(message, new Object[] {array});
        } else if (setType == char[].class) {
            char[] array = new char[dis.readInt()];
            for (int i = 0; i < array.length; i++) {
                array[i] = dis.readChar();
            }
            setter.invoke(message, new Object[] {array});
        } else if (setType == String[].class) {
            String[] array = new String[dis.readInt()];
            for (int i = 0; i < array.length; i++) {
                array[i] = new String(dis.readUTF());
            }
            setter.invoke(message, new Object[] {array});
        } else {
            throw new RuntimeException("Unknown class type for setter (JGN.set): " + setType.getCanonicalName());
        }
    }
    
    private static final void get(Message message, Method getter, DataOutputStream dos) throws IllegalArgumentException, IOException, IllegalAccessException, InvocationTargetException {
        Class returnType = getter.getReturnType();
        if ((returnType == Boolean.class) || (returnType == boolean.class)) {
            dos.writeBoolean(((Boolean)getter.invoke(message, new Object[0])).booleanValue());
        } else if ((returnType == Byte.class) || (returnType == byte.class)) {
            dos.writeByte(((Byte)getter.invoke(message, new Object[0])).byteValue());
        } else if ((returnType == Short.class) || (returnType == short.class)) {
            dos.writeShort(((Short)getter.invoke(message, new Object[0])).shortValue());
        } else if ((returnType == Integer.class) || (returnType == int.class)) {
            dos.writeInt(((Integer)getter.invoke(message, new Object[0])).intValue());
        } else if ((returnType == Long.class) || (returnType == long.class)) {
            dos.writeLong(((Long)getter.invoke(message, new Object[0])).longValue());
        } else if ((returnType == Float.class) || (returnType == float.class)) {
            dos.writeFloat(((Float)getter.invoke(message, new Object[0])).floatValue());
        } else if ((returnType == Double.class) || (returnType == double.class)) {
            dos.writeDouble(((Double)getter.invoke(message, new Object[0])).doubleValue());
        } else if ((returnType == Character.class) || (returnType == char.class)) {
            dos.writeChar(((Character)getter.invoke(message, new Object[0])).charValue());
        } else if (returnType == String.class) {
            String s = (String)getter.invoke(message, new Object[0]);
            if (s == null) {
                System.out.println("Null value: " + message.getClass().getCanonicalName() + "." + getter.getName());
            }
            dos.writeUTF(s);
        } else if (returnType == Boolean[].class) {
            Boolean[] array = (Boolean[])getter.invoke(message, new Object[0]);
            dos.writeInt(array.length);
            for (int i = 0; i < array.length; i++) {
                dos.writeBoolean(array[i].booleanValue());
            }
        } else if (returnType == boolean[].class) {
            boolean[] array = (boolean[])getter.invoke(message, new Object[0]);
            dos.writeInt(array.length);
            for (int i = 0; i < array.length; i++) {
                dos.writeBoolean(array[i]);
            }
        } else if (returnType == Byte[].class) {
            Byte[] array = (Byte[])getter.invoke(message, new Object[0]);
            dos.writeInt(array.length);
            for (int i = 0; i < array.length; i++) {
                dos.writeByte(array[i].byteValue());
            }
        } else if (returnType == byte[].class) {
            byte[] array = (byte[])getter.invoke(message, new Object[0]);
            dos.writeInt(array.length);
            for (int i = 0; i < array.length; i++) {
                dos.writeByte(array[i]);
            }
        } else if (returnType == Short[].class) {
            Short[] array = (Short[])getter.invoke(message, new Object[0]);
            dos.writeInt(array.length);
            for (int i = 0; i < array.length; i++) {
                dos.writeShort(array[i].shortValue());
            }
        } else if (returnType == short[].class) {
            short[] array = (short[])getter.invoke(message, new Object[0]);
            dos.writeInt(array.length);
            for (int i = 0; i < array.length; i++) {
                dos.writeShort(array[i]);
            }
        } else if (returnType == Integer[].class) {
            Integer[] array = (Integer[])getter.invoke(message, new Object[0]);
            dos.writeInt(array.length);
            for (int i = 0; i < array.length; i++) {
                dos.writeInt(array[i].intValue());
            }
        } else if (returnType == int[].class) {
            int[] array = (int[])getter.invoke(message, new Object[0]);
            dos.writeInt(array.length);
            for (int i = 0; i < array.length; i++) {
                dos.writeInt(array[i]);
            }
        } else if (returnType == Long[].class) {
            Long[] array = (Long[])getter.invoke(message, new Object[0]);
            dos.writeInt(array.length);
            for (int i = 0; i < array.length; i++) {
                dos.writeLong(array[i].longValue());
            }
        } else if (returnType == long[].class) {
            long[] array = (long[])getter.invoke(message, new Object[0]);
            dos.writeInt(array.length);
            for (int i = 0; i < array.length; i++) {
                dos.writeLong(array[i]);
            }
        } else if (returnType == Float[].class) {
            Float[] array = (Float[])getter.invoke(message, new Object[0]);
            dos.writeInt(array.length);
            for (int i = 0; i < array.length; i++) {
                dos.writeFloat(array[i].floatValue());
            }
        } else if (returnType == float[].class) {
            float[] array = (float[])getter.invoke(message, new Object[0]);
            dos.writeInt(array.length);
            for (int i = 0; i < array.length; i++) {
                dos.writeFloat(array[i]);
            }
        } else if (returnType == Double[].class) {
            Double[] array = (Double[])getter.invoke(message, new Object[0]);
            dos.writeInt(array.length);
            for (int i = 0; i < array.length; i++) {
                dos.writeDouble(array[i].doubleValue());
            }
        } else if (returnType == double[].class) {
            double[] array = (double[])getter.invoke(message, new Object[0]);
            dos.writeInt(array.length);
            for (int i = 0; i < array.length; i++) {
                dos.writeDouble(array[i]);
            }
        } else if (returnType == Character[].class) {
            Character[] array = (Character[])getter.invoke(message, new Object[0]);
            dos.writeInt(array.length);
            for (int i = 0; i < array.length; i++) {
                dos.writeChar(array[i].charValue());
            }
        } else if (returnType == char[].class) {
            char[] array = (char[])getter.invoke(message, new Object[0]);
            dos.writeInt(array.length);
            for (int i = 0; i < array.length; i++) {
                dos.writeChar(array[i]);
            }
        } else if (returnType == String[].class) {
            String[] array = (String[])getter.invoke(message, new Object[0]);
            dos.writeInt(array.length);
            for (int i = 0; i < array.length; i++) {
                dos.writeUTF(array[i]);
            }
        } else {
            throw new RuntimeException("Unknown class type for getter (JGN.get): " + returnType.getCanonicalName());
        }
    }
    
    /**
     * @return
     * 		A unique long randomly generated.
     */
    public static final long getUniqueLong() {
        long l = Math.round(Math.random() * Long.MAX_VALUE);
        l += Math.round(Math.random() * Long.MIN_VALUE);
        return l;
    }
    
    private static final void init() {
        if (registered == null) {
            registered = new HashMap();
            getMethods = new HashMap();
            setMethods = new HashMap();
            handlers = new HashMap();
            
            try {
                if (Class.forName("org.eclipse.jdt.internal.compiler.batch.Main") != null) {
                    dynamicDirectory = new File("generated");
                    loader = new DynamicClassLoader(dynamicDirectory);
                    if (dynamicDirectory.exists()) {
                        File[] files = dynamicDirectory.listFiles();
                        for (int i = 0; i < files.length; i++) {
                            files[i].delete();
                        }
                    } else {
                        dynamicDirectory.mkdirs();
                    }
                }
            } catch(ClassNotFoundException exc) {
                System.err.println("WARNING: JDT Compiler not found, reverting to reflection.");
            }
            
            registerMessage(Receipt.class, Short.MIN_VALUE);
            registerMessage(FileTransferMessage.class, (short)(Short.MIN_VALUE / 2));
            registerMessage(PingMessage.class, (short)((Short.MIN_VALUE / 4) + 1));
            registerMessage(PongMessage.class, (short)((Short.MIN_VALUE / 4) + 2));
            registerMessage(TimeSyncMessage.class, (short)((Short.MIN_VALUE / 4) + 3));
        }
    }
    
    private static final boolean isValidMethod(Method m) {
        Class returnType = m.getReturnType();
        if ((returnType == Boolean.class) ||
            (returnType == Boolean[].class) ||
            (returnType == boolean.class) ||
            (returnType == boolean[].class) ||
            (returnType == Byte.class) ||
            (returnType == Byte[].class) ||
            (returnType == byte.class) ||
            (returnType == byte[].class) ||
            (returnType == Short.class) ||
            (returnType == Short[].class) ||
            (returnType == short.class) ||
            (returnType == short[].class) ||
            (returnType == Integer.class) ||
            (returnType == Integer[].class) ||
            (returnType == int.class) ||
            (returnType == int[].class) ||
            (returnType == Long.class) ||
            (returnType == Long[].class) ||
            (returnType == long.class) ||
            (returnType == long[].class) ||
            (returnType == Float.class) ||
            (returnType == Float[].class) ||
            (returnType == float.class) ||
            (returnType == float[].class) ||
            (returnType == Double.class) ||
            (returnType == Double[].class) ||
            (returnType == double.class) ||
            (returnType == double[].class) ||
            (returnType == Character.class) ||
            (returnType == Character[].class) ||
            (returnType == char.class) ||
            (returnType == char[].class) ||
            (returnType == String.class) ||
            (returnType == String[].class) ||
            (returnType == void.class)) {
            if ((!m.getName().equals("getResendTimeout")) &&
                (!m.getName().equals("setResendTimeout")) &&
                (!m.getName().equals("getMaxTries")) &&
                (!m.getName().equals("getTried")) &&
                (!m.getName().equals("getId")) &&
                (!m.getName().equals("setId")) &&
                (!m.getName().equals("getType")) &&
                (!m.getName().equals("setType")) &&
                (!m.getName().equals("getRemoteAddress")) &&
                (!m.getName().equals("setRemoteAddress")) &&
                (!m.getName().equals("getRemotePort")) &&
                (!m.getName().equals("setRemotePort"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * This allows a MessageHandler to be explicitly set on a Message class. This should
     * be called after JGN.registerMessage().
     * 
     * @param messageClass
     * @param handler
     */
    public static final void setHandler(Class messageClass, MessageHandler handler) {
        init();
        handlers.put(messageClass, handler);
    }

    private static Deflater d = new Deflater(Deflater.HUFFMAN_ONLY);
    private static byte[] compressionBuffer = new byte[512 * 2000];
    public static synchronized final byte[] compress(byte[] b) {
    	// TODO Default to HUFFMAN, but allow for others in the future
    	d.setInput(b);
    	int length = d.deflate(compressionBuffer);
    	byte[] temp = new byte[length];
    	System.arraycopy(compressionBuffer, 0, temp, 0, length);
    	return temp;
    }
    
    private static Inflater i = new Inflater();
    private static byte[] uncompressionBuffer = new byte[512 * 2000];
    public static synchronized final byte[] uncompress(byte[] b) throws DataFormatException {
    	// TODO Default to HUFFMAN, but allow for others in the future
    	i.setInput(b);
    	int length = i.inflate(uncompressionBuffer);
    	byte[] temp = new byte[length];
    	System.arraycopy(uncompressionBuffer, 0, temp, 0, length);
    	return temp;
    }
}
