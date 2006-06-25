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
 * Created: Jun 3, 2006
 */
package com.captiveimagination.jgn.message;

import com.captiveimagination.jgn.*;

/**
 * Message is the foundation for all communication in JGN.
 * Extending classes simply need to add getter/setters for
 * their Messages and the data will be serialized across
 * when a message is sent.
 * 
 * @author Skip M. B. Balk
 * @author Matthew D. Hicks
 */
public abstract class Message implements Cloneable {
	private static int UNIQUE_ID = 0;
	
	public static final byte PRIORITY_CRITICAL = 4;
	public static final byte PRIORITY_HIGH = 3;
	public static final byte PRIORITY_NORMAL = 2;
	public static final byte PRIORITY_LOW = 1;
	public static final byte PRIORITY_TRIVIAL = 0;
	
	private long id;
	private short groupId;
	private byte priority;
	private MessageClient client;
	
	public Message() {
		// Assign default priority
		this.priority = PRIORITY_NORMAL;
	}
	
	/**
	 * Unique message id from this message server that
	 * is generated on instantiation if the message implements
	 * UniqueMessage.
	 * 
	 * @return
	 */
	public long getId() {
		return id;
	}
	
	/**
	 * Unique message id from this message server that
	 * is generated on instantiation. (if the message
	 * implements UniqueMessage). This method is
	 * invoked on instantiation and overridden when a
	 * message is received from a remote client to keep
	 * the id as is sent, so this should never be called
	 * externally.
	 * 
	 * @param id
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * Ignored unless the extending Message implements
	 * GroupMessage interface. This should be set explicitly
	 * if implementing GroupMessage or it will default to
	 * group based on the message's class.
	 * 
	 * @return
	 * 		groupId reference for this Message as a short
	 */
	public short getGroupId() {
		return groupId;
	}
	
	/**
	 * Sets the groupId for this Message. This is only utilized
	 * if the Message implements GroupMessage. This will be defaulted
	 * to 0 unless otherwise set. If the value is set to 0 the
	 * queue will base groupings off of the message's class.
	 * 
	 * @param groupId
	 */
	public void setGroupId(short groupId) {
		this.groupId = groupId;
	}
	
	/**
	 * Priority
	 * 
	 * @return
	 * 		the priority of this Message object
	 */
	public byte getPriority() {
		return priority;
	}
	
	/**
	 * The priority defines how the message is
	 * handled inside the queue for sending and
	 * receiving of messages. A high priority message
	 * that exists in the queue at the same time
	 * as a low priority message even if the low
	 * priority message was received first, should
	 * be polled before the lower priority message.
	 * 
	 * @param priority
	 */
	public void setPriority(byte priority) {
		this.priority = priority;
	}

	/**
	 * This method is called internally when a Message is
	 * sent or received so there is a trace-back point of
	 * origin.
	 * 
	 * @param client
	 */
	public void setMessageClient(MessageClient client) {
		this.client = client;
	}
	
	/**
	 * This is method will return the associated MessageClient
	 * for this Message. The setMessageClient is called internally
	 * when a Message is sent or received so a trace-back point
	 * exists.
	 * 
	 * @return
	 * 		the MessageClient this message was received to or
	 * 		sent from
	 */
	public MessageClient getMessageClient() {
		return client;
	}

	public Message clone() throws CloneNotSupportedException {
		return (Message)super.clone();
	}

	public static synchronized int nextUniqueId() {
		return ++UNIQUE_ID;
	}
}
