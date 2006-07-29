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
 * Created: Jul 28, 2006
 */
package com.captiveimagination.jgn.sync;

import java.util.concurrent.*;

/**
 * @author Matthew D. Hicks
 */
class SyncObject {
	private short objectId;
	private Object object;
	private long rate;
	private GraphicalController controller;
	private ConcurrentHashMap<Short,Long> playerToSynchronized;
	
	public SyncObject(short objectId, Object object, long rate, GraphicalController controller) {
		this.objectId = objectId;
		this.object = object;
		this.rate = rate;
		this.controller = controller;
		playerToSynchronized = new ConcurrentHashMap<Short,Long>();
	}
	
	public short getObjectId() {
		return objectId;
	}
	
	public Object getObject() {
		return object;
	}
	
	public boolean isReady(short playerId) {
		float adjustment = controller.proximity(object, playerId);
		long lastSynchronized = -1;
		if (playerToSynchronized.containsKey(playerId)) {
			lastSynchronized = playerToSynchronized.get(playerId);
		}
		long adjusted = Math.round(rate * 5.0f - ((rate * 5.0f) * adjustment));
		if (adjustment == 0.0f) adjusted = 0;
		if (System.nanoTime() > lastSynchronized + rate + adjusted) {
			return true;
		}
		return false;
	}
	
	public Object getForSynchronization(short playerId) {
		playerToSynchronized.put(playerId, System.nanoTime());
		return object;
	}
}
