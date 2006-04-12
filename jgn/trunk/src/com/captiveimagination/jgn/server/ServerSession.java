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
import com.captiveimagination.jgn.message.player.*;

public interface ServerSession {
    /**
     * This method is invoked when a request to join the server is received.
     * 
     * @param message
     * @param server
     * @return
     */
	public PlayerJoinResponseMessage receivedJoinRequest(PlayerJoinRequestMessage message);
	
    /**
     * This method is invoked if receivedJoinRequest returns a accept PlayerJoinResponseMessage.
     * This message should inform currently connected clients of the new player join.
     * 
     * @param playerId
     * @return
     *      Should return the PlayerJoinRequestMessage that will be sent to all currently
     *      connected clients.
     */
    public PlayerJoinRequestMessage createClientJoinRequest(short playerId);
    
	public long getPlayerTimeout();
	
	public void expirePlayer(short playerId);
	
	public JGNPlayer getPlayer(short playerId);
	
	public JGNPlayer[] getPlayers();
}
