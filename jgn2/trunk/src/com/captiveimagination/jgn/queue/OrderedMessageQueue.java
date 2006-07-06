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
 * Created: Jul 5, 2006
 */
package com.captiveimagination.jgn.queue;

import java.util.*;
import java.util.concurrent.atomic.*;

import com.captiveimagination.jgn.message.*;

/**
 * @author Matthew D. Hicks
 */
public class OrderedMessageQueue implements MessageQueue {
	private HashMap<Object,TreeSet<OrderedMessage>> queue;
	private HashMap<Object,AtomicInteger> lastReceived;
	
	private volatile int size;
	private volatile long total;
	
	public OrderedMessageQueue() {
		queue = new HashMap<Object,TreeSet<OrderedMessage>>();
		lastReceived = new HashMap<Object,AtomicInteger>();
		size = 0;
		total = 0;
	}
	
	public void add(Message message) {
		if (message == null) throw new NullPointerException("Message must not be null");
		
		OrderedMessage m = (OrderedMessage)message;
		
		synchronized (queue) {
			if (message.getGroupId() == -1) {
				// No groupId, so we base it off the class
				if (queue.containsKey(message.getClass())) {
					queue.get(message.getClass()).add(m);
				} else {
					TreeSet<OrderedMessage> set = new TreeSet<OrderedMessage>();
					set.add(m);
					queue.put(message.getClass(), set);
				}
			} else {
				// An groupId has been assigned, so lets use it
				if (queue.containsKey(message.getGroupId())) {
					queue.get(message.getGroupId()).add(m);
				} else {
					TreeSet<OrderedMessage> set = new TreeSet<OrderedMessage>();
					set.add(m);
					queue.put(message.getGroupId(), set);
				}
			}
		}
		size++;
		total++;
	}

	public Message poll() {
		if (isEmpty()) return null;
		
		synchronized (queue) {
			Message m = null;
			Iterator iterator = queue.keySet().iterator();
			while (iterator.hasNext()) {
				Object key = iterator.next();
				if (!queue.get(key).isEmpty()) {
					AtomicInteger integer = lastReceived.get(key);
					if ((integer == null) && (queue.get(key).first().getOrderId() == 0)) {
						// First message found
						integer = new AtomicInteger(0);
						lastReceived.put(key, integer);
						m = queue.get(key).first();
						queue.get(key).remove(m);
					} else if ((integer != null) && (queue.get(key).first().getOrderId() == integer.intValue() + 1)) {
						integer.incrementAndGet();
						m = queue.get(key).first();
						queue.get(key).remove(m);
					}
				}
			}
			if (m != null) size--;
			return m;
		}
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public long getTotal() {
		return total;
	}

	public int getSize() {
		return size;
	}

}
