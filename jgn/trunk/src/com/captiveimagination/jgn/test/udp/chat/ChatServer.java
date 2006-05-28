/*
 * Created on Nov 29, 2005
 */
package com.captiveimagination.jgn.test.udp.chat;

import java.io.*;

import com.captiveimagination.jgn.*;

public class ChatServer {
    private MessageServer server;
    
    public ChatServer(int port) throws IOException {
        server = new UDPMessageServer(IP.getLocalHost(), port);
    }
    
    public void start() {
        server.addMessageListener(new ServerListener(server));
        // We start the update thread - this is an alternative to calling update() in your game thread
        server.startUpdateThread();
    }
}
