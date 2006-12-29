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

import java.util.*;

import com.captiveimagination.jgn.clientserver.message.*;
import com.captiveimagination.jgn.convert.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.ro.*;
import com.captiveimagination.jgn.so.*;
import com.captiveimagination.jgn.sync.message.*;
import com.captiveimagination.jgn.translation.*;

/**
 * Foundational static class for various functionality that is abstract from any other
 * specific class but is necessary for the JGN project.
 * 
 * @author Matthew D. Hicks
 */
public class JGN {
	// registry maps (short) id --> messageClass
	private static final HashMap<Short,Class<? extends Message>> registry = new HashMap<Short,Class<? extends Message>>();
	// registryReverse maps messageClass --> (short) id
	private static final HashMap<Class<? extends Message>,Short> registryReverse = new HashMap<Class<? extends Message>,Short>();
	// converters maps messageClass --> ConversionHandler
	private static final HashMap<Class<? extends Message>,ConversionHandler> converters = new HashMap<Class<? extends Message>,ConversionHandler>();

	static {
		// Certain messages must be known before negotiation so this is explicitly done here
		short n = -1;
		// foundation
		register(LocalRegistrationMessage.class, --n);
		register(StreamMessage.class, --n);
		register(NoopMessage.class, --n);
		register(Receipt.class, --n);
		register(DisconnectMessage.class, --n);
		// remote objects
		register(RemoteObjectRequestMessage.class, --n);
		register(RemoteObjectResponseMessage.class, --n);
		// JGN base
		register(PlayerStatusMessage.class, --n);
		register(ChatMessage.class, --n);
		// sync
		register(Synchronize2DMessage.class, --n);
		register(Synchronize3DMessage.class, --n);
		register(SynchronizePhysicsMessage.class, --n);
		// SharedObject Messages
		register(ObjectCreateMessage.class, --n);
		register(ObjectUpdateMessage.class, --n);
		register(ObjectDeleteMessage.class, --n);
		// Translation
		register(TranslatedMessage.class, --n);
	}
	
	/**
	 * Messages must be registered via this method preferrably before any communication
	 * occurs for efficiency in the initial connectivity negotiation process.
	 *
	 * [ase] note, this isn't an option, it is a must.
	 * the announced class will be registered with registry and reversRegistry, also
	 * a fitting Conversionhandler will be established
	 *
	 * as an implementation detail, note, that all messageclasses registered by this method
	 * receive a positive id, while system message will have negativ ids.
	 * [/ase]
	 * 
	 * @param c the message class to be registered
	 */
	public static final synchronized void register(Class<? extends Message> c) {
		if (registry.containsValue(c))
			return;
		
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
		return (short)Math.round(Math.random() * Short.MAX_VALUE);
	}
		
	/**
	 * Request the ConversionHandler associated with this Message class.
	 * 
	 * @param c the messageclass involved
	 * @return
	 * 		ConversionHandler associated with the Class <code>c</code>
	 * 		if the Message class referenced has not be registered yet
	 * 		<code>null</code> will be returned.
	 */
	public static final ConversionHandler getConverter(Class<? extends Message> c) {
		return converters.get(c);
	}

	/**
	 * request the id associated locally with the messageclass given
	 * uses the reverseRegistry
	 *
	 * @param c - the message type, asked for
	 * @return short - the id
	 * @throws MessageHandlingException - if class not registered
	 */
	public static final short getMessageTypeId(Class<? extends Message> c) throws MessageHandlingException {
		try {
			return registryReverse.get(c);
		} catch (NullPointerException e) {
			throw new MessageHandlingException("Messageclass unknown; may be not registered", null, e);
		}
	}

	/**
	 * requests a class for given id
	 * @param typeId
	 * @return  the message class or <code>null</code> if no class was registered with that id
	 */
	public static final Class<? extends Message> getMessageTypeClass(short typeId) {
		return registry.get(typeId);
	}

	/**
	 * fills the currently registered non-system messages (that is their names) into the LRM,
	 * together with their local ID's
	 * will not contain sysmtem level messages (with ids < 0)
	 *
	 * @param message the LRM to be filled in
	 */
	public static final void populateRegistrationMessage(LocalRegistrationMessage message) {
		message.setPriority(PriorityMessage.PRIORITY_HIGH);

		// all registered keys
		Short[] registeredIds = registry.keySet().toArray(new Short[registry.keySet().size()]);
		
		// Determine non-system message count
		int nonSystem = 0;
		for (Short s : registeredIds) {
			if (s >= 0) nonSystem++;
		}
		
		short[] ids = new short[nonSystem];
		String[] names = new String[nonSystem];

		// extract id + name
		int count = 0;
		for (Short id : registeredIds) {
			if (id < 0) continue;
			ids[count] = id;
			names[count] = registry.get(id).getName();
			count++;
		}
		message.setIds(ids);
		message.setMessageClasses(names);
	}

	/**
	 * tries to generate a pretty random long
	 * [ase]: random will return the same value
	 *        each time the applikation (eg. VM) will start anew.
	 *        This may be good for developers. Might consider using a private
	 *        java.util.Random for production.
	 * [/ase]
	 *
	 * @return long
	 */
	public static final long generateUniqueId() {
		long id = Math.round(Math.random() * Long.MAX_VALUE);
		id += Math.round(Math.random() * Long.MIN_VALUE);
		return id;
	}


	/**
	 * convenience routine for creating a Runnable that wraps a list of Updatables
	 * each pass through the list will be sperated by 1 ms pause.
	 *
	 * @see Updatable
	 * @see UpdatableRunnable
	 * @param updatables - one or more "tasks" to be done until they are not any more alive.
	 * @return Runnable - ready for wrapping into a thread and get started.
	 */
	public static final Runnable createRunnable(Updatable... updatables) {
		return createRunnable(1, updatables);
	}

	/**
	 * same as createRunnable(Updatable... updatables) but lets you specify the sleep time
	 * between passes.
	 *
	 * @param sleep - time to snare in ms
	 * @param updatables -one or more "tasks" to be done until they are not any more alive.
	 * @return Runnable - ready for wrapping into a thread and get started.
	 */
	public static final Runnable createRunnable(long sleep, Updatable... updatables) {
		return new UpdatableRunnable(sleep, updatables);
	}

	/**
	 * convenience routine for creating a ready-to-run thread around a Runnable that wraps a
	 * list of Updatables. Each pass through the list will be separated by 1 ms pause.
	 *
	 * @param updatables - one or more "tasks" to be done until they are not any more alive.
	 * @return Thread
	 */
	public static final Thread createThread(Updatable... updatables) {
		return new Thread(createRunnable(updatables));
	}
}

/**
 * A Runnable that within it's run method cycle a list of updatables as long as they
 * stay alive. Additionally a sleep time between cycles can be specified
 */
class UpdatableRunnable implements Runnable {
	private long sleep;
	private Updatable[] updatables;

	/**
	 * Creates the Runnable
	 * @param sleep      if > 0, will sleep this amount in ms between cycles;
	 *                   else no delay (but a Thread.yield instead)
	 *                   NOTE, that bigger values may reduce the response time of the system
	 *                   but too small a value will possibly eat up most CPU time, and make other
	 *                   threads unresponsive.
	 * @param updatables the tasks, to be run by executing their update() method.
	 */
	public UpdatableRunnable(long sleep, Updatable... updatables) {
		this.sleep = sleep;
		this.updatables = updatables;
	}

	/**
	 * periodically calls the update() method of all Updatables
	 * The owning thread will terminate, when all Updatables deliver isAlive() == false.
	 *
	 * Each Throwable during an update() will be wrapped into a RunTimeException and
	 * currently terminate ALL tasks and the owning thread.
	 */
	public void run() {
 		boolean alive;
		do {
      alive = false;
  		for (Updatable u : updatables) {
	    	if (u.isAlive()) {
					alive = true;     // ase: if at least one u is alive(), keep running
          try {
						u.update();
          } catch(Throwable t) {
						// TODO ase: think again about termination
						throw new RuntimeException(t);
			    }
        }
      }
      if (sleep > 0) {
				try {
					Thread.sleep(sleep);  // ase: changed this from "1" to sleep
				} catch(InterruptedException exc) {
					// ase: no real need for --> exc.printStackTrace();
				}
			} else {
				Thread.yield();
			}
		} while (alive);
	}
}
