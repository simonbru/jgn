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
import java.net.*;
import java.util.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.register.listener.*;
import com.captiveimagination.jgn.register.message.*;

/**
 * @author Matthew D. Hicks
 */
public class ServerRegister extends Thread {
    public static long TIMEOUT = 2 * 60 * 1000; // Default timeout set to 2 minutes
    
    private static Server debugServer;
    
    private HashMap servers;
    private MessageServer server;
    
    public ServerRegister(MessageServer server) throws IOException {
    	registerMessages();
    	
        servers = new HashMap();
        if (debugServer != null) {
        	addServer(debugServer);
        }
        
        this.server = server;
        
        // The listener that registers/unregisters servers to the server list
        ServerListener sl = new ServerListener(this);
        server.addMessageListener(sl);
        
        // The listener that receives server list requests and fulfills them
        ClientListener cl = new ClientListener(this);
        server.addMessageListener(cl);
        
        System.out.println("Successfully started " + server.getClass().getName() + " register at " + server.getAddress().toString() + ":" + server.getPort() + "...");
    }
    
    public void run() {
        while (true) {
            try {
                Thread.sleep(100);
            } catch(InterruptedException exc) {
                exc.printStackTrace();
            }
            validateExpires();
            
            server.update();
        }
    }
    
    private void validateExpires() {
        Server[] temp = getServers();
        Long created;
        for (int i = 0; i < temp.length; i++) {
            created = (Long)servers.get(temp[i]);
            if (System.currentTimeMillis() > (created.longValue() + TIMEOUT)) {
            	if ((debugServer != null) && (temp[i] == debugServer)) continue;
                System.out.println("Server has expired, removing: " + temp[i].getServerName());
                servers.remove(temp[i]);
            }
        }
    }
    
    public synchronized boolean addServer(Server server) {
        boolean existed = removeServer(server.getAddress(), server.getPortUDP(), server.getPortTCP());
        servers.put(server, new Long(System.currentTimeMillis()));
        return existed;
    }
    
    public boolean removeServer(byte[] address, int portUDP, int portTCP) {
        Server server;
        Server[] temp = getServers();
        for (int i = 0; i < temp.length; i++) {
            server = (Server)temp[i];
            if ((server.getAddress()[0] == address[0]) &&
                (server.getAddress()[1] == address[1]) &&
                (server.getAddress()[2] == address[2]) &&
                (server.getAddress()[3] == address[3]) &&
                (server.getPortUDP() == portUDP) &&
                (server.getPortTCP() == portTCP)) {
                    servers.remove(server);
                    return true;
            }
        }
        return false;
    }
    
    public Server[] getServers() {
        return (Server[])servers.keySet().toArray(new Server[servers.size()]);
    }
    
    public MessageServer getMessageServer() {
        return server;
    }
    
    public static void registerMessages() {
    	JGN.registerMessage(RegisterServerMessage.class, (short)-1);
        JGN.registerMessage(RequestServersMessage.class, (short)-2);
        JGN.registerMessage(ServerStatusMessage.class, (short)-3);
        JGN.registerMessage(UnregisterServerMessage.class, (short)-4);
    }
    
    public static void main(String[] args) throws Exception  {
    	MessageServer server = null;
    	
    	for (int i = 0; i < args.length; i++) {
    		if (args[i].equalsIgnoreCase("tcp")) {
    			server = new TCPMessageServer(new InetSocketAddress((InetAddress)null, 9801));
    		} else if (args[i].equalsIgnoreCase("udp")) {
    			server = new UDPMessageServer(new InetSocketAddress((InetAddress)null, 9801));
    		} else if (args[i].equalsIgnoreCase("debug")) {
    			InetAddress address = InetAddress.getByName("captiveimagination.com");
    			debugServer = new Server("DebugServer", address.getHostName(), address.getAddress(), 9901, 9902, "DebugGame", "DebugMap", "DebugInfo", 2, 16);
    		}
    	}
    	if (server == null) {
    		server = new UDPMessageServer(new InetSocketAddress((InetAddress)null, 9801));
    		
    	}
        ServerRegister register = new ServerRegister(server);
        register.start();
    }
}
