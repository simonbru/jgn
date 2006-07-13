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
 * Created: Jul 13, 2006
 */
package com.captiveimagination.jgn.ro;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.captiveimagination.jgn.*;

/**
 * @author Matthew D. Hicks
 */
public class RemoteObjectManager {
	private static final HashMap<MessageClient,HashMap<Class<? extends RemoteObject>, RemoteObjectHandler>> map = new HashMap<MessageClient,HashMap<Class<? extends RemoteObject>, RemoteObjectHandler>>();
	
	@SuppressWarnings("all")
	public static final <T extends RemoteObject> T createRemoteObject(Class<? extends T> remoteClass, MessageClient client) throws IOException {
		HashMap<Class<? extends RemoteObject>, RemoteObjectHandler> clientMap = map.get(client);
		if (clientMap == null) {
			clientMap = new HashMap<Class<? extends RemoteObject>, RemoteObjectHandler>();
			map.put(client, clientMap);
		}
		if (clientMap.containsKey(remoteClass)) throw new IOException("A remote object by this name already exists for this MessageClient: " + remoteClass.getName());
		RemoteObjectHandler handler = new RemoteObjectHandler(remoteClass, client);
		
		Object o = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[] {remoteClass}, handler);
		
		clientMap.put(remoteClass, handler);
		
		return (T)o;
	}
	
	public static final void destroyRemoteObject(Class<? extends RemoteObject> remoteClass, MessageClient client) {
		
	}
	
	public static void main(String[] args) throws Exception {
		MyRemoteObject object = createRemoteObject(MyRemoteObject.class, null);
		System.out.println("Object: " + object.getClass());
	}
}

interface MyRemoteObject extends RemoteObject {
	public void test();
}
