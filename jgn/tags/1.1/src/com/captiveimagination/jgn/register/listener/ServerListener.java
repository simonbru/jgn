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
package com.captiveimagination.jgn.register.listener;

import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.register.*;
import com.captiveimagination.jgn.register.message.*;

/**
 * @author Matthew D. Hicks
 */
public class ServerListener implements MessageListener {
    private ServerRegister register;
    
    public ServerListener(ServerRegister register) {
        this.register = register;
    }
    
    public void messageReceived(Message message) {
    }
    
    public void messageReceived(RegisterServerMessage message) {
        Server server = new Server();
        server.setAddress(message.getAddress());
        server.setGame(message.getGame());
        server.setHost(message.getHost());
        server.setInfo(message.getInfo());
        server.setMap(message.getMap());
        server.setPortUDP(message.getPortUDP());
        server.setPortTCP(message.getPortTCP());
        server.setServerName(message.getServerName());
        server.setPlayers(message.getMaxPlayers());
        server.setMaxPlayers(message.getMaxPlayers());
        if (!register.addServer(server)) {
            System.out.println("Server registered: " + server.getServerName() + " (" + register.getServers().length + " total servers registered).");
        }
    }
    
    public void messageReceived(UnregisterServerMessage message) {
        register.removeServer(message.getAddress(), message.getPortUDP(), message.getPortTCP());
        System.out.println("Server unregistered: " + message.getAddress() + ":" + message.getPortUDP() + ":" + message.getPortTCP() + " (" + register.getServers().length + " total servers registered).");
    }

    public int getListenerMode() {
        return MessageListener.CLOSEST;
    }
}
