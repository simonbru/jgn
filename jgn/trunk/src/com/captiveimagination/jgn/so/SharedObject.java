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
 * Created: Oct 1, 2006
 */
package com.captiveimagination.jgn.so;

import java.lang.reflect.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.convert.*;
import com.captiveimagination.magicbeans.*;

/**
 * @author Matthew D. Hicks
 */
public class SharedObject {
	private String name;
	private Object object;
	private Class interfaceClass;
	private HashMap<String, Converter> converters;
	private ConcurrentLinkedQueue<String> updates;
	private ConcurrentLinkedQueue<MessageServer> servers;
	private ConcurrentLinkedQueue<MessageClient> clients;

	protected SharedObject(String name, Object object, Class interfaceClass, HashMap<String, Converter> converters) {
		this.name = name;
		this.object = object;
		this.interfaceClass = interfaceClass;
		this.converters = converters;
		updates = new ConcurrentLinkedQueue<String>();
		servers = new ConcurrentLinkedQueue<MessageServer>();
		clients = new ConcurrentLinkedQueue<MessageClient>();
	}

	protected String getName() {
		return name;
	}

	protected Object getObject() {
		return object;
	}

	protected void add(MessageServer server, ObjectCreateMessage message) {
		if (!servers.contains(server)) {
			servers.add(server);
			message.setName(name);
			message.setInterfaceClass(interfaceClass.getName());
			server.broadcast(message);
		}
	}

	protected boolean remove(MessageServer server, ObjectDeleteMessage message) {
		message.setName(name);
		server.broadcast(message);
		return servers.remove(server);
	}

	protected void add(MessageClient client, ObjectCreateMessage message) {
		addInternal(client);
		message.setName(name);
		message.setInterfaceClass(interfaceClass.getName());
		client.sendMessage(message);
	}
	
	protected void addInternal(MessageClient client) {
		if (!clients.contains(client)) {
			clients.add(client);
		}
	}
	
	protected void broadcast(MessageClient client, ByteBuffer buffer) {
		// Create the object
		ObjectCreateMessage message = new ObjectCreateMessage();
		message.setName(name);
		message.setInterfaceClass(interfaceClass.getName());
		client.sendMessage(message);
		// Send all values for object
		updateClient(client, buffer);
	}
	
	protected void updateClient(MessageClient client, ByteBuffer buffer) {
		ObjectUpdateMessage update = new ObjectUpdateMessage();
		String[] fields = converters.keySet().toArray(new String[converters.size()]);
		update(buffer, update, fields, client);
	}

	protected boolean remove(MessageClient client, ObjectDeleteMessage message) {
		message.setName(name);
		client.sendMessage(message);
		return clients.remove(client);
	}

	protected boolean contains(MessageServer server) {
		return servers.contains(server);
	}

	protected boolean contains(MessageClient client) {
		return clients.contains(client);
	}

	protected void updated(String field) {
		if (!updates.contains(field)) updates.add(field);
	}

	protected void update(ByteBuffer buffer, ObjectUpdateMessage message) {
		if (updates.size() == 0) return;
		String[] fields = updates.toArray(new String[updates.size()]);
		update(buffer, message, fields, null);
	}
	
	protected void update(ByteBuffer buffer, ObjectUpdateMessage message, String[] fields, MessageClient client) {
		message.setName(name);
		Converter converter;
		Method getter;
		for (int i = 0; i < fields.length; i++) {
			updates.remove(fields[i]);
			converter = converters.get(fields[i]);
			getter = SharedObjectManager.getInstance().getMethod(interfaceClass.getName() + ".get." + fields[i]);
			try {
				converter.get(object, getter, buffer);
			} catch(Exception exc) {
				exc.printStackTrace();	// TODO remove this
			}
		}
		message.setFields(fields);
		byte[] buf = new byte[buffer.position()];
		buffer.rewind();
		buffer.get(buf);
		message.setData(buf);

		if (client != null) {
			client.sendMessage(message);
		} else {
			// Send to MessageServer clients
			Iterator<MessageServer> serverIterator = servers.iterator();
			while (serverIterator.hasNext()) {
				serverIterator.next().broadcast(message);
			}
	
			// Send to MessageClients
			Iterator<MessageClient> clientIterator = clients.iterator();
			while (clientIterator.hasNext()) {
				clientIterator.next().sendMessage(message);
			}
		}
	}

	protected void apply(ObjectUpdateMessage message, ByteBuffer buffer) {
		buffer.put(message.getData());
		buffer.rewind();
		try {
			for (String field : message.getFields()) {
				Converter converter = converters.get(field);
				MagicBeanHandler handler = MagicBeanManager.getInstance().getMagicBeanHandler(object);
				Method setter = handler.getClass().getMethod("setValue", new Class[] {String.class, Object.class});
				Object value = converter.set(object, null, buffer);
				setter.invoke(handler, new Object[] {field, value});
			}
		} catch (IllegalAccessException exc) {
			exc.printStackTrace();
		} catch (NoSuchMethodException exc) {
			exc.printStackTrace();
		} catch (InvocationTargetException exc) {
			exc.printStackTrace();
		}
	}
}
