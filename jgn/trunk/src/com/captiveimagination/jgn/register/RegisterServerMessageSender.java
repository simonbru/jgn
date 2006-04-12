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
package com.captiveimagination.jgn.register;

import java.io.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.register.message.*;

/**
 * Sends messages to the registry to update the status of this server.
 * 
 * @author Matthew D. Hicks
 */
public class RegisterServerMessageSender implements MessageSender {
    private MessageServer messageServer;
    private Server server;
    private IP address;
    private int port;
    private boolean enabled;
    
    public RegisterServerMessageSender(MessageServer messageServer, Server server, IP address, int port) {
        this.messageServer = messageServer;
        this.server = server;
        this.address = address;
        this.port = port;
        enabled = true;
        
        ServerRegister.registerMessages();
    }
    
    public int getUpdatesPerCycle() {
        return 1;
    }

    public void sendMessage() {
        RegisterServerMessage message = new RegisterServerMessage();
        message.setAddress(server.getAddress());
        message.setGame(server.getGame());
        message.setHost(server.getHost());
        message.setInfo(server.getInfo());
        message.setMap(server.getMap());
        message.setPortUDP(server.getPortUDP());
        message.setPortTCP(server.getPortTCP());
        message.setServerName(server.getServerName());
        try {
            messageServer.sendMessage(message, address, port);
        } catch(IOException exc) {
            throw new RuntimeException(exc);
        }
    }

    public void enable() {
        enabled = true;
    }
    
    public void disable() {
        enabled = false;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public static final Updater createUpdaterAndServerRegistrationSender(MessageServer messageServer, Server server, IP registryAddress, int registryPort) {
        Updater updater = new Updater(15 * 1000);
        RegisterServerMessageSender sender = new RegisterServerMessageSender(messageServer, server, registryAddress, registryPort);
        updater.add(sender);
        updater.start();
        return updater;
    }
}
