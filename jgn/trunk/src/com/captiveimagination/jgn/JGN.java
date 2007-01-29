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

import com.captiveimagination.jgn.clientserver.message.PlayerStatusMessage;
import com.captiveimagination.jgn.convert.ConversionHandler;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.ro.RemoteObjectRequestMessage;
import com.captiveimagination.jgn.ro.RemoteObjectResponseMessage;
import com.captiveimagination.jgn.so.ObjectCreateMessage;
import com.captiveimagination.jgn.so.ObjectDeleteMessage;
import com.captiveimagination.jgn.so.ObjectUpdateMessage;
import com.captiveimagination.jgn.sync.message.Synchronize2DMessage;
import com.captiveimagination.jgn.sync.message.Synchronize3DMessage;
import com.captiveimagination.jgn.sync.message.SynchronizePhysicsMessage;
import com.captiveimagination.jgn.translation.TranslatedMessage;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Foundational static class for various functionality that is abstract from any other
 * specific class but is necessary for the JGN project.
 *
 * @author Matthew D. Hicks
 * @author Alfons Seul
 */
public class JGN {
	// registry maps (short) id --> messageClass
	private static final HashMap<Short, Class<? extends Message>> registry =
			new HashMap<Short, Class<? extends Message>>();
	// registryReverse maps messageClass --> (short) id
	private static final HashMap<Class<? extends Message>, Short> registryReverse =
			new HashMap<Class<? extends Message>, Short>();
	// hierarchy maps a messageclass --> List of superclasses, interfaces upto Message.class
	private static final HashMap<Class<? extends Message>, ArrayList<Class<?>>> hierarchy =
		  new HashMap<Class<? extends Message>, ArrayList<Class<?>>>();
	private static final int systemIdCnt; // used in populateLocalRegistryMessage()

	private static final Logger LOG = Logger.getLogger("com.captiveimagination.jgn.JGN");

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

		systemIdCnt = registry.size();

		// make sure, configuration+logging system is setup
		JGNConfig.ensureJGNConfigured();
	}

	/**
	 * Messages must be registered via this method preferrably before any communication
	 * occurs for efficiency in the initial connectivity negotiation process.
	 * <p/>
	 * note, this isn't an option, it is a must.
	 * the announced class will be registered with registry and reversRegistry, also
	 * a fitting Conversionhandler will be established
	 * <p/>
	 * as an implementation detail, note, that all messageclasses registered by this method
	 * receive a positive id, while system message will have negativ ids.
	 *
	 * @param c the message class to be registered
	 * @throws RuntimeException if class has no parameterless Ctor,
	 *                          or a single, non-static, non-final, non-transient field isn't
	 *                          serializable.
	 */
	public static final synchronized void register(Class<? extends Message> c) {
		if (registry.containsValue(c))
			return;

		// check for existence of a parameterless constructor
		// since system message don't need this check, it's put into the public access point:
		Constructor[] ctors = c.getConstructors();
		boolean hasIt = false;
		for (Constructor ctor : ctors) {
			if (ctor.getParameterTypes().length == 0) {
				hasIt = true;
				break;
			}
		}
		if (! hasIt) {
      // this message can't be serialized, this is an application error.
      // Stop further processing
			LOG.severe("Message " + c.getName() + " can't be serialized; no paramterless constructor.");
			LOG.severe("Application will terminate");
			throw new RuntimeException("Fatal: Message " + c.getName() + " can't be serialized. Check Constructors.");
		}
		short id = generateId();
		while (registry.containsKey(id)) {
			id = generateId();
		}
		register(c, id);
	}

	private static final void register(Class<? extends Message> c, short id) {
		// check if the message follows rules and register conversionHandler for it
		if (ConversionHandler.getConversionHandler(c) == null) {
			// this message can't be serialized, this is an application error.
			// Stop further processing
			LOG.severe("Message " + c.getName() + " can't be serialized. check fields");
			LOG.severe("Application will terminate");
			throw new RuntimeException("Fatal: Message " + c.getName() + " can't be serialized. Check Fields.");
		}
		registry.put(id, c);
		registryReverse.put(c, id);
		hierarchy.put(c, scanMessClassHierarchy(c)); // build up the hierarchy
	}

	private static final short generateId() {
		return (short) Math.round(Math.random() * Short.MAX_VALUE);
	}

//	------- not used anymore since Jan 16, 2007
//	/**
//	 * Request the ConversionHandler associated with this Message class.
//	 *
//	 * note, this is a convenience for calling ConversionHandler.getConversionHandler(c)
//	 * @param c the messageclass involved
//	 * @return
//	 * 		ConversionHandler associated with the MessageClass <code>c</code>
//	 * 		if the Message class referenced has not be registered yet
//	 * 		<code>null</code> will be returned.
//	 * @deprecated use ConversionHandler.getConversionHandler(c) instead
//	 */
//	public static final ConversionHandler getConverter(Class<? extends Message> c) {
//		ConversionHandler res = ConversionHandler.getConversionHandler(c);
//		if (res == null) { // couldn't find nor create a handler:
//			// although this might have been known earlier, (when registering)
//			// Stop further processing, this is an application error.
//			throw new RuntimeException("Fatal: Message "+c.getName()+" can't be serialized. Check Fields.");
//		}
//		return res;
//	}


	/**
	 * request the id associated locally with the messageclass given
	 * uses the reverseRegistry
	 *
	 * @param c - the message type, asked for
	 * @return short - the id
	 * @throws MessageHandlingException - if class not registered
	 */
	public static final short getMessageTypeId(Class<? extends Message> c) throws MessageHandlingException {
// not, what is intended ... (NPE would never occur...)
//		try {
//			return registryReverse.get(c);
//		} catch (NullPointerException e) {
//			throw new MessageHandlingException("Messageclass unknown; may be not registered", null, e);
//		}
// instead, do this:
		Short result = registryReverse.get(c);
		if (result == null) {
			LOG.warning("Messageclass " + c.getName() + " not registered");
			throw new MessageHandlingException("Messageclass " + c.getName() + " registered");
		}
		return result; // thanks to unboxing...
	}

	/**
	 * requests a class for given id
	 *
	 * @param typeId
	 * @return  the message class or <code>null</code> if no class was registered with that id
	 */
	public static final Class<? extends Message> getMessageTypeClass(short typeId) {
		return registry.get(typeId);
	}

	/**
	 * returns a sorted list of superclasses upto Message.class. On each level all the implemented
	 * Interfaces are recursivly included. This hierarchy is used in DynamicMessageListener mimic,
	 * to find the next fitting method, if there is no method in Listener that deals directly with
	 * the argument class.
	 *
	 * @param c - a 'real' message class to get the hierarchy list for
	 * @return a list of classes and Interfaces representing the superclass/interface structure of c
	 *         or null, if c was not scanned before
	 */
	public static ArrayList<Class<?>> getMessClassHierarchy(Class<? extends Message> c) {
		return hierarchy.get(c);
	}

	private static ArrayList<Class<?>> scanMessClassHierarchy(Class<?> c) {
		ArrayList<Class<?>> list = new ArrayList<Class<?>>();
		do {
			if (list.contains(c)) break;
			list.add(c);
			scanIF(list, c); // recursively find all interfaces and their super
		} while ((c = c.getSuperclass()) != Message.class); // next superclass
		return list;
	}

	private static void scanIF(ArrayList<Class<?>> lst, Class c) {
		Class[] interfaces = c.getInterfaces();
		for (Class ifc : interfaces) {
			if (lst.contains(ifc)) break;
			lst.add(ifc);
			scanIF(lst, ifc);
		}
	}

	/**
	 * fills the currently registered non-system messages (that is their names) into the LRM,
	 * together with their local ID's
	 * will not contain sysmtem level messages (with ids < 0)
	 *
	 * @param message the LRM to be filled in
	 */
	public static final void populateRegistrationMessage(LocalRegistrationMessage message) {
		// destination arrays
		int nonSystem = registry.size() - systemIdCnt;
		short[] ids = new short[nonSystem];
		String[] names = new String[nonSystem];
		// registered keys
		Short[] registeredIds = registry.keySet().toArray(new Short[registry.keySet().size()]);

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
		message.setPriority(PriorityMessage.PRIORITY_HIGH);

	}

	/**
	 * tries to generate a pretty random long
	 * note: random will return the same value
	 *        each time the applikation (eg. VM) will start anew.
	 *        This may be good for developers. Might consider using a private
	 *        java.util.Random for production.
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
	 * @param updatables - one or more "tasks" to be done until they are not any more alive.
	 * @return Runnable - ready for wrapping into a thread and get started.
	 * @see Updatable
	 * @see UpdatableRunnable
	 */
	public static final Runnable createRunnable(Updatable... updatables) {
		return createRunnable(1, updatables);
	}

	/**
	 * same as createRunnable(Updatable... updatables) but lets you specify the sleep time
	 * between passes.
	 *
	 * @param sleep - time to snare in ms, if <=0 will instead Thread.yield()
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
		return createThread(1, updatables);
	}

	/**
	 * same as createThread(Updatable... updatables) but lets you specify the sleep time
	 * between passes.
	 * The returned thread will have a name of "JGN_Upd"
	 *
	 * @param sleep - time to snare in ms, if <=0 will instead Thread.yield()
	 * @param updatables -one or more "tasks" to be done until they are not any more alive.
	 * @return Thread - ready for getting started.
	 */
	public static final Thread createThread(long sleep, Updatable... updatables) {
		Thread res = new Thread(createRunnable(sleep, updatables));
		res.setName("JGN_Upd" + res.getId());
		return res;
	}

}

/**
 * A Runnable that within it's run method, cycles a list of updatables as long as they
 * stay alive. Additionally a sleep time between cycles can be specified
 */
class UpdatableRunnable implements Runnable {
	private long sleep;
	private Updatable[] updatables;
	private Logger log;

	/**
	 * Creates the Runnable
	 *
	 * @param sleep      if > 0, will sleep this amount in ms between cycles;
	 *                   else no delay (but a Thread.yield instead)
	 *                   NOTE that bigger values may reduce the response time of the system
	 *                   but too small a value will possibly eat up most CPU time, and make other
	 *                   threads unresponsive.
	 * @param updatables the tasks, to be run by executing their update() method.
	 */
	public UpdatableRunnable(long sleep, Updatable... updatables) {
		this.sleep = sleep;
		this.updatables = updatables;
		// note this logger is per instance (and not based on classname) !
		log = Logger.getLogger("com.captiveimagination.jgn.UpdRun");
	}

	/**
	 * periodically calls the update() method of all Updatables
	 * The owning thread will terminate, when all Updatables deliver isAlive() == false.
	 * <p/>
	 * Each Throwable during an update() will be wrapped into a RunTimeException and
	 * currently terminate ALL tasks and the owning thread.
	 */
	public void run() {
 		boolean alive;
		long threadId = Thread.currentThread().getId(); // this is as of Jdk15!

		// note: use following line, to adjust the real id of the real thread
		//       with the fake id as of JDK14 logger!!
		log.info("JGN update thread " + threadId + " started");

		if (log.isLoggable(Level.FINER)) {
			for (Updatable u : updatables) {
				log.log(Level.FINER, " -works on: {0}", u);
			}
		}
		do {
      alive = false;
  		for (Updatable u : updatables) {
	    	if (u.isAlive()) {
					alive = true;     // if at least one u is alive(), keep running
          try {
						u.update();
					} catch (Throwable t) {
						// TODO ase: think again about termination
						log.severe("Update thread " + threadId + " will die, because..");
						log.log(Level.SEVERE, "-->", t);
						throw new RuntimeException(t);
			    }
        }
      }
      if (sleep > 0) {
				try {
					Thread.sleep(sleep);
				} catch (InterruptedException exc) {
					// no real need for --> exc.printStackTrace();
					// therefore this is space for rent...
				}
			} else {
				Thread.yield();
			}
		} while (alive);
		log.info("JGN update thread " + threadId + " terminated");
	}
}
