/*
 * Created on 6-jun-2006
 */

package com.captiveimagination.jgn;

public interface MessageQueue {
	public void add(Message m);

	public Message poll();

	public boolean isEmpty();
}
