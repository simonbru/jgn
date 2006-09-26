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
	
	private long id;
	private short groupId;
	private short playerId;
	private short destinationPlayerId;
	private long timestamp;
	private int tries;
	private int maxTries;
	private long timeout;
	private MessageClient client;
	
	public Message() {
		groupId = -1;	// Default to -1 meaning there is no group assignment
		playerId = -1;
		destinationPlayerId = -1;
		timestamp = -1;
		tries = 0;
		maxTries = 5;
		timeout = 5000;
	}
	
	/**
	 * Unique message id from this message server that
	 * is generated on instantiation if the message implements
	 * UniqueMessage.
	 * 
	 * @return
	 * 		long
	 */
	@Hide
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
	@Hide
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
	@Hide
	public void setGroupId(short groupId) {
		this.groupId = groupId;
	}
	
	/*
	 * Ignored unless the extending Message implements
	 * PlayerMessage. This is useful for distinguishing
	 * between players on a game server.
	 */
	@Hide
	public short getPlayerId() {
		return playerId;
	}
	
	/**
	 * Ignored unless the extending Message implements
	 * PlayerMessage. This is useful for distinguishing
	 * between players on a game server.
	 * 
	 * @param playerId
	 */
	@Hide
	public void setPlayerId(short playerId) {
		this.playerId = playerId;
	}
	
	/**
	 * Ignored unless the extending Message implements
	 * PlayerMessage. This is utilized for routing of
	 * messages typically through a server to a destination
	 * player.
	 * 
	 * @return
	 * 		destinationPlayerId
	 */
	@Hide
	public short getDestinationPlayerId() {
		return destinationPlayerId;
	}
	
	/**
	 * Ignored unless the extending Message implements
	 * PlayerMessage. This is utilized for routing of
	 * messages typically through a server to a destination
	 * player.
	 * 
	 * @param destinationPlayerId
	 */
	@Hide
	public void setDestinationPlayerId(short destinationPlayerId) {
		this.destinationPlayerId = destinationPlayerId;
	}
	
	/**
	 * This method is called internally when a Message is
	 * sent or received so there is a trace-back point of
	 * origin.
	 * 
	 * @param client
	 */
	@Hide
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
	@Hide
	public MessageClient getMessageClient() {
		return client;
	}
	
	public Message clone() throws CloneNotSupportedException {
		return (Message)super.clone();
	}

	@Hide
	public long getTimestamp() {
		return timestamp;
	}
	
	@Hide
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	@Hide
	public int getTries() {
		return tries;
	}
	
	@Hide
	public void setTries(int tries) {
		this.tries = tries;
	}
	
	@Hide
	public int getMaxTries() {
		return maxTries;
	}
	
	@Hide
	public void setMaxTries(int maxTries) {
		this.maxTries = maxTries;
	}
	
	@Hide
	public long getTimeout() {
		return timeout;
	}
	
	@Hide
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
	
	@Hide
	public static synchronized int nextUniqueId() {
		if (UNIQUE_ID == Integer.MAX_VALUE - 1) UNIQUE_ID = 0;
		return ++UNIQUE_ID;
	}
}
