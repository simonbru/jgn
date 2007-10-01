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
package com.captiveimagination.jgn.message.peer;

import com.captiveimagination.jgn.message.*;

/**
 * Base class for PeerMessage communication.
 * 
 * @author Matthew D. Hicks
 */
public class PeerMessage extends CertifiedMessage {
	public static final byte REQUEST_JOIN = 1;
	public static final byte REQUEST_RESPONSE = 2;
	public static final byte REQUEST_PEERS = 3;
	public static final byte REQUEST_NOOP = 4;
	public static final byte REQUEST_DISCONNECT = 5;
	
	private long peerId;
	private byte requestType;
	private int portUDP;
	private int portTCP;
	
	public long getPeerId() {
		return peerId;
	}

	public void setPeerId(long peerId) {
		this.peerId = peerId;
	}

	public byte getRequestType() {
		return requestType;
	}

	public void setRequestType(byte requestType) {
		this.requestType = requestType;
	}

	public int getPortTCP() {
		return portTCP;
	}

	public void setPortTCP(int portTCP) {
		this.portTCP = portTCP;
	}

	public int getPortUDP() {
		return portUDP;
	}

	public void setPortUDP(int portUDP) {
		this.portUDP = portUDP;
	}

	public long getResendTimeout() {
		return 2000;
	}

	public int getMaxTries() {
		return 5;
	}

	public static final String getRequestType(byte type) {
		if (type == REQUEST_JOIN) {
			return "REQUEST_JOIN";
		} else if (type == REQUEST_RESPONSE) {
			return "REQUEST_RESPONSE";
		} else if (type == REQUEST_PEERS) {
			return "REQUEST_PEERS";
		} else if (type == REQUEST_NOOP) {
			return "REQUEST_NOOP";
		} else if (type == REQUEST_DISCONNECT) {
			return "REQUEST_DISCONNECT";
		}
		return "Unknown: " + type;
	}
}
