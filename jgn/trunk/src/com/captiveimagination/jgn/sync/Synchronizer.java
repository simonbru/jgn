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
 * Created: Jul 27, 2006
 */
package com.captiveimagination.jgn.sync;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.clientserver.*;
import com.captiveimagination.jgn.sync.message.*;

/**
 * @author Matthew D. Hicks
 */
public abstract class Synchronizer implements Updatable {
	public static enum Type {
		AUTHORITATIVE,
		PASSIVE
	}
	private GraphicalController controller;
	private ConcurrentLinkedQueue<SyncObject> syncList;
	private ConcurrentHashMap<Short,SyncObject> passiveList;
	private boolean keepAlive;
	
	public Synchronizer(GraphicalController controller) {
		this.controller = controller;
		syncList = new ConcurrentLinkedQueue<SyncObject>();
		passiveList = new ConcurrentHashMap<Short,SyncObject>();
		keepAlive = true;
	}
	
	public void update(short playerId, MessageSender sender) {
		Iterator<SyncObject> iterator = syncList.iterator();
		SyncObject syncObject;
		while (iterator.hasNext()) {
			syncObject = iterator.next();
			if (syncObject.isReady(playerId)) {
				SynchronizeMessage message = controller.createSynchronizationMessage(syncObject.getForSynchronization(playerId));
				message.setSyncObjectId(syncObject.getObjectId());
				sender.sendMessage(message);
			}
		}
	}
	
	public void register(short objectId, Object object) {
		register(objectId, object, 0, 0, Type.PASSIVE);
	}
	
	public void register(short objectId, Object object, long rateMillis, long rateNanos) {
		register(objectId, object, rateMillis, rateNanos, Type.AUTHORITATIVE);
	}
	
	private void register(short objectId, Object object, long rateMillis, long rateNanos, Type type) {
		long rate = (rateMillis * 1000000) + rateNanos;
		SyncObject syncObject = new SyncObject(objectId, object, rate, controller);
		if (type == Type.AUTHORITATIVE) {
			syncList.add(syncObject);
		}
		passiveList.put(objectId, syncObject);
	}

	protected GraphicalController getController() {
		return controller;
	}
	
	protected SyncObject getObject(short objectId) {
		return passiveList.get(objectId);
	}
	
	public void shutdown() {
		keepAlive = false;
	}
	
	public boolean isAlive() {
		return keepAlive;
	}
}