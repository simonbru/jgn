/*
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
 */
package com.captiveimagination.jgn.message;

import java.util.*;

/**
 * This message type allows a series of messages to be sent in a guaranteed order
 * of delivery. This message type extends the CertifiedMessage type for guaranteed
 * delivery. The only method that should ever be explicitly called by implementing
 * applications is setOrderGroup(). All other methods are used internally for
 * verification of proper delivery.
 * 
 * @author Matthew D. Hicks
 */
public abstract class OrderedMessage extends CertifiedMessage {
	private static HashMap countGroups;
	private static long machineCode = createUniversalUniqueId();
	
	private long orderId;
	private short orderGroup;
	private long orderOriginator;
	
	public OrderedMessage() {
		orderId = -1;
		orderOriginator = machineCode;
	}
	
	/**
	 * This is set internally set on send if this is not explicitly set. If set explicitly the numbers
	 * MUST start at 0 and never skip or the next in sequence will never be received.
	 * 
	 * @param orderId
	 */
	public void setOrderId(long orderId) {
		this.orderId = orderId;
	}
	
	/**
	 * @return
	 * 		the sequence id of this message
	 */
	public long getOrderId() {
		return orderId;
	}
	
	/**
	 * This defaults to 0, but may be set the application needs to use multiple groupings
	 * of ordered messages.
	 * 
	 * @param orderGroup
	 */
	public void setOrderGroup(short orderGroup) {
		this.orderGroup = orderGroup;
	}
	
	/**
	 * @return
	 * 		The order group number differentiating one group from another
	 */
	public short getOrderGroup() {
		return orderGroup;
	}
	
	/**
	 * The orderOriginator is generated when this class is first used in order to define
	 * a universal unique id to be used to globally differentiate one machine from another.
	 * 
	 * @param orderOriginator
	 */
	public void setOrderOriginator(long orderOriginator) {
		this.orderOriginator = orderOriginator;
	}
	
	/**
	 * @return
	 * 		The universal unique identifier for the originating machine.
	 */
	public long getOrderOriginator() {
		return orderOriginator;
	}
	
	public int getMaxTries() {
		return 0;
	}
	
	public static synchronized final long createUniqueId(short orderGroup) {
		if (countGroups == null) countGroups = new HashMap();
		Long count = (Long)countGroups.get(new Short(orderGroup));
		if (count == null) {
			count = new Long(0);
		} else {
			count = new Long(count.longValue() + 1);
		}
		countGroups.put(new Short(orderGroup), count);
		return count.longValue();
	}
	
	private static long createUniversalUniqueId() {
		long id = Math.round(Math.random() * Long.MAX_VALUE);
		return id;
	}
}
