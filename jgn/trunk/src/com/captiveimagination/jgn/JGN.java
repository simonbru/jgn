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
 * Created: Jun 10, 2006
 */
package com.captiveimagination.jgn;

import java.io.*;
import java.util.*;

import com.captiveimagination.jgn.convert.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.ro.*;

/**
 * Foundational static class for various functionality that is abstract from any other
 * specific class but is necessary for the JGN project.
 * 
 * @author Matthew D. Hicks
 */
public class JGN {
	private static final HashMap<Short,Class<? extends Message>> registry = new HashMap<Short,Class<? extends Message>>();
	private static final HashMap<Class<? extends Message>,Short> registryReverse = new HashMap<Class<? extends Message>,Short>();
	private static final HashMap<Class<? extends Message>,ConversionHandler> converters = new HashMap<Class<? extends Message>,ConversionHandler>();
	static {
		// Certain messages must be known before negotiation so this is explicitly done here
		register(LocalRegistrationMessage.class, (short)-1);
		register(StreamMessage.class, (short)-2);
		register(NoopMessage.class, (short)-3);
		register(Receipt.class, (short)-4);
		register(DisconnectMessage.class, (short)-5);
		register(RemoteObjectRequest.class, (short)-6);
		register(RemoteObjectResponse.class, (short)-7);
	}
	
	/**
	 * Messages must be registered via this method preferrably before any communication
	 * occurs for efficiency in the initial connectivity negotiation process.
	 * 
	 * @param c
	 */
	public static final synchronized void register(Class<? extends Message> c) {
		if (registry.containsValue(c)) {
			return;
		}
		
		short id = generateId();
        while (registry.containsKey(id)) {
        	id = generateId();
        }
        register(c, id);
	}
	
	private static final void register(Class<? extends Message> c, short id) {
		converters.put(c, ConversionHandler.getConversionHandler(c));
        registry.put(id, c);
        registryReverse.put(c, id);
	}
	
	private static final short generateId() {
		short id = (short)Math.round(Math.random() * Short.MAX_VALUE);
        return id;
	}
		
	/**
	 * Request the ConversionHandler associated with this Message class.
	 * 
	 * @param c
	 * @return
	 * 		ConversionHandler associated with the Class <code>c</code>
	 * 		if the Message class referenced has not be registered yet
	 * 		<code>null</code> will be returned.
	 */
	public static final ConversionHandler getConverter(Class<? extends Message> c) {
		return converters.get(c);
	}
	
	public static final short getMessageTypeId(Class<? extends Message> c) {
		return registryReverse.get(c);
	}
	
	public static final Class<? extends Message> getMessageTypeClass(short typeId) {
		return registry.get(typeId);
	}

	public static final void populateRegistrationMessage(LocalRegistrationMessage message) {
		message.setPriority(PriorityMessage.PRIORITY_HIGH);
		Short[] shorts = (Short[])registry.keySet().toArray(new Short[registry.keySet().size()]);
		
		// Determine non-system message count
		int nonSystem = 0;
		for (short s : shorts) {
			if (s >= 0) nonSystem++;
		}
		
		short[] ids = new short[nonSystem];
		String[] names = new String[nonSystem];
		int count = 0;
		for (int i = 0; i < shorts.length; i++) {
			if (shorts[i] < 0) continue;
			ids[count] = shorts[i];
			names[count] = registry.get(shorts[i]).getName();
			count++;
		}
		message.setIds(ids);
		message.setMessageClasses(names);
	}

	public static final Runnable createMessageServerRunnable(final MessageServer server) {
		Runnable r = new Runnable() {
			public void run() {
				while (server.isAlive()) {
					try {
						server.update();
					} catch(IOException exc) {
						throw new RuntimeException(exc);
					}
					try {
						Thread.sleep(1);
					} catch(InterruptedException exc) {
						exc.printStackTrace();
					}
				}
			}
		};
		return r;
	}
	
	public static final Thread createMessageServerThread(MessageServer server) {
		return new Thread(createMessageServerRunnable(server));
	}
}
