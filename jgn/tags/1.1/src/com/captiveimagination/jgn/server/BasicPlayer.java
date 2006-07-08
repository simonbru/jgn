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
package com.captiveimagination.jgn.server;

import com.captiveimagination.jgn.*;

/**
 * @author Matthew D. Hicks
 *
 */
public class BasicPlayer implements JGNPlayer {
	private short playerId;
	private IP address;
	private int portUDP;
    private int portTCP;
	private long lastHeardFrom;
	
	public BasicPlayer(short playerId, IP address, int portUDP, int portTCP) {
		this.playerId = playerId;
		this.address = address;
		this.portUDP = portUDP;
        this.portTCP = portTCP;
		lastHeardFrom = System.currentTimeMillis();
	}
	
	public short getPlayerId() {
		return playerId;
	}

	public IP getAddress() {
        return address;
	}

	public int getUDPPort() {
		return portUDP;
	}
    
    public void setUDPPort(int portUDP) {
        this.portUDP = portUDP;
    }
    
    public int getTCPPort() {
        return portTCP;
    }
    
    public void setTCPPort(int portTCP) {
        this.portTCP = portTCP;
    }
    
    public int getPlayerPort() {
        if (portTCP > 0) {
            return portTCP;
        } else {
            return portUDP;
        }
    }

	public long getLastHeardFrom() {
		return lastHeardFrom;
	}
	
	public void heardFrom() {
		lastHeardFrom = System.currentTimeMillis();
	}
}
