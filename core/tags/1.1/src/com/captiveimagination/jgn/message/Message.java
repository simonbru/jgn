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

import com.captiveimagination.jgn.*;

/**
 * Message is a simple bean that can be extended for message
 * sending and receiving. This is the absolute core element
 * the entire API is based on. Note that Message is sent
 * without any verification of reception. If the message you
 * are sending requires verification that it has been sent
 * you should extend CertifiedMessage instead.
 * 
 * Though this project uses reflection to gather the getter
 * and setter methods on the extended, registered beans, for
 * practical purposes the return/set values should only be
 * primitives or Strings. Any objects or arrays will be ignored.
 * Further, Messages MUST have an empty constructor to be able
 * to instantiate the Messages dynamically.
 * 
 * @author Matthew D. Hicks
 */
public abstract class Message {
	private short type;
    private long id;
    private long sentStamp;
    private IP address;
    private int port;
    private int messageLength;
    private MessageServer server;
    
    public Message() {
    	type = JGN.getType(this);
    }
    
    /**
     * This method sets the unique id for this message.
     * This is set at send-time and need not be assigned
     * by the sender.
     * 
     * @param id
     */
    public final void setId(long id) {
        this.id = id;
    }
    
    /**
     * @return
     *      the unique identifier for this specific
     *      instantiation of this message. This is
     *      set by the sender internally before
     *      transmission and is overwritten if sent
     *      a second time.
     */
    public final long getId() {
        return id;
    }

    /**
     * This is automatically set to the current time
     * in milliseconds when the message is sent.
     * 
     * @param sentStamp
     */
    public final void setSentStamp(long sentStamp) {
        this.sentStamp = sentStamp;
    }
    
    /**
     * @return
     *      The time in milliseconds that the message
     *      was sent.
     */
    public final long getSentStamp() {
        return sentStamp;
    }
    
    /**
     * @return
     *      The unique message type. This should always
     *      be the same number but allows a distinction
     *      between different extensions of Message.
     */
    public final short getType() {
    	return type;
    }
    
    /**
     * This is set internally when a message is received
     * and dynamically generated and represents the remote
     * machine that sent this messsages' internet address.
     * 
     * @param address
     */
    public final void setRemoteAddress(IP address) {
        this.address = address;
    }
    
    /**
     * @return
     *      The address that this message was received
     *      from.
     */
    public final IP getRemoteAddress() {
        return address;
    }
    
    /**
     * This is set internally when a message is received
     * and dynamically generated and represents the remote
     * machine that sent this messages' port.
     * 
     * @param port
     */
    public final void setRemotePort(int port) {
        this.port = port;
    }
    
    /**
     * @return
     *      The port that this message was received
     *      from.
     */
    public final int getRemotePort() {
        return port;
    }

    /**
     * This method is called internally when a message is generated
     * from the server.
     * 
     * @param messageLength
     */
    public void setMessageLength(int messageLength) {
        this.messageLength = messageLength;
    }
    
    /**
     * @return
     *      the size in bytes of this message, this only returns a non-zero
     *      number when the message is received from a remote host.
     */
    public int getMessageLength() {
        return messageLength;
    }

    /**
     * This is used internally to specify the server from which a message has been sent
     * or from which it has been received on the local machine.
     * 
     * @param server
     */
    public final void setMessageServer(MessageServer server) {
        this.server = server;
    }
    
    /**
     * @return
     *      the local MessageServer from which this message was sent or received.
     */
    public final MessageServer getMessageServer() {
        return server;
    }
}
