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
 * Created: Aug 18, 2006
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
 *
 */
public class SharedObjectManager implements BeanChangeListener, Updatable {
	private static SharedObjectManager instance;
	
	private boolean alive;
	private ConcurrentHashMap<Object, ConcurrentHashMap<String, Object>> queue;
	private ConcurrentHashMap<Class, HashMap<String, Converter>> converters;
	private ByteBuffer buffer;
	
	private SharedObjectManager() {
		alive = true;
		queue = new ConcurrentHashMap<Object, ConcurrentHashMap<String, Object>>();
		converters = new ConcurrentHashMap<Class, HashMap<String, Converter>>();
		buffer = ByteBuffer.allocateDirect(512 * 1000);
	}
	
	public <T> T createSharedBean(String name, Class<? extends T> beanInterface) {
		// Create Magic Bean
		MagicBeanManager manager = MagicBeanManager.getInstance();
		T bean = manager.createMagicBean(beanInterface);
		
		// Create class conversion if it doesn't already exist
		if (!converters.containsKey(beanInterface)) {
			HashMap<String, Converter> map = new HashMap<String, Converter>();
			Method[] methods = beanInterface.getMethods();
			for (Method m : methods) {
				if ((m.getName().startsWith("get")) && (m.getParameterTypes().length == 0)) {
					String field = m.getName().substring(3);
					map.put(field, ConversionHandler.getConverter(m.getReturnType()));
				}
			}
			converters.put(beanInterface, map);
		}
		
		// Create Listener and register with server
		manager.addBeanChangeListener(bean, this);
		
		// Create queue entry for this bean
		queue.put(bean, new ConcurrentHashMap<String, Object>());
		
		return bean;
	}
	
	public synchronized void update() {
		Iterator<Object> objIterator = queue.keySet().iterator();
		Object object;
		ConcurrentHashMap<String, Object> updates;
		Iterator<String> changesIterator;
		String field;
		Object value;
		HashMap<String, Converter> map;
		Converter converter;
		buffer.clear();
		while (objIterator.hasNext()) {		// TODO make it only iterate over beans with changes
			object = objIterator.next();
			updates = queue.get(object);
			changesIterator = updates.keySet().iterator();
			map = converters.get(object.getClass().getInterfaces()[0]);		// TODO see if there's a better way to do this
			while (changesIterator.hasNext()) {
				field = changesIterator.next();
				value = updates.get(field);
				changesIterator.remove();
				
				// Get converter
				converter = map.get(field);
				
				// Construct a message from changes
				//converter.get
				
				// Cycle through MessageServers/MessageClients associated with this object and send
				
				System.out.println("Changes: " + object + ", " + field + ", " + value + ", " + converter);
			}
		}
	}
	
	public boolean isAlive() {
		return alive;
	}
	
	public void shutdown() {
		alive = false;
	}

	public void beanChanged(Object object, String name, Object oldValue, Object newValue) {
		queue.get(object).put(name, newValue);
	}
	
	public static final SharedObjectManager getInstance() {
		if (instance == null) {
			instance = new SharedObjectManager();
		}
		return instance;
	}
	
	public static void main(String[] args) throws Exception {
		SharedObjectManager manager = SharedObjectManager.getInstance();
		JGN.createThread(manager).start();
		MyBean bean = manager.createSharedBean("TestBean", MyBean.class);
		bean.setTest("One");
		bean.setTest("Two");
	}
}

interface MyBean {
	public String getTest();
	
	public void setTest(String test);
}
