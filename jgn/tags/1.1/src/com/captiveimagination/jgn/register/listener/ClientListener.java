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

import java.io.*;

import com.captiveimagination.jgn.event.*;
import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.register.*;
import com.captiveimagination.jgn.register.message.*;

/**
 * @author Matthew D. Hicks
 */
public class ClientListener implements MessageListener {
    private ServerRegister register;
    
    public ClientListener(ServerRegister register) {
        this.register = register;
    }
    
    public void messageReceived(Message message) {
    }
    
    public void messageReceived(RequestServersMessage message) {
        Server[] servers = register.getServers();
        ServerStatusMessage ssm;
        for (int i = 0; i < servers.length; i++) {
            ssm = new ServerStatusMessage();
            ssm.setAddress(servers[i].getAddress());
            ssm.setGame(servers[i].getGame());
            ssm.setHost(servers[i].getHost());
            ssm.setInfo(servers[i].getInfo());
            ssm.setMap(servers[i].getMap());
            ssm.setPortUDP(servers[i].getPortUDP());
            ssm.setPortTCP(servers[i].getPortTCP());
            ssm.setServerName(servers[i].getServerName());
            ssm.setPlayers(servers[i].getPlayers());
            ssm.setMaxPlayers(servers[i].getMaxPlayers());
            try {
                register.getMessageServer().sendMessage(ssm, message.getRemoteAddress(), message.getRemotePort());
            } catch(IOException exc) {
                throw new RuntimeException(exc);
            }
        }
        System.out.println("Server list sent to: " + message.getRemoteAddress().toString() + ":" + message.getRemotePort() + " - " + servers.length + " servers sent.");
    }

    public int getListenerMode() {
        return MessageListener.CLOSEST;
    }
}
