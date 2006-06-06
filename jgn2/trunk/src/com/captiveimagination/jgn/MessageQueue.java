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
 * Created: Jun 6, 2006
 */
package com.captiveimagination.jgn;

import java.util.LinkedList;

public class MessageQueue {
	LinkedList<Message>[] lists;

	public MessageQueue() {
		lists = new LinkedList[5];
		for (int i = 0; i < lists.length; i++) {
			lists[i] = new LinkedList<Message>();
		}
	}

	private volatile int size = 0;

	public void add(Message m) {
		int p = m.getPriority();

		if (p < Message.PRIORITY_TRIVIAL) throw new IllegalStateException();
		if (p > Message.PRIORITY_CRITICAL) throw new IllegalStateException();

		synchronized (lists[p]) {
			lists[p].addLast(m);
		}

		size++;
	}

	public Message poll() {
		if (isEmpty()) return null;

		for (int i = lists.length - 1; i >= 0; i--) {
			synchronized (lists[i]) {
				if (lists[i].isEmpty()) continue;

				Message m = lists[i].removeFirst();
				size--;
				return m;
			}
		}

		return null;
	}

	public boolean isEmpty() {
		return size == 0;
	}
}